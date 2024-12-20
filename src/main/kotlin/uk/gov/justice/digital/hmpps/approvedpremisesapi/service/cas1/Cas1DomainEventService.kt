package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TriggerSourceType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventAdditionalInformation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.domainevent.SnsEventPersonReferenceCollection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.reflect.KClass

@SuppressWarnings("TooManyFunctions")
@Service
class Cas1DomainEventService(
  private val objectMapper: ObjectMapper,
  private val domainEventRepository: DomainEventRepository,
  val domainEventWorker: ConfiguredDomainEventWorker,
  private val userService: UserService,
  @Value("\${domain-events.cas1.emit-enabled}") private val emitDomainEventsEnabled: Boolean,
  private val domainEventUrlConfig: DomainEventUrlConfig,
  private val cas1DomainEventMigrationService: Cas1DomainEventMigrationService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getApplicationSubmittedDomainEvent(id: UUID) = get(id, ApplicationSubmittedEnvelope::class)
  fun getApplicationAssessedDomainEvent(id: UUID) = get(id, ApplicationAssessedEnvelope::class)
  fun getBookingMadeEvent(id: UUID) = get(id, BookingMadeEnvelope::class)
  fun getPersonArrivedEvent(id: UUID) = get(id, PersonArrivedEnvelope::class)
  fun getPersonNotArrivedEvent(id: UUID) = get(id, PersonNotArrivedEnvelope::class)
  fun getPersonDepartedEvent(id: UUID) = get(id, PersonDepartedEnvelope::class)
  fun getBookingNotMadeEvent(id: UUID) = get(id, BookingNotMadeEnvelope::class)
  fun getBookingCancelledEvent(id: UUID) = get(id, BookingCancelledEnvelope::class)
  fun getBookingChangedEvent(id: UUID) = get(id, BookingChangedEnvelope::class)
  fun getBookingKeyWorkerAssignedEvent(id: UUID) = get(id, BookingKeyWorkerAssignedEnvelope::class)
  fun getApplicationWithdrawnEvent(id: UUID) = get(id, ApplicationWithdrawnEnvelope::class)
  fun getApplicationExpiredEvent(id: UUID) = get(id, ApplicationExpiredEnvelope::class)
  fun getPlacementApplicationWithdrawnEvent(id: UUID) = get(id, PlacementApplicationWithdrawnEnvelope::class)
  fun getPlacementApplicationAllocatedEvent(id: UUID) = get(id, PlacementApplicationAllocatedEnvelope::class)
  fun getMatchRequestWithdrawnEvent(id: UUID) = get(id, MatchRequestWithdrawnEnvelope::class)
  fun getAssessmentAppealedEvent(id: UUID) = get(id, AssessmentAppealedEnvelope::class)
  fun getAssessmentAllocatedEvent(id: UUID) = get(id, AssessmentAllocatedEnvelope::class)
  fun getRequestForPlacementCreatedEvent(id: UUID) = get(id, RequestForPlacementCreatedEnvelope::class)
  fun getRequestForPlacementAssessedEvent(id: UUID) = get(id, RequestForPlacementAssessedEnvelope::class)
  fun getFurtherInformationRequestMadeEvent(id: UUID) = get(id, FurtherInformationRequestedEnvelope::class)

  private fun <T : Any> get(id: UUID, type: KClass<T>): DomainEvent<T>? {
    val entity = domainEventRepository.findByIdOrNull(id) ?: return null
    return toDomainEvent(entity, type)
  }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown")
  fun <T : Any> toDomainEvent(entity: DomainEventEntity, type: KClass<T>): DomainEvent<T> {
    checkNotNull(entity.applicationId) { "application id should not be null" }

    val dataJson = when {
      type == BookingCancelledEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED ->
        cas1DomainEventMigrationService.bookingCancelledJson(entity)
      type == PersonArrivedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED ->
        cas1DomainEventMigrationService.personArrivedJson(entity)
      type == PersonDepartedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED ->
        cas1DomainEventMigrationService.personDepartedJson(entity)
      (type == ApplicationSubmittedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED) ||
        (type == ApplicationAssessedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED) ||
        (type == BookingMadeEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_BOOKING_MADE) ||
        (type == PersonNotArrivedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED) ||
        (type == BookingNotMadeEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE) ||
        (type == BookingChangedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED) ||
        (type == BookingKeyWorkerAssignedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED) ||
        (type == ApplicationWithdrawnEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN) ||
        (type == ApplicationExpiredEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED) ||
        (type == AssessmentAppealedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED) ||
        (type == PlacementApplicationWithdrawnEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN) ||
        (type == PlacementApplicationAllocatedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED) ||
        (type == MatchRequestWithdrawnEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN) ||
        (type == AssessmentAllocatedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED) ||
        (type == RequestForPlacementCreatedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED) ||
        (type == RequestForPlacementAssessedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED) ||
        (type == FurtherInformationRequestedEnvelope::class && entity.type == DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED) ->
        entity.data
      else -> throw RuntimeException("Unsupported DomainEventData type ${type.qualifiedName}/${entity.type.name}")
    }

    val data = objectMapper.readValue(dataJson, type.java)

    return DomainEvent(
      id = entity.id,
      applicationId = entity.applicationId,
      crn = entity.crn,
      nomsNumber = entity.nomsNumber,
      occurredAt = entity.occurredAt.toInstant(),
      data = data,
    )
  }

  @Transactional
  fun saveApplicationSubmittedDomainEvent(domainEvent: DomainEvent<ApplicationSubmittedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED,
    )

  @Transactional
  fun saveApplicationAssessedDomainEvent(domainEvent: DomainEvent<ApplicationAssessedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED,
    )

  @Transactional
  fun saveBookingMadeDomainEvent(domainEvent: DomainEvent<BookingMadeEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_MADE,
    )

  @Transactional
  fun savePersonArrivedEvent(domainEvent: DomainEvent<PersonArrivedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PERSON_ARRIVED,
    )

  @Transactional
  fun savePersonNotArrivedEvent(domainEvent: DomainEvent<PersonNotArrivedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PERSON_NOT_ARRIVED,
      emit = emit,
    )

  @Transactional
  fun savePersonDepartedEvent(domainEvent: DomainEvent<PersonDepartedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PERSON_DEPARTED,
    )

  @Transactional
  fun saveBookingNotMadeEvent(domainEvent: DomainEvent<BookingNotMadeEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_NOT_MADE,
    )

  @Transactional
  fun saveBookingCancelledEvent(domainEvent: DomainEvent<BookingCancelledEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CANCELLED,
    )

  @Transactional
  fun saveBookingChangedEvent(domainEvent: DomainEvent<BookingChangedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_CHANGED,
    )

  @Transactional
  fun saveApplicationWithdrawnEvent(domainEvent: DomainEvent<ApplicationWithdrawnEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_WITHDRAWN,
      emit = emit,
    )

  @Transactional
  fun saveAssessmentAppealedEvent(domainEvent: DomainEvent<AssessmentAppealedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_APPEALED,
    )

  @Transactional
  fun savePlacementApplicationWithdrawnEvent(domainEvent: DomainEvent<PlacementApplicationWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN,
    )

  @Transactional
  fun savePlacementApplicationAllocatedEvent(domainEvent: DomainEvent<PlacementApplicationAllocatedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED,
    )

  @Transactional
  fun saveMatchRequestWithdrawnEvent(domainEvent: DomainEvent<MatchRequestWithdrawnEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN,
    )

  @Transactional
  fun saveRequestForPlacementCreatedEvent(domainEvent: DomainEvent<RequestForPlacementCreatedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED,
      emit = emit,
    )

  @Transactional
  fun saveRequestForPlacementAssessedEvent(domainEvent: DomainEvent<RequestForPlacementAssessedEnvelope>) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED,
    )

  @Transactional
  fun saveApplicationExpiredEvent(
    domainEvent: DomainEvent<ApplicationExpiredEnvelope>,
    triggerSource: TriggerSourceType,
    emit: Boolean,
  ) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_APPLICATION_EXPIRED,
      triggerSource = triggerSource,
      emit = emit,
    )

  @Transactional
  fun saveAssessmentAllocatedEvent(domainEvent: DomainEvent<AssessmentAllocatedEnvelope>, triggerSource: TriggerSourceType) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_ALLOCATED,
      triggerSource = triggerSource,
    )

  @Transactional
  fun saveFurtherInformationRequestedEvent(domainEvent: DomainEvent<FurtherInformationRequestedEnvelope>, emit: Boolean = true) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED,
      emit = emit,
    )

  @Transactional
  fun saveKeyWorkerAssignedEvent(domainEvent: DomainEvent<BookingKeyWorkerAssignedEnvelope>, emit: Boolean) =
    saveAndEmit(
      domainEvent = domainEvent,
      eventType = DomainEventType.APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED,
      emit = emit,
    )

  fun getAllDomainEventsForApplication(applicationId: UUID) =
    getAllDomainEventsById(applicationId = applicationId)

  fun getAllDomainEventsForSpaceBooking(spaceBookingId: UUID) =
    getAllDomainEventsById(spaceBookingId = spaceBookingId)

  private fun getAllDomainEventsById(applicationId: UUID? = null, spaceBookingId: UUID? = null) =
    domainEventRepository.findAllTimelineEventsByIds(applicationId, spaceBookingId).distinctBy { it.id }

  @Transactional
  fun saveAndEmit(
    domainEvent: DomainEvent<*>,
    eventType: DomainEventType,
    emit: Boolean = true,
    triggerSource: TriggerSourceType? = null,
  ) {
    val domainEventEntity = domainEventRepository.save(
      DomainEventEntity(
        id = domainEvent.id,
        applicationId = domainEvent.applicationId,
        assessmentId = domainEvent.assessmentId,
        bookingId = domainEvent.bookingId,
        cas1SpaceBookingId = domainEvent.cas1SpaceBookingId,
        crn = domainEvent.crn,
        type = eventType,
        occurredAt = domainEvent.occurredAt.atOffset(ZoneOffset.UTC),
        createdAt = OffsetDateTime.now(),
        data = objectMapper.writeValueAsString(domainEvent.data),
        service = "CAS1",
        triggerSource = triggerSource,
        triggeredByUserId = userService.getUserForRequestOrNull()?.id,
        nomsNumber = domainEvent.nomsNumber,
        metadata = domainEvent.metadata,
        schemaVersion = domainEvent.schemaVersion,
      ),
    )

    if (emit) {
      emit(domainEventEntity)
    }
  }

  fun replay(domainEventId: UUID) {
    val domainEventEntity = domainEventRepository.findByIdOrNull(domainEventId)
      ?: throw NotFoundProblem(domainEventId, "DomainEvent")

    emit(domainEventEntity)
  }

  private fun emit(
    domainEvent: DomainEventEntity,
  ) {
    if (!emitDomainEventsEnabled) {
      log.info("Not emitting SNS event for domain event because domain-events.cas1.emit-enabled is not enabled")
      return
    }

    val eventType = domainEvent.type
    val typeName = eventType.typeName
    val typeDescription = eventType.typeDescription
    val crn = domainEvent.crn
    val nomsNumber = domainEvent.nomsNumber ?: "Unknown NOMS Number"
    val detailUrl = domainEventUrlConfig.getUrlForDomainEventId(eventType, domainEvent.id)

    domainEventWorker.emitEvent(
      SnsEvent(
        eventType = typeName,
        version = 1,
        description = typeDescription,
        detailUrl = detailUrl,
        occurredAt = domainEvent.occurredAt,
        additionalInformation = SnsEventAdditionalInformation(
          applicationId = domainEvent.applicationId,
        ),
        personReference = SnsEventPersonReferenceCollection(
          identifiers = listOf(
            SnsEventPersonReference("CRN", crn),
            SnsEventPersonReference("NOMS", nomsNumber),
          ),
        ),
      ),
      domainEvent.id,
    )
  }
}
