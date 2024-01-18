package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.transaction.Transactional
import kotlin.reflect.KClass

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService",
)
class DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  private val hmppsQueueService: HmppsQueueService,
  @Value("\${domain-events.cas2.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
  @Value("\${url-templates.api.cas2.application-submitted-event-detail}") private val cas2ApplicationSubmittedDetailUrlTemplate: String,
  @Value("\${url-templates.api.cas2.application-status-updated-event-detail}") private val cas2ApplicationStatusUpdatedDetailUrlTemplate: String,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private val domainTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingTopicException("domainevents not found")
  }

  fun getCas2ApplicationSubmittedDomainEvent(id: UUID) = get<Cas2ApplicationSubmittedEvent>(id)

  fun getCas2ApplicationStatusUpdatedDomainEvent(id: UUID) = get<Cas2ApplicationStatusUpdatedEvent>(id)

  private inline fun <reified T : Cas2Event> get(id: UUID): DomainEvent<T>? {
    val domainEventEntity = domainEventRepository.findByIdOrNull(id) ?: return null

    val data = when {
      enumTypeFromDataType(T::class) == domainEventEntity.type ->
        objectMapper.readValue(domainEventEntity.data, T::class.java)
      else -> throw RuntimeException("Unsupported DomainEventData type ${T::class.qualifiedName}/${domainEventEntity.type.name}")
    }
    return DomainEvent(
      id = domainEventEntity.id,
      applicationId = domainEventEntity.applicationId,
      bookingId = null,
      crn = domainEventEntity.crn,
      occurredAt = domainEventEntity.occurredAt.toInstant(),
      data = data,
    )
  }

  @Transactional
  fun saveCas2ApplicationSubmittedDomainEvent(domainEvent: DomainEvent<Cas2ApplicationSubmittedEvent>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "applications.cas2.application.submitted",
      typeDescription = "An application has been submitted for a CAS2 placement",
      detailUrl = cas2ApplicationSubmittedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      personReference = domainEvent.data.eventDetails.personReference,
    )

  @Transactional
  fun saveCas2ApplicationStatusUpdatedDomainEvent(domainEvent: DomainEvent<Cas2ApplicationStatusUpdatedEvent>) =
    saveAndEmit(
      domainEvent = domainEvent,
      typeName = "applications.cas2.application.status-updated",
      typeDescription = "An assessor has updated the status of a CAS2 application",
      detailUrl = cas2ApplicationStatusUpdatedDetailUrlTemplate.replace("#eventId", domainEvent.id.toString()),
      personReference = domainEvent.data.eventDetails.personReference,

    )

  private fun <T : Cas2Event> saveAndEmit(
    domainEvent: DomainEvent<T>,
    typeName: String,
    typeDescription: String,
    detailUrl: String,
    personReference: PersonReference,
  ) {
    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        crn = domainEvent.crn,
        type = enumTypeFromDataType(domainEvent.data::class),
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS2",
      ),
    )

    if (emitDomainEventsEnabled) {
      val personReferenceIdentifiers = listOf(
        SnsEventPersonReference("NOMS", personReference.noms),
        SnsEventPersonReference("CRN", personReference.crn.toString()),
      )

      val snsEvent = SnsEvent(
        eventType = typeName,
        version = 1,
        description = typeDescription,
        detailUrl = detailUrl,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        additionalInformation = SnsEventAdditionalInformation(
          applicationId = domainEvent.applicationId,
        ),
        personReference = SnsEventPersonReferenceCollection(
          identifiers = personReferenceIdentifiers,
        ),
      )

      val publishResult = domainTopic.snsClient.publish(
        PublishRequest(domainTopic.arn, objectMapper.writeValueAsString(snsEvent))
          .withMessageAttributes(mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(snsEvent.eventType))),
      )

      log.info("Emitted SNS event (Message Id: ${publishResult.messageId}, Sequence Id: ${publishResult.sequenceNumber}) for Domain Event: ${domainEvent.id} of type: ${snsEvent.eventType}")
    } else {
      log.info("Not emitting SNS event for domain event because domain-events.cas2.emit-enabled is not enabled")
    }
  }

  private fun <T : Cas2Event> enumTypeFromDataType(type: KClass<T>): DomainEventType = when (type) {
    Cas2ApplicationSubmittedEvent::class -> DomainEventType.CAS2_APPLICATION_SUBMITTED
    Cas2ApplicationStatusUpdatedEvent::class -> DomainEventType.CAS2_APPLICATION_STATUS_UPDATED
    else -> throw RuntimeException("Unrecognised domain event type: ${type.qualifiedName}")
  }
}
