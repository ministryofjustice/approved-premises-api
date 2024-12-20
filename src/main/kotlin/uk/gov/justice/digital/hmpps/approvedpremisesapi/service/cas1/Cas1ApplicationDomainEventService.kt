package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmitted
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedSubmittedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Ldu
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Region
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Team
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApprovedPremisesApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MetaDataName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.CaseDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.DomainEventTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.mapOfNonNullValues
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.Period
import java.util.UUID

@Service
class Cas1ApplicationDomainEventService(
  private val domainEventService: Cas1DomainEventService,
  private val offenderService: OffenderService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  private val domainEventTransformer: DomainEventTransformer,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  private val clock: Clock,
) {

  @SuppressWarnings("ThrowsCount", "TooGenericExceptionThrown")
  fun applicationSubmitted(
    application: ApprovedPremisesApplicationEntity,
    submitApplication: SubmitApprovedPremisesApplication,
    username: String,
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = OffsetDateTime.now()

    val offenderDetails =
      when (val offenderDetailsResult = offenderService.getOffenderByCrn(application.crn, username, true)) {
        is AuthorisableActionResult.Success -> offenderDetailsResult.entity
        is AuthorisableActionResult.Unauthorised ->
          throw RuntimeException(
            "Unable to get Offender Details when creating Application" +
              "Submitted Domain Event: Unauthorised",
          )

        is AuthorisableActionResult.NotFound ->
          throw RuntimeException(
            "Unable to get Offender Details when creating Application" +
              " Submitted Domain Event: Not Found",
          )
      }

    val risks =
      when (val riskResult = offenderService.getRiskByCrn(application.crn, username)) {
        is AuthorisableActionResult.Success -> riskResult.entity
        is AuthorisableActionResult.Unauthorised ->
          throw RuntimeException("Unable to get Risks when creating Application Submitted Domain Event: Unauthorised")

        is AuthorisableActionResult.NotFound ->
          throw RuntimeException("Unable to get Risks when creating Application Submitted Domain Event: Not Found")
      }

    val mappaLevel = risks.mappa.value?.level

    val staffDetails = when (val staffDetailsResult = apDeliusContextApiClient.getStaffDetail(username)) {
      is ClientResult.Success -> staffDetailsResult.body
      is ClientResult.Failure -> staffDetailsResult.throwException()
    }

    val caseDetail = when (val caseDetailResult = apDeliusContextApiClient.getCaseDetail(application.crn)) {
      is ClientResult.Success -> caseDetailResult.body
      is ClientResult.Failure -> caseDetailResult.throwException()
    }

    domainEventService.saveApplicationSubmittedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = offenderDetails.otherIds.nomsNumber,
        occurredAt = eventOccurredAt.toInstant(),
        data = ApplicationSubmittedEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt.toInstant(),
          eventType = EventType.applicationSubmitted,
          eventDetails = getApplicationSubmittedForDomainEvent(
            application,
            offenderDetails,
            mappaLevel,
            submitApplication,
            staffDetails,
            caseDetail,
          ),
        ),
        metadata = mapOfNonNullValues(
          MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE to submitApplication.reasonForShortNotice,
          MetaDataName.CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER to submitApplication.reasonForShortNoticeOther,
          MetaDataName.CAS1_REQUESTED_AP_TYPE to application.apType.name,
        ),
      ),
    )
  }

  fun applicationWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    withdrawingUser: UserEntity,
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = Instant.now(clock)

    domainEventService.saveApplicationWithdrawnEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt,
        data = ApplicationWithdrawnEnvelope(
          id = domainEventId,
          timestamp = eventOccurredAt,
          eventType = EventType.applicationWithdrawn,
          eventDetails = getApplicationWithdrawn(application, withdrawingUser, eventOccurredAt),
        ),
      ),
      emit = application.isSubmitted(),
    )
  }

  private fun getApplicationWithdrawn(
    application: ApprovedPremisesApplicationEntity,
    user: UserEntity,
    eventOccurredAt: Instant,
  ): ApplicationWithdrawn {
    return ApplicationWithdrawn(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      personReference = PersonReference(
        crn = application.crn,
        noms = application.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      withdrawnAt = eventOccurredAt,
      withdrawnBy = domainEventTransformer.toWithdrawnBy(user),
      withdrawalReason = application.withdrawalReason!!,
      otherWithdrawalReason = application.otherWithdrawalReason,
    )
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun getApplicationSubmittedForDomainEvent(
    application: ApprovedPremisesApplicationEntity,
    offenderDetails: OffenderDetailSummary,
    mappaLevel: String?,
    submitApplication: SubmitApprovedPremisesApplication,
    staffDetails: StaffDetail,
    caseDetail: CaseDetail,
  ): ApplicationSubmitted {
    return ApplicationSubmitted(
      applicationId = application.id,
      applicationUrl = applicationUrlTemplate.resolve("id", application.id.toString()),
      personReference = PersonReference(
        crn = application.crn,
        noms = offenderDetails.otherIds.nomsNumber ?: "Unknown NOMS Number",
      ),
      deliusEventNumber = application.eventNumber,
      mappa = mappaLevel,
      offenceId = application.offenceId,
      releaseType = submitApplication.releaseType.toString(),
      age = Period.between(offenderDetails.dateOfBirth, LocalDate.now()).years,
      gender = when (offenderDetails.gender.lowercase()) {
        "male" -> ApplicationSubmitted.Gender.male
        "female" -> ApplicationSubmitted.Gender.female
        else -> throw RuntimeException("Unknown gender: ${offenderDetails.gender}")
      },
      targetLocation = submitApplication.targetLocation,
      submittedAt = Instant.now(clock),
      submittedBy = getApplicationSubmittedSubmittedBy(staffDetails, caseDetail),
      sentenceLengthInMonths = null,
    )
  }

  private fun getApplicationSubmittedSubmittedBy(
    staffDetails: StaffDetail,
    caseDetail: CaseDetail,
  ): ApplicationSubmittedSubmittedBy {
    return ApplicationSubmittedSubmittedBy(
      staffMember = staffDetails.toStaffMember(),
      probationArea = domainEventTransformer.toProbationArea(staffDetails),
      team = getTeamFromCaseDetail(caseDetail),
      ldu = getLduFromCaseDetail(caseDetail),
      region = getRegionFromStaffDetails(staffDetails),
    )
  }

  private fun getLduFromCaseDetail(caseDetail: CaseDetail): Ldu {
    return Ldu(
      code = caseDetail.case.manager.team.ldu.code,
      name = caseDetail.case.manager.team.ldu.name,
    )
  }

  private fun getTeamFromCaseDetail(caseDetail: CaseDetail): Team {
    return Team(
      code = caseDetail.case.manager.team.code,
      name = caseDetail.case.manager.team.name,
    )
  }

  private fun getRegionFromStaffDetails(staffDetails: StaffDetail): Region {
    return Region(
      code = staffDetails.probationArea.code,
      name = staffDetails.probationArea.description,
    )
  }
}
