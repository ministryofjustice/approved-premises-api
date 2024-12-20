package uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.HMPPSTierApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.Mappa
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskTier
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.MappaDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService

@Component
class OffenderRisksDataSource(
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val apOASysContextApiClient: ApOASysContextApiClient,
  private val hmppsTierApiClient: HMPPSTierApiClient,
  private val sentryService: SentryService,
) {

  private val ignoredRegisterTypesForFlags = listOf("RVHR", "RHRH", "RMRH", "RLRH", "MAPP")

  fun getPersonRisks(crn: String): PersonRisks {
    val caseDetailResponse = apDeliusContextApiClient.getCaseDetail(crn)

    return PersonRisks(
      roshRisks = getRoshRisksEnvelope(crn),
      mappa = caseDetailResponse.toMappa(),
      tier = getRiskTierEnvelope(crn),
      flags = caseDetailResponse.toRiskFlags(),
    )
  }

  private fun getRoshRisksEnvelope(crn: String): RiskWithStatus<RoshRisks> {
    return when (val roshRisksResponse = apOASysContextApiClient.getRoshRatings(crn)) {
      is ClientResult.Success -> {
        val summary = roshRisksResponse.body.rosh

        when (summary.anyRisksAreNull()) {
          true -> RiskWithStatus(status = RiskStatus.NotFound)
          false -> RiskWithStatus(
            value = RoshRisks(
              overallRisk = summary.determineOverallRiskLevel().text,
              riskToChildren = summary.riskChildrenCommunity!!.text,
              riskToPublic = summary.riskPublicCommunity!!.text,
              riskToKnownAdult = summary.riskKnownAdultCommunity!!.text,
              riskToStaff = summary.riskStaffCommunity!!.text,
              lastUpdated = roshRisksResponse.body.dateCompleted?.toLocalDate()
                ?: roshRisksResponse.body.initiationDate.toLocalDate(),
            ),
          )
        }
      }
      is ClientResult.Failure.StatusCode -> when (roshRisksResponse.status) {
        HttpStatus.NOT_FOUND -> RiskWithStatus(status = RiskStatus.NotFound)
        else -> recordAndReturnError("getRoshRatings, crn: $crn", roshRisksResponse.toException())
      }
      is ClientResult.Failure -> recordAndReturnError("getRoshRatings, crn: $crn", roshRisksResponse.toException())
    }
  }

  private fun getRiskTierEnvelope(crn: String): RiskWithStatus<RiskTier> =
    when (val tierResponse = hmppsTierApiClient.getTier(crn)) {
      is ClientResult.Success -> RiskWithStatus(
        value = RiskTier(
          level = tierResponse.body.tierScore,
          lastUpdated = tierResponse.body.calculationDate.toLocalDate(),
        ),
      )
      is ClientResult.Failure.StatusCode -> when (tierResponse.status) {
        HttpStatus.NOT_FOUND -> RiskWithStatus(status = RiskStatus.NotFound)
        else -> recordAndReturnError("getTier, crn: $crn", tierResponse.toException())
      }
      is ClientResult.Failure -> recordAndReturnError("getTier, crn: $crn", tierResponse.toException())
    }

  fun ClientResult<CaseDetail>.toMappa(): RiskWithStatus<Mappa> = when (this) {
    is ClientResult.Success -> this.body.toMappa()
    is ClientResult.Failure.StatusCode -> when (this.status) {
      HttpStatus.NOT_FOUND -> RiskWithStatus(status = RiskStatus.NotFound)
      else -> recordAndReturnError("toMappa, body: ${this.body}", this.toException())
    }
    is ClientResult.Failure -> recordAndReturnError("toMappa", this.toException())
  }

  fun CaseDetail.toMappa(): RiskWithStatus<Mappa> = when (this.mappaDetail) {
    null -> RiskWithStatus(status = RiskStatus.NotFound)
    else -> {
      when (this.mappaDetail.isMissingMandatoryProperties()) {
        true -> RiskWithStatus(status = RiskStatus.NotFound)
        else -> RiskWithStatus(
          value = Mappa(
            "CAT ${this.mappaDetail.categoryDescription!!}/LEVEL ${this.mappaDetail.levelDescription!!}",
            this.mappaDetail.lastUpdated.toLocalDate(),
          ),
        )
      }
    }
  }

  private fun MappaDetail.isMissingMandatoryProperties(): Boolean = this.categoryDescription == null || this.levelDescription == null

  fun ClientResult<CaseDetail>.toRiskFlags(): RiskWithStatus<List<String>> = when (this) {
    is ClientResult.Success -> this.body.toRiskFlags()
    is ClientResult.Failure.StatusCode -> when (this.status) {
      HttpStatus.NOT_FOUND -> RiskWithStatus(status = RiskStatus.NotFound)
      else -> recordAndReturnError("toRiskFlags, body: ${this.body}", this.toException())
    }
    is ClientResult.Failure -> recordAndReturnError("toRiskFlags", this.toException())
  }

  fun CaseDetail.toRiskFlags(): RiskWithStatus<List<String>> {
    val registrations = this.registrations.filter { !ignoredRegisterTypesForFlags.contains(it.code) }

    return when (registrations.isEmpty()) {
      true -> RiskWithStatus(status = RiskStatus.NotFound)
      else -> RiskWithStatus(value = registrations.map { it.description })
    }
  }

  private fun <T> recordAndReturnError(targetInfo: String, cause: Throwable, value: T? = null): RiskWithStatus<T> {
    sentryService.captureException(
      CannotDetermineRiskException(
        message = "An error occurred obtaining Risks for $targetInfo. Returning RiskWithStatus(status=Error). This does not necessarily block processing",
        cause = cause,
      ),
    )
    return RiskWithStatus(status = RiskStatus.Error, value)
  }

  private class CannotDetermineRiskException(message: String, cause: Throwable) : RuntimeException(message, cause)
}
