package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import io.hypersistence.utils.hibernate.type.json.JsonType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.MapKeyEnumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEventSummary
import java.time.OffsetDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType as Cas1EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.EventType as Cas2EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.EventType as Cas3EventType

@Repository
interface DomainEventRepository : JpaRepository<DomainEventEntity, UUID> {

  @Query(
    """
     SELECT 
        d.id as id,
        d.type as type, 
        d.occurredAt as occurredAt,
        d.applicationId as applicationId,
        d.assessmentId as assessmentId,
        d.bookingId as bookingId,
        d.triggerSource as triggerSource,
        CASE
            WHEN b.id IS NOT NULL THEN b.premises.id
            WHEN sb.id IS NOT NULL THEN sb.premises.id
            ELSE NULL
        END as premisesId,
        a.id as appealId,
        d.cas1SpaceBookingId as cas1SpaceBookingId,
        u as triggeredByUser
      FROM DomainEventEntity d 
      LEFT OUTER JOIN BookingEntity b ON b.id = d.bookingId
      LEFT OUTER JOIN Cas1SpaceBookingEntity sb ON sb.id = d.cas1SpaceBookingId
      LEFT OUTER JOIN AppealEntity a ON a.application.id = d.applicationId 
      LEFT OUTER JOIN UserEntity u ON u.id = d.triggeredByUserId
      WHERE
        (
            (:applicationId IS NULL) OR (
                d.applicationId = :applicationId
            )
        ) AND (
            (:spaceBookingId IS NULL) OR (
                d.cas1SpaceBookingId = :spaceBookingId
            )
        )
    """,
  )
  fun findAllTimelineEventsByIds(applicationId: UUID?, spaceBookingId: UUID?): List<DomainEventSummary>

  @Query(
    "SELECT * FROM domain_events domain_event " +
      "where date_part('month', domain_event.occurred_at) = :month " +
      "AND date_part('year', domain_event.occurred_at) = :year ",
    nativeQuery = true,
  )
  fun findAllCreatedInMonth(month: Int, year: Int): List<DomainEventEntity>

  fun findByApplicationId(applicationId: UUID): List<DomainEventEntity>

  fun findByType(type: DomainEventType): List<DomainEventEntity>

  @Query(
    """SELECT id FROM DomainEventEntity where type = :type AND bookingId = :bookingId""",
  )
  fun findIdsByTypeAndBookingId(type: DomainEventType, bookingId: UUID): List<UUID>

  fun findByAssessmentIdAndType(assessmentId: UUID, type: DomainEventType): List<DomainEventEntity>

  @Modifying
  @Query(
    """
    UPDATE domain_events 
    SET
        data = CAST(:updatedData as jsonb)
    WHERE id = :id
    """,
    nativeQuery = true,
  )
  fun updateData(id: UUID, updatedData: String)

  @Modifying
  @Query(
    """
      UPDATE domain_events
      SET 
        booking_id = null,
        cas1_space_booking_id = booking_id
      WHERE booking_id = :bookingId  
    """,
    nativeQuery = true,
  )
  fun replaceBookingIdWithSpaceBookingId(bookingId: UUID)
}

@Entity
@Table(name = "domain_events")
data class DomainEventEntity(
  @Id
  val id: UUID,
  val applicationId: UUID?,
  val assessmentId: UUID?,
  val bookingId: UUID?,
  @Column(name = "cas1_space_booking_id")
  val cas1SpaceBookingId: UUID?,
  val crn: String,
  @Enumerated(value = EnumType.STRING)
  val type: DomainEventType,
  val occurredAt: OffsetDateTime,
  val createdAt: OffsetDateTime,
  @Type(JsonType::class)
  val data: String,
  val service: String,
  @Enumerated(value = EnumType.STRING)
  val triggerSource: TriggerSourceType? = null,
  val triggeredByUserId: UUID?,
  val nomsNumber: String?,
  @ElementCollection
  @MapKeyColumn(name = "name")
  @MapKeyEnumerated(EnumType.STRING)
  @Column(name = "value")
  @CollectionTable(name = "domain_events_metadata", joinColumns = [ JoinColumn(name = "domain_event_id") ])
  val metadata: Map<MetaDataName, String?> = emptyMap(),
  /**
   * Use to track the schema version used for the [data] property. The schema version
   * will be specific to the corresponding [type]. This version number relates to
   * [DomainEventSchemaVersion.versionNo]
   *
   * This will be null for any domain events recorded before this concept was introduced.
   *
   * The domain event schema version used for a given domain event record is only required when the
   * initial domain event schema changes in a way that we need to start tracking which schema a domain event uses.
   *
   * For information on introducing new schema versions, see the 'modifying_domain_event_schemas.md' file
   */
  val schemaVersion: Int?,
)

enum class TriggerSourceType { USER, SYSTEM }

enum class MetaDataName {
  CAS1_APP_REASON_FOR_SHORT_NOTICE,
  CAS1_APP_REASON_FOR_SHORT_NOTICE_OTHER,
  CAS1_PLACEMENT_APPLICATION_ID,
  CAS1_REQUESTED_AP_TYPE,
  CAS1_PLACEMENT_REQUEST_ID,
  CAS1_CANCELLATION_ID,
}

enum class DomainEventCas {
  CAS1,
  CAS2,
  CAS3,
}

data class DomainEventSchemaVersion(val versionNo: Int?, val description: String?)

val DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION = DomainEventSchemaVersion(
  versionNo = null,
  description = "The initial version of this domain event",
)

enum class DomainEventType(
  val cas: DomainEventCas,
  val typeName: String,
  val typeDescription: String,
  val timelineEventType: TimelineEventType?,
  val schemaVersions: List<DomainEventSchemaVersion> = listOf(DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION),
) {
  APPROVED_PREMISES_APPLICATION_SUBMITTED(
    DomainEventCas.CAS1,
    Cas1EventType.applicationSubmitted.value,
    "An application has been submitted for an Approved Premises placement",
    TimelineEventType.approvedPremisesApplicationSubmitted,
  ),
  APPROVED_PREMISES_APPLICATION_ASSESSED(
    DomainEventCas.CAS1,
    Cas1EventType.applicationAssessed.value,
    "An application has been assessed for an Approved Premises placement",
    TimelineEventType.approvedPremisesApplicationAssessed,
  ),
  APPROVED_PREMISES_APPLICATION_EXPIRED(
    DomainEventCas.CAS1,
    Cas1EventType.applicationExpired.value,
    "An Approved Premises application has expired",
    TimelineEventType.approvedPremisesApplicationExpired,
  ),
  APPROVED_PREMISES_BOOKING_MADE(
    DomainEventCas.CAS1,
    Cas1EventType.bookingMade.value,
    "An Approved Premises booking has been made",
    TimelineEventType.approvedPremisesBookingMade,
    schemaVersions = listOf(
      DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION,
      DomainEventSchemaVersion(2, "Added characteristics field"),
    ),
  ),
  APPROVED_PREMISES_PERSON_ARRIVED(
    DomainEventCas.CAS1,
    Cas1EventType.personArrived.value,
    "Someone has arrived at an Approved Premises for their Booking",
    TimelineEventType.approvedPremisesPersonArrived,
    schemaVersions = listOf(
      DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION,
      DomainEventSchemaVersion(2, "Added recordedBy field"),
    ),
  ),
  APPROVED_PREMISES_PERSON_NOT_ARRIVED(
    DomainEventCas.CAS1,
    Cas1EventType.personNotArrived.value,
    "Someone has failed to arrive at an Approved Premises for their Booking",
    TimelineEventType.approvedPremisesPersonNotArrived,
  ),
  APPROVED_PREMISES_PERSON_DEPARTED(
    DomainEventCas.CAS1,
    Cas1EventType.personDeparted.value,
    "Someone has left an Approved Premises",
    TimelineEventType.approvedPremisesPersonDeparted,
    schemaVersions = listOf(
      DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION,
      DomainEventSchemaVersion(2, "Added recordedBy field"),
    ),
  ),
  APPROVED_PREMISES_BOOKING_NOT_MADE(
    DomainEventCas.CAS1,
    Cas1EventType.bookingNotMade.value,
    "It was not possible to create a Booking on this attempt",
    TimelineEventType.approvedPremisesBookingNotMade,
  ),
  APPROVED_PREMISES_BOOKING_CANCELLED(
    DomainEventCas.CAS1,
    Cas1EventType.bookingCancelled.value,
    "An Approved Premises Booking has been cancelled",
    TimelineEventType.approvedPremisesBookingCancelled,
    schemaVersions = listOf(
      DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION,
      DomainEventSchemaVersion(2, "Added mandatory cancelledAtDate and cancellationRecordedAt fields"),
    ),
  ),
  APPROVED_PREMISES_BOOKING_CHANGED(
    DomainEventCas.CAS1,
    Cas1EventType.bookingChanged.value,
    "An Approved Premises Booking has been changed",
    TimelineEventType.approvedPremisesBookingChanged,
    schemaVersions = listOf(
      DEFAULT_DOMAIN_EVENT_SCHEMA_VERSION,
      DomainEventSchemaVersion(2, "Captures previous set values, if changed."),
    ),
  ),
  APPROVED_PREMISES_BOOKING_KEYWORKER_ASSIGNED(
    DomainEventCas.CAS1,
    Cas1EventType.bookingKeyWorkerAssigned.value,
    "A keyworker has been assigned to the booking",
    TimelineEventType.approvedPremisesBookingKeyworkerAssigned,
  ),
  APPROVED_PREMISES_APPLICATION_WITHDRAWN(
    DomainEventCas.CAS1,
    Cas1EventType.applicationWithdrawn.value,
    "An Approved Premises Application has been withdrawn",
    TimelineEventType.approvedPremisesApplicationWithdrawn,
  ),
  APPROVED_PREMISES_ASSESSMENT_APPEALED(
    DomainEventCas.CAS1,
    Cas1EventType.assessmentAppealed.value,
    "An Approved Premises Assessment has been appealed",
    TimelineEventType.approvedPremisesAssessmentAppealed,
  ),
  APPROVED_PREMISES_ASSESSMENT_ALLOCATED(
    DomainEventCas.CAS1,
    Cas1EventType.assessmentAllocated.value,
    "An Approved Premises Assessment has been allocated",
    TimelineEventType.approvedPremisesAssessmentAllocated,
  ),
  APPROVED_PREMISES_ASSESSMENT_INFO_REQUESTED(
    DomainEventCas.CAS1,
    Cas1EventType.informationRequestMade.value,
    "An information request has been made for an Approved Premises Assessment",
    TimelineEventType.approvedPremisesInformationRequest,
  ),
  APPROVED_PREMISES_PLACEMENT_APPLICATION_WITHDRAWN(
    DomainEventCas.CAS1,
    Cas1EventType.placementApplicationWithdrawn.value,
    "An Approved Premises Request for Placement has been withdrawn",
    TimelineEventType.approvedPremisesPlacementApplicationWithdrawn,
  ),
  APPROVED_PREMISES_PLACEMENT_APPLICATION_ALLOCATED(
    DomainEventCas.CAS1,
    Cas1EventType.placementApplicationAllocated.value,
    "An Approved Premises Request for Placement has been allocated",
    TimelineEventType.approvedPremisesPlacementApplicationAllocated,
  ),
  APPROVED_PREMISES_MATCH_REQUEST_WITHDRAWN(
    DomainEventCas.CAS1,
    Cas1EventType.matchRequestWithdrawn.value,
    "An Approved Premises Match Request has been withdrawn",
    TimelineEventType.approvedPremisesMatchRequestWithdrawn,
  ),
  APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_CREATED(
    DomainEventCas.CAS1,
    Cas1EventType.requestForPlacementCreated.value,
    "An Approved Premises Request for Placement has been created",
    TimelineEventType.approvedPremisesRequestForPlacementCreated,
  ),
  APPROVED_PREMISES_REQUEST_FOR_PLACEMENT_ASSESSED(
    DomainEventCas.CAS1,
    Cas1EventType.requestForPlacementAssessed.value,
    "An request for placement has been assessed",
    TimelineEventType.approvedPremisesRequestForPlacementAssessed,
  ),
  CAS2_APPLICATION_SUBMITTED(
    DomainEventCas.CAS2,
    Cas2EventType.applicationSubmitted.value,
    "An application has been submitted for a CAS2 placement",
    null,
  ),
  CAS2_APPLICATION_STATUS_UPDATED(
    DomainEventCas.CAS2,
    Cas2EventType.applicationStatusUpdated.value,
    "An assessor has updated the status of a CAS2 application",
    null,
  ),
  CAS3_BOOKING_CANCELLED(
    DomainEventCas.CAS3,
    Cas3EventType.bookingCancelled.value,
    "A booking for a Transitional Accommodation premises has been cancelled",
    null,
  ),
  CAS3_BOOKING_CONFIRMED(
    DomainEventCas.CAS3,
    Cas3EventType.bookingConfirmed.value,
    "A booking has been confirmed for a Transitional Accommodation premises",
    null,
  ),
  CAS3_BOOKING_PROVISIONALLY_MADE(
    DomainEventCas.CAS3,
    Cas3EventType.bookingProvisionallyMade.value,
    "A booking has been provisionally made for a Transitional Accommodation premises",
    null,
  ),
  CAS3_PERSON_ARRIVED(
    DomainEventCas.CAS3,
    Cas3EventType.personArrived.value,
    "Someone has arrived at a Transitional Accommodation premises for their booking",
    null,
  ),
  CAS3_PERSON_ARRIVED_UPDATED(
    DomainEventCas.CAS3,
    Cas3EventType.personArrivedUpdated.value,
    "Someone has changed arrival date at a Transitional Accommodation premises for their booking",
    null,
  ),
  CAS3_PERSON_DEPARTED(
    DomainEventCas.CAS3,
    Cas3EventType.personDeparted.value,
    "Someone has left a Transitional Accommodation premises",
    null,
  ),
  CAS3_REFERRAL_SUBMITTED(
    DomainEventCas.CAS3,
    Cas3EventType.referralSubmitted.value,
    "A referral for Transitional Accommodation has been submitted",
    null,
  ),
  CAS3_PERSON_DEPARTURE_UPDATED(
    DomainEventCas.CAS3,
    Cas3EventType.personDepartureUpdated.value,
    "Person has updated departure date of Transitional Accommodation premises",
    null,
  ),
  CAS3_BOOKING_CANCELLED_UPDATED(
    DomainEventCas.CAS3,
    Cas3EventType.bookingCancelledUpdated.value,
    "A cancelled booking for a Transitional Accommodation premises has been updated",
    null,
  ),
  CAS3_ASSESSMENT_UPDATED(
    DomainEventCas.CAS3,
    Cas3EventType.assessmentUpdated.value,
    "A field has been updated on an assessment",
    null,
  ),
  CAS3_DRAFT_REFERRAL_DELETED(
    DomainEventCas.CAS3,
    Cas3EventType.draftReferralDeleted.value,
    "A draft referral has been deleted",
    null,
  ),
}
