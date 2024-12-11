package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail

import io.sentry.Sentry
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEventDetails
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Status
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.ExternalUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.ValidationErrors
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EmailNotificationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.Constants.HDC_APPLICATION_TYPE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationStatusTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toCas2UiFormattedHourOfDay
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2BailStatusUpdateService")
class Cas2BailStatusUpdateService(
  private val cas2BailAssessmentRepository: Cas2BailAssessmentRepository,
  private val cas2BailStatusUpdateRepository: Cas2BailStatusUpdateRepository,
  private val cas2BailStatusUpdateDetailRepository: Cas2BailStatusUpdateDetailRepository,
  private val domainEventService: DomainEventService,
  private val emailNotificationService: EmailNotificationService,
  private val notifyConfig: NotifyConfig,
  private val statusFinder: Cas2PersistedApplicationStatusFinder,
  private val statusTransformer: ApplicationStatusTransformer,
  @Value("\${url-templates.frontend.cas2.application}") private val applicationUrlTemplate: String,
  @Value("\${url-templates.frontend.cas2.application-overview}") private val applicationOverviewUrlTemplate: String,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  fun isValidStatus(statusUpdate: Cas2AssessmentStatusUpdate): Boolean {
    return findActiveStatusByName(statusUpdate.newStatus) != null
  }

  @Transactional
  @SuppressWarnings("ReturnCount")
  fun createForAssessment(
    assessmentId: UUID,
    statusUpdate: Cas2AssessmentStatusUpdate,
    assessor: ExternalUserEntity,
  ): CasResult<Cas2BailStatusUpdateEntity> {
    val assessment = cas2BailAssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound()

    val status = findActiveStatusByName(statusUpdate.newStatus)
      ?: return CasResult.GeneralValidationError("The status ${statusUpdate.newStatus} is not valid")

    val newDetails = statusUpdate.newStatusDetails.isNullOrEmpty()
    val statusDetails = if (newDetails) {
      emptyList()
    } else {
      statusUpdate.newStatusDetails?.map { detail ->
        status.findStatusDetailOnStatus(detail)
          ?: return CasResult.GeneralValidationError("The status detail $detail is not valid")
      }
    }

    if (ValidationErrors().any()) {
      return CasResult.FieldValidationError(ValidationErrors())
    }

    val createdStatusUpdate = cas2BailStatusUpdateRepository.save(
      Cas2BailStatusUpdateEntity(
        id = UUID.randomUUID(),
        assessment = assessment,
        application = assessment.application,
        assessor = assessor,
        statusId = status.id,
        description = status.description,
        label = status.label,
        createdAt = OffsetDateTime.now(),
      ),
    )

    statusDetails?.forEach { detail ->
      cas2BailStatusUpdateDetailRepository.save(
        Cas2BailStatusUpdateDetailEntity(
          id = UUID.randomUUID(),
          statusDetailId = detail.id,
          statusUpdate = createdStatusUpdate,
          label = detail.label,
        ),
      )
    }

    sendEmailStatusUpdated(assessment.application.createdByUser, assessment.application, createdStatusUpdate)

    createStatusUpdatedDomainEvent(createdStatusUpdate, statusDetails)

    return CasResult.Success(createdStatusUpdate)
  }

  private fun findActiveStatusByName(statusName: String): Cas2PersistedApplicationStatus? {
    return statusFinder.active()
      .find { status -> status.name == statusName }
  }

  fun createStatusUpdatedDomainEvent(
    statusUpdate: Cas2BailStatusUpdateEntity,
    statusDetails: List<Cas2PersistedApplicationStatusDetail>? = emptyList(),
  ) {
    val domainEventId = UUID.randomUUID()
    val eventOccurredAt = statusUpdate.createdAt
    val application = statusUpdate.application
    val newStatus = statusUpdate.status()
    val assessor = statusUpdate.assessor

    domainEventService.saveCas2ApplicationStatusUpdatedDomainEvent(
      DomainEvent(
        id = domainEventId,
        applicationId = application.id,
        crn = application.crn,
        nomsNumber = application.nomsNumber,
        occurredAt = eventOccurredAt.toInstant(),
        data = Cas2ApplicationStatusUpdatedEvent(
          id = domainEventId,
          timestamp = eventOccurredAt.toInstant(),
          eventType = EventType.applicationStatusUpdated,
          eventDetails = Cas2ApplicationStatusUpdatedEventDetails(
            applicationId = application.id,
            applicationUrl = applicationUrlTemplate.replace("#id", application.id.toString()),
            personReference = PersonReference(
              crn = application.crn,
              noms = application.nomsNumber.toString(),
            ),
            newStatus = Cas2Status(
              name = newStatus.name,
              description = newStatus.description,
              label = newStatus.label,
              statusDetails = statusDetails?.let { statusTransformer.transformStatusDetailListToDetailItemList(it) },
            ),
            updatedBy = ExternalUser(
              username = assessor.username,
              name = assessor.name,
              email = assessor.email,
              origin = assessor.origin,
            ),
            updatedAt = eventOccurredAt.toInstant(),
          ),
        ),
      ),
    )
  }

  private fun sendEmailStatusUpdated(user: NomisUserEntity, application: Cas2BailApplicationEntity, status: Cas2BailStatusUpdateEntity) {
    if (application.createdByUser.email != null) {
      emailNotificationService.sendCas2Email(
        recipientEmailAddress = user.email!!,
        templateId = notifyConfig.templates.cas2ApplicationStatusUpdated,
        personalisation = mapOf(
          "applicationStatus" to status.label,
          "dateStatusChanged" to status.createdAt.toLocalDate().toCas2UiFormat(),
          "timeStatusChanged" to status.createdAt.toCas2UiFormattedHourOfDay(),
          "applicationType" to HDC_APPLICATION_TYPE,
          "nomsNumber" to application.nomsNumber,
          "applicationUrl" to applicationOverviewUrlTemplate.replace("#id", application.id.toString()),
        ),
      )
    } else {
      log.error("Email not found for User ${application.createdByUser.id}. Unable to send email when updating status of Application ${application.id}")
      Sentry.captureMessage("Email not found for User ${application.createdByUser.id}. Unable to send email when updating status of Application ${application.id}")
    }
  }
}
