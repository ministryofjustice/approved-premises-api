package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequested
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.Instant
import java.util.UUID

@Service
class Cas1AssessmentDomainEventService(
  private val domainEventService: DomainEventService,
  private val communityApiClient: CommunityApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.assessment}") private val assessmentUrlTemplate: UrlTemplate,
) {

  fun assessmentAllocated(assessment: AssessmentEntity, allocatedToUser: UserEntity, allocatingUser: UserEntity?) {
    val allocatedToStaffDetails = when (val result = communityApiClient.getStaffUserDetails(allocatedToUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val allocatingUserStaffDetails = allocatingUser?.let {
      when (val result = communityApiClient.getStaffUserDetails(allocatingUser.deliusUsername)) {
        is ClientResult.Success -> result.body
        is ClientResult.Failure -> result.throwException()
      }
    }

    val id = UUID.randomUUID()
    val occurredAt = Instant.now()

    domainEventService.saveAssessmentAllocatedEvent(
      DomainEvent(
        id = id,
        applicationId = assessment.application.id,
        assessmentId = assessment.id,
        crn = assessment.application.crn,
        nomsNumber = assessment.application.nomsNumber,
        occurredAt = occurredAt,
        data = AssessmentAllocatedEnvelope(
          id = id,
          timestamp = occurredAt,
          eventType = EventType.assessmentAllocated,
          eventDetails = AssessmentAllocated(
            assessmentId = assessment.id,
            assessmentUrl = assessmentUrlTemplate.resolve("id", assessment.id.toString()),
            applicationId = assessment.application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", assessment.application.id.toString()),
            allocatedAt = Instant.now(),
            personReference = PersonReference(
              crn = assessment.application.crn,
              noms = assessment.application.nomsNumber ?: "Unknown NOMS Number",
            ),
            allocatedTo = allocatedToStaffDetails.toStaffMember(),
            allocatedBy = allocatingUserStaffDetails?.let {
              allocatingUserStaffDetails.toStaffMember()
            },
          ),
        ),
      ),
    )
  }

  fun furtherInformationRequested(assessment: AssessmentEntity, clarificationNoteEntity: AssessmentClarificationNoteEntity, emit: Boolean = true) {
    val requesterStaffDetails = when (val result = communityApiClient.getStaffUserDetails(clarificationNoteEntity.createdByUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val recipientStaffDetails = when (val result = communityApiClient.getStaffUserDetails(assessment.application.createdByUser.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    val id = UUID.randomUUID()
    val occurredAt = Instant.now()

    val data = FurtherInformationRequestedEnvelope(
      id = id,
      timestamp = occurredAt,
      eventType = EventType.informationRequestMade,
      eventDetails = FurtherInformationRequested(
        assessmentId = assessment.id,
        assessmentUrl = assessmentUrlTemplate.resolve("id", assessment.id.toString()),
        applicationId = assessment.application.id,
        applicationUrl = applicationUrlTemplate.resolve("id", assessment.application.id.toString()),
        personReference = PersonReference(
          crn = assessment.application.crn,
          noms = assessment.application.nomsNumber ?: "Unknown NOMS Number",
        ),
        requestedAt = Instant.now(),
        requester = requesterStaffDetails.toStaffMember(),
        recipient = recipientStaffDetails.toStaffMember(),
        requestId = clarificationNoteEntity.id,
      ),
    )

    val domainEvent = DomainEvent(
      id = id,
      applicationId = assessment.application.id,
      assessmentId = assessment.id,
      crn = assessment.application.crn,
      nomsNumber = assessment.application.nomsNumber,
      occurredAt = occurredAt,
      data = data,
    )

    domainEventService.saveFurtherInformationRequestedEvent(domainEvent, emit)
  }
}
