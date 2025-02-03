package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2Event
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
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
import kotlin.reflect.KClass

@Service(
  "uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.DomainEventService",
)
class DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  private val hmppsQueueService: HmppsQueueService,
  @Value("\${domain-events.cas2.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
  private val domainEventUrlConfig: DomainEventUrlConfig,
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
      nomsNumber = domainEventEntity.nomsNumber,
    )
  }

  @Transactional
  fun saveCas2ApplicationSubmittedDomainEvent(domainEvent: DomainEvent<Cas2ApplicationSubmittedEvent>) =
    saveAndEmit(
      domainEvent = domainEvent,
      personReference = domainEvent.data.eventDetails.personReference,
    )

  @Transactional
  fun saveCas2ApplicationStatusUpdatedDomainEvent(domainEvent: DomainEvent<Cas2ApplicationStatusUpdatedEvent>) =
    saveAndEmit(
      domainEvent = domainEvent,
      personReference = domainEvent.data.eventDetails.personReference,
    )

  private fun <T : Cas2Event> saveAndEmit(
    domainEvent: DomainEvent<T>,
    personReference: PersonReference,
  ) {
    val enumType = enumTypeFromDataType(domainEvent.data::class)
    val typeName = enumType.typeName
    val typeDescription = enumType.typeDescription
    val detailUrl = domainEventUrlConfig.getUrlForDomainEventId(enumType, domainEvent.id)

    domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        cas1SpaceBookingId = domainEvent.cas1SpaceBookingId,
        crn = domainEvent.crn,
        nomsNumber = domainEvent.nomsNumber,
        type = enumTypeFromDataType(domainEvent.data::class),
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS2",
        triggerSource = null,
        triggeredByUserId = null,
        schemaVersion = domainEvent.schemaVersion,
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
        PublishRequest.builder()
          .topicArn(domainTopic.arn)
          .message(objectMapper.writeValueAsString(snsEvent))
          .messageAttributes(
            mapOf(
              "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(snsEvent.eventType).build(),
            ),
          ).build(),
      ).get()

      log.info("Emitted SNS event (Message Id: ${publishResult.messageId()}, Sequence Id: ${publishResult.sequenceNumber()}) for Domain Event: ${domainEvent.id} of type: ${snsEvent.eventType}")
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
