package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import org.apache.commons.collections4.ListUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApOASysContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ProbationOffenderSearchApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.datasource.OffenderDetailsDataSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ProbationOffenderSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.probationoffendersearchapi.ProbationOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import java.util.stream.Collectors

@Service("CAS2OffenderService")
@Suppress(
  "ReturnCount",
)
class OffenderService(
  private val prisonsApiClient: PrisonsApiClient,
  private val probationOffenderSearchApiClient: ProbationOffenderSearchApiClient,
  private val apOASysContextApiClient: ApOASysContextApiClient,
  private val offenderDetailsDataSource: OffenderDetailsDataSource,
  @Value("\${cas2.crn-search-limit:400}") private val numberOfCrn: Int,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun getPersonByNomsNumber(nomsNumber: String, currentUser: NomisUserEntity): ProbationOffenderSearchResult {
    fun logFailedResponse(probationResponse: ClientResult.Failure<List<ProbationOffenderDetail>>) =
      log.warn("Could not get inmate details for $nomsNumber", probationResponse.toException())

    val probationResponse = probationOffenderSearchApiClient.searchOffenderByNomsNumber(nomsNumber)

    val probationOffenderDetailList = when (probationResponse) {
      is ClientResult.Success -> probationResponse.body
      is ClientResult.Failure.StatusCode -> when (probationResponse.status) {
        HttpStatus.NOT_FOUND -> return ProbationOffenderSearchResult.NotFound(nomsNumber)
        HttpStatus.FORBIDDEN -> return ProbationOffenderSearchResult.Forbidden(nomsNumber, probationResponse.toException())
        else -> {
          logFailedResponse(probationResponse)
          return ProbationOffenderSearchResult.Unknown(nomsNumber, probationResponse.toException())
        }
      }

      is ClientResult.Failure -> {
        logFailedResponse(probationResponse)
        return ProbationOffenderSearchResult.Unknown(nomsNumber, probationResponse.toException())
      }
    }

    if (probationOffenderDetailList.isEmpty()) {
      return ProbationOffenderSearchResult.NotFound(nomsNumber)
    } else {
      val probationOffenderDetail = probationOffenderDetailList[0]

      // check for restrictions or exclusions
      if (hasRestrictionOrExclusion(probationOffenderDetail)) return ProbationOffenderSearchResult.Forbidden(nomsNumber)

      // check inmate details from Prison API
      val inmateDetails = getInmateDetailsForProbationOffender(probationOffenderDetail)
        ?: return ProbationOffenderSearchResult.NotFound(nomsNumber)

      // check if same prison
      return if (isOffenderSamePrisonAsUser(inmateDetails, currentUser)) {
        ProbationOffenderSearchResult.Success.Full(nomsNumber, probationOffenderDetail, inmateDetails)
      } else {
        ProbationOffenderSearchResult.Forbidden(nomsNumber)
      }
    }
  }

  private fun hasRestrictionOrExclusion(probationOffenderDetail: ProbationOffenderDetail): Boolean {
    return probationOffenderDetail.currentExclusion == true || probationOffenderDetail.currentRestriction == true
  }

  private fun isOffenderSamePrisonAsUser(inmateDetail: InmateDetail?, currentUser: NomisUserEntity): Boolean {
    return currentUser.activeCaseloadId == inmateDetail?.assignedLivingUnit?.agencyId
  }

  private fun getInmateDetailsForProbationOffender(probationOffenderDetail: ProbationOffenderDetail): InmateDetail? {
    return probationOffenderDetail.otherIds.nomsNumber?.let { nomsNumber ->
      when (val inmateDetailsResult = getInmateDetailByNomsNumber(probationOffenderDetail.otherIds.crn, nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        else -> null
      }
    }
  }

  fun getOffenderNameOrPlaceholder(crn: String): String {
    return when (val offenderResponse = offenderDetailsDataSource.getOffenderDetailSummary(crn)) {
      is ClientResult.Success ->
        "${offenderResponse.body.firstName} ${offenderResponse.body.surname}"

      is ClientResult.Failure.StatusCode ->
        if (offenderResponse.status.value() == HttpStatus.NOT_FOUND.value()) {
          return "Person Not Found"
        } else {
          return "Unknown"
        }

      is ClientResult.Failure -> return "Unknown"
    }
  }

  private fun getOffenderSummariesByCrns(
    crns: Set<String>,
  ): Map<String, ClientResult<OffenderDetailSummary>> {
    if (crns.isEmpty()) return emptyMap()

    return offenderDetailsDataSource.getOffenderDetailSummaries(crns.toList())
  }

  fun getMapOfPersonNamesAndCrns(crns: List<String>): Map<String, String> {
    val personNamesListOfMaps = ListUtils.partition(crns.toList(), numberOfCrn).stream()
      .map { partitionedCrns ->
        getOffenderNamesOrPlaceholder(partitionedCrns.toSet())
      }.collect(Collectors.toList())

    return personNamesListOfMaps.flatMap { it.toList() }.toMap()
  }

  private fun getOffenderNamesOrPlaceholder(crns: Set<String>): Map<String, String> {
    val offenderSummaries = getOffenderSummariesByCrns(crns)

    return crns.map { crn ->
      when (val offenderResponse = offenderSummaries[crn]) {
        is ClientResult.Success ->
          return@map crn to "${offenderResponse.body.firstName} ${offenderResponse.body.surname}"

        is ClientResult.Failure.StatusCode ->
          if (offenderResponse.status.value() == HttpStatus.NOT_FOUND.value()) {
            return@map crn to "Person Not Found"
          } else {
            return@map crn to "Unknown"
          }

        else -> return@map crn to "Unknown"
      }
    }.toMap()
  }

  private fun getInfoForPerson(crn: String): PersonInfoResult {
    var offenderResponse = offenderDetailsDataSource.getOffenderDetailSummary(crn)

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body

      is ClientResult.Failure.StatusCode -> if (offenderResponse.status.value() == HttpStatus.NOT_FOUND.value()) {
        return PersonInfoResult.NotFound(crn)
      } else {
        return PersonInfoResult.Unknown(crn, offenderResponse.toException())
      }

      is ClientResult.Failure -> return PersonInfoResult.Unknown(crn, offenderResponse.toException())
    }

    val inmateDetails = offender.otherIds.nomsNumber?.let { nomsNumber ->
      when (val inmateDetailsResult = getInmateDetailByNomsNumber(offender.otherIds.crn, nomsNumber)) {
        is AuthorisableActionResult.Success -> inmateDetailsResult.entity
        else -> null
      }
    }

    return PersonInfoResult.Success.Full(
      crn = crn,
      offenderDetailSummary = offender,
      inmateDetail = inmateDetails,
    )
  }

  fun getFullInfoForPersonOrThrow(crn: String): PersonInfoResult.Success.Full {
    val personInfo = getInfoForPerson(crn)
    when (personInfo) {
      is PersonInfoResult.NotFound, is PersonInfoResult.Unknown -> throw NotFoundProblem(crn, "Offender")
      is PersonInfoResult.Success.Restricted -> throw ForbiddenProblem()
      is PersonInfoResult.Success.Full -> return personInfo
    }
  }

  fun getInmateDetailByNomsNumber(crn: String, nomsNumber: String): AuthorisableActionResult<InmateDetail?> {
    var inmateDetailResponse = prisonsApiClient.getInmateDetailsWithWait(nomsNumber)

    val hasCacheTimedOut = inmateDetailResponse is ClientResult.Failure.PreemptiveCacheTimeout
    if (hasCacheTimedOut) {
      inmateDetailResponse = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
    }

    fun logFailedResponse(inmateDetailResponse: ClientResult.Failure<InmateDetail>) = when (hasCacheTimedOut) {
      true -> log.warn("Could not get inmate details for $crn after cache timed out", inmateDetailResponse.toException())
      false -> log.warn("Could not get inmate details for $crn as an unsuccessful response was cached", inmateDetailResponse.toException())
    }

    val inmateDetail = when (inmateDetailResponse) {
      is ClientResult.Success -> inmateDetailResponse.body
      is ClientResult.Failure.StatusCode -> when (inmateDetailResponse.status) {
        HttpStatus.NOT_FOUND -> {
          logFailedResponse(inmateDetailResponse)
          return AuthorisableActionResult.NotFound()
        }

        HttpStatus.FORBIDDEN -> {
          logFailedResponse(inmateDetailResponse)
          return AuthorisableActionResult.Unauthorised()
        }
        else -> {
          logFailedResponse(inmateDetailResponse)
          null
        }
      }
      is ClientResult.Failure -> {
        logFailedResponse(inmateDetailResponse)
        null
      }
    }

    return AuthorisableActionResult.Success(inmateDetail)
  }

  fun getOffenderByCrn(crn: String): AuthorisableActionResult<OffenderDetailSummary> {
    var offenderResponse = offenderDetailsDataSource.getOffenderDetailSummary(crn)

    val offender = when (offenderResponse) {
      is ClientResult.Success -> offenderResponse.body
      is ClientResult.Failure.StatusCode -> if (offenderResponse.status.equals(HttpStatus.NOT_FOUND)) return AuthorisableActionResult.NotFound() else offenderResponse.throwException()
      is ClientResult.Failure -> offenderResponse.throwException()
    }

    return AuthorisableActionResult.Success(offender)
  }

  fun getRiskByCrn(crn: String): AuthorisableActionResult<PersonRisks> {
    return when (getOffenderByCrn(crn)) {
      is AuthorisableActionResult.NotFound -> AuthorisableActionResult.NotFound()
      is AuthorisableActionResult.Unauthorised -> AuthorisableActionResult.Unauthorised()
      is AuthorisableActionResult.Success -> {
        val risks = PersonRisks(
          // Note that Tier, Mappa and Flags are all hardcoded to NotFound
          // and these unused 'envelopes' will be removed.
          roshRisks = getRoshRisksEnvelope(crn),
          mappa = RiskWithStatus(status = RiskStatus.NotFound),
          tier = RiskWithStatus(status = RiskStatus.NotFound),
          flags = RiskWithStatus(status = RiskStatus.NotFound),
        )

        AuthorisableActionResult.Success(
          risks,
        )
      }
    }
  }

  private fun getRoshRisksEnvelope(crn: String): RiskWithStatus<RoshRisks> {
    when (val roshRisksResponse = apOASysContextApiClient.getRoshRatings(crn)) {
      is ClientResult.Success -> {
        val summary = roshRisksResponse.body.rosh

        if (summary.anyRisksAreNull()) {
          return RiskWithStatus(
            status = RiskStatus.NotFound,
            value = null,
          )
        }

        return RiskWithStatus(
          status = RiskStatus.Retrieved,
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
      is ClientResult.Failure.StatusCode -> return if (roshRisksResponse.status.value() == HttpStatus.NOT_FOUND.value()) {
        RiskWithStatus(
          status = RiskStatus.NotFound,
          value = null,
        )
      } else {
        RiskWithStatus(
          status = RiskStatus.Error,
          value = null,
        )
      }
      is ClientResult.Failure -> return RiskWithStatus(
        status = RiskStatus.Error,
        value = null,
      )
    }
  }
}
