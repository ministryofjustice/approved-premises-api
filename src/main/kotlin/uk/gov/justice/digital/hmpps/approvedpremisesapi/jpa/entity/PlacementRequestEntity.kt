package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.LockModeType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Immutable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Service
@Suppress("FunctionNaming")
@Repository
interface PlacementRequestRepository : JpaRepository<PlacementRequestEntity, UUID> {
  fun findByApplicationId(applicationId: UUID): List<PlacementRequestEntity>

  fun findByApplication(application: ApprovedPremisesApplicationEntity): List<PlacementRequestEntity>

  @Query(
    """
    SELECT
      pq.*,
      application.created_at as application_date,
      CASE
        WHEN (pq.is_parole) THEN 'parole'
        ELSE 'standardRequest'
      END as request_type,
      apa.name as person_name,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' as person_risks_tier
    from
      placement_requests pq
      left join approved_premises_applications apa on apa.id = pq.application_id
      left join ap_areas area on area.id = apa.ap_area_id
      left join applications application on application.id = pq.application_id
    where
      pq.reallocated_at IS NULL 
      AND (:status IS NULL OR pq.is_withdrawn IS FALSE)
      AND (:status IS NULL OR (
        CASE
          WHEN EXISTS (
            SELECT
              1
            from
              cancellations c
              right join bookings booking on c.booking_id = booking.id
            WHERE
              booking.id = pq.booking_id
              AND c.id IS NULL
          ) THEN 'matched'
          WHEN EXISTS (
            SELECT 
                1 
            FROM 
                cas1_space_bookings sb 
            WHERE
                sb.placement_request_id = pq.id AND
                sb.cancellation_occurred_at IS NULL
          ) THEN 'matched'   
          WHEN EXISTS (
            SELECT
              1
            from
              booking_not_mades bnm
            WHERE
              bnm.placement_request_id = pq.id
          ) THEN 'unableToMatch' 
          ELSE 'notMatched'
        END
      ) = :status)
      AND (:crn IS NULL OR EXISTS (SELECT 1 FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crn)))
      AND (
        :crnOrName IS NULL OR 
        (
            (EXISTS (SELECT 1 FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crnOrName)))
            OR
            (EXISTS (SELECT 1 FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.name LIKE UPPER('%' || :crnOrName || '%')))
        )
      )
      AND (:tier IS NULL OR EXISTS (SELECT 1 FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.risk_ratings -> 'tier' -> 'value' ->> 'level' = :tier)) 
      AND (CAST(:arrivalDateFrom AS date) IS NULL OR pq.expected_arrival >= :arrivalDateFrom) 
      AND (CAST(:arrivalDateTo AS date) IS NULL OR pq.expected_arrival <= :arrivalDateTo)
      AND (
        :requestType IS NULL OR 
        (
            (:requestType = 'parole' AND pq.is_parole IS TRUE)
            OR
            (:requestType = 'standardRelease' AND pq.is_parole IS FALSE)
        )
      )
      AND ((CAST(:apAreaId AS pg_catalog.uuid) IS NULL) OR area.id = :apAreaId)
      AND ((CAST(:cruManagementAreaId AS pg_catalog.uuid) IS NULL) OR apa.cas1_cru_management_area_id = :cruManagementAreaId)
  """,
    nativeQuery = true,
  )
  fun allForDashboard(
    status: String? = null,
    crn: String? = null,
    crnOrName: String? = null,
    tier: String? = null,
    arrivalDateFrom: LocalDate? = null,
    arrivalDateTo: LocalDate? = null,
    requestType: String? = null,
    apAreaId: UUID? = null,
    cruManagementAreaId: UUID? = null,
    pageable: Pageable? = null,
  ): Page<PlacementRequestEntity>

  @Query("SELECT p from PlacementRequestEntity p WHERE p.dueAt IS NULL")
  fun findAllWithNullDueAt(pageable: Pageable?): Slice<PlacementRequestEntity>

  @Modifying
  @Query("UPDATE PlacementRequestEntity p SET p.dueAt = :dueAt WHERE p.id = :id")
  fun updateDueAt(id: UUID, dueAt: OffsetDateTime?)

  @Query(
    """
    WITH UNFILTERED AS (
      SELECT
      pq.duration AS requestedPlacementDuration,
      pq.expected_arrival AS requestedPlacementArrivalDate,
      pq.id AS id,
      application.crn AS personCrn,
      apa.risk_ratings -> 'tier' -> 'value' ->> 'level' AS personTier,
      pq.application_id AS applicationId,
      apa.name as personName,
      pq.reallocated_at as reallocatedAt,
      pq.is_withdrawn as isWithdrawn,
      pq.booking_id as bookingId,
      pq.is_parole as isParole,
      area.id as apAreaId,
      pq.created_at as created_at,
      apa.cas1_cru_management_area_id as cruManagementAreaId,
      CASE WHEN (pq.is_parole) THEN 'parole' ELSE 'standardRelease' END AS requestType,      
      (SELECT EXISTS (SELECT 1 FROM cancellations c right join bookings booking on c.booking_id = booking.id WHERE booking.id = pq.booking_id AND c.id IS NULL)) AS hasCancellation,
      (SELECT EXISTS (SELECT 1 FROM cas1_space_bookings sb WHERE sb.placement_request_id = pq.id AND sb.cancellation_occurred_at IS NULL)) AS hasSpaceBooking,   
      (SELECT EXISTS (SELECT 1 FROM booking_not_mades bnm WHERE bnm.placement_request_id = pq.id)) AS hasBookingNotMade,
      application.submitted_at::date AS applicationSubmittedDate,
      premises.name AS bookingPremisesName,
      bookings.arrival_date AS bookingArrivalDate,
      (SELECT EXISTS (SELECT 1 FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crn))) as crnHasApplication,
      (SELECT EXISTS (SELECT 1 FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crnOrName))) as crnOrNameHasApplication,
      (SELECT EXISTS (SELECT 1 FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.name LIKE UPPER('%' || :crnOrName || '%'))) as nameHasApplication
      FROM
      placement_requests pq
      LEFT JOIN approved_premises_applications apa ON apa.id = pq.application_id
      LEFT JOIN ap_areas area ON area.id = apa.ap_area_id
      LEFT JOIN applications application ON application.id = pq.application_id
      LEFT JOIN bookings ON pq.booking_id = bookings.id
      LEFT JOIN premises ON bookings.premises_id = premises.id
    ),
    EXC_STATUS AS (
      SELECT *,
      CASE WHEN hasCancellation THEN 'matched' WHEN hasSpaceBooking THEN 'matched' WHEN hasBookingNotMade THEN 'unableToMatch' ELSE 'notMatched' END AS placementRequestStatus
      FROM UNFILTERED
      WHERE
      reallocatedAt IS NULL 
      AND (:crn IS NULL OR crnHasApplication IS TRUE)
      AND (:crnOrName IS NULL OR (crnOrNameHasApplication IS TRUE OR nameHasApplication))
      AND (:tier IS NULL OR personTier = :tier)
      AND (CAST(:arrivalDateFrom AS DATE) IS NULL OR requestedPlacementArrivalDate >= :arrivalDateFrom) 
      AND (CAST(:arrivalDateTo AS DATE) IS NULL OR requestedPlacementArrivalDate <= :arrivalDateTo)
      AND (:requestType IS NULL OR requestType = :requestType)
      AND (:apAreaId IS NULL OR apAreaId = :apAreaId)
      AND (:cruManagementAreaId IS NULL OR cruManagementAreaId = :cruManagementAreaId)
    )
    SELECT * FROM EXC_STATUS
    WHERE
    (:status IS NULL OR isWithdrawn IS FALSE)
    AND (:status IS NULL OR placementRequestStatus = :status)    
    """,
    nativeQuery = true,
  )
  fun allForCas1Dashboard(
    status: String? = null,
    crn: String? = null,
    crnOrName: String? = null,
    tier: String? = null,
    arrivalDateFrom: LocalDate? = null,
    arrivalDateTo: LocalDate? = null,
    requestType: String? = null,
    apAreaId: UUID? = null,
    cruManagementAreaId: UUID? = null,
    pageable: Pageable? = null,
  ): Page<Cas1PlacementRequestSummary>
}

/**
 * Note - in the future this entity will be renamed to 'Match Request' to align with terminology
 * used on the UI
 */
@Entity
@Table(name = "placement_requests")
data class PlacementRequestEntity(
  @Id
  val id: UUID,
  val expectedArrival: LocalDate,
  val duration: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id")
  val assessment: ApprovedPremisesAssessmentEntity,

  @OneToOne
  @JoinColumn(name = "placement_application_id")
  var placementApplication: PlacementApplicationEntity?,

  val createdAt: OffsetDateTime,

  /**
   * Notes from the assessor for the CRU Manager
   */
  val notes: String?,

  /**
   * If a booking is cancelled it will remain linked to the placement
   * request until a new booking is made against it
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity?,

  @OneToMany(mappedBy = "placementRequest", fetch = FetchType.LAZY)
  var spaceBookings: MutableList<Cas1SpaceBookingEntity>,

  @Deprecated("Placement requests are no longer allocated to users")
  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  var allocatedToUser: UserEntity?,

  @OneToMany(mappedBy = "placementRequest", fetch = FetchType.LAZY)
  var bookingNotMades: MutableList<BookingNotMadeEntity>,

  @Deprecated(
    """
    Placement requests are no longer allocated to users
    
    Note that because placement requests with a value in this field are excluded from
    lists of active placement requests, we need to be careful if/when removing this
    column to ensure such placement requests are still not shown
    """,
  )
  var reallocatedAt: OffsetDateTime?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "placement_requirements_id")
  var placementRequirements: PlacementRequirementsEntity,

  var isParole: Boolean,
  var isWithdrawn: Boolean,

  @Enumerated(value = EnumType.STRING)
  var withdrawalReason: PlacementRequestWithdrawalReason?,

  @Deprecated("Placement requests are no longer allocated to users and don't have a due at concept")
  var dueAt: OffsetDateTime?,

  @Version
  var version: Long = 1,
) {
  fun isInWithdrawableState() = isActive()

  fun hasActiveBooking() =
    (booking != null && booking?.cancellations.isNullOrEmpty()) ||
      (spaceBookings.any { it.isActive() })

  fun expectedDeparture(): LocalDate = expectedArrival.plusDays(duration.toLong())

  fun isReallocated() = reallocatedAt != null

  fun isActive() = !isWithdrawn && !isReallocated()

  /**
   * In the model we don't currently have an entity representing the placement request
   * dates specified when the application was originally created. Instead, this is first
   * realised as an automatically created [PlacementRequestEntity] when the [AssessmentEntity]
   * is approved.
   *
   * Ideally we'd model the request for these dates as a subtype of [PlacementApplicationEntity].
   * Unfortunately, it's non-trivial to amend the data model and workflow implementation
   * to allow us to use [PlacementApplicationEntity] for this purpose.
   *
   * Note that we do populated [PlacementApplicationAutomaticEntity] to support reporting on
   * such requests. This could maybe be used as a starting point to fix the data model.
   *
   * For Withdrawal functionality we have a use-case for the user to be able to withdraw the original
   * application dates without withdrawing the whole application and/or assessment.
   *
   * Without a first class entity to represent these dates, we have instead elected to use
   * the [PlacementRequestEntity] that was created for these dates to represent this yet unrealised entity.
   *
   * Whilst not ideal, this gives us a tangible and addressable entity against which:
   *
   * 1) The user can request withdrawals
   * 2) We can raise domain events
   * 3) We can send emails
   *
   * To achieve the aforementioned facade we present such instances of [PlacementRequestEntity] as a
   * 'Request for Placement' whenever presented to user, whether in a list of withdrawable elements,
   * being described on the domain event timeline, or being mentioned in an email on withdrawal
   *
   * This property is used to identify such instances.
   */
  fun isForApplicationsArrivalDate() = placementApplication == null
}

@Repository
interface LockablePlacementRequestRepository : JpaRepository<LockablePlacementRequestEntity, UUID> {
  @Query("SELECT a FROM LockablePlacementRequestEntity a WHERE a.id = :id")
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  fun acquirePessimisticLock(id: UUID): LockablePlacementRequestEntity?
}

/**
 * Provides a version of the PlacementRequestEntity with no relationships, allowing
 * us to lock the PlacementRequests table only without JPA/Hibernate attempting to
 * lock all eagerly loaded relationships
 */
@Entity
@Table(name = "placement_requests")
@Immutable
class LockablePlacementRequestEntity(
  @Id
  val id: UUID,
)

enum class PlacementRequestWithdrawalReason(val apiValue: WithdrawPlacementRequestReason) {
  DUPLICATE_PLACEMENT_REQUEST(WithdrawPlacementRequestReason.duplicatePlacementRequest),
  ALTERNATIVE_PROVISION_IDENTIFIED(WithdrawPlacementRequestReason.alternativeProvisionIdentified),
  WITHDRAWN_BY_PP(WithdrawPlacementRequestReason.withdrawnByPP),
  CHANGE_IN_CIRCUMSTANCES(WithdrawPlacementRequestReason.changeInCircumstances),
  CHANGE_IN_RELEASE_DECISION(WithdrawPlacementRequestReason.changeInReleaseDecision),
  NO_CAPACITY_DUE_TO_LOST_BED(WithdrawPlacementRequestReason.noCapacityDueToLostBed),
  NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION(WithdrawPlacementRequestReason.noCapacityDueToPlacementPrioritisation),
  NO_CAPACITY(WithdrawPlacementRequestReason.noCapacity),
  ERROR_IN_PLACEMENT_REQUEST(WithdrawPlacementRequestReason.errorInPlacementRequest),
  RELATED_APPLICATION_WITHDRAWN(WithdrawPlacementRequestReason.relatedApplicationWithdrawn),
  RELATED_PLACEMENT_APPLICATION_WITHDRAWN(WithdrawPlacementRequestReason.relatedPlacementApplicationWithdrawn),
  ;

  companion object {
    fun valueOf(apiValue: WithdrawPlacementRequestReason): PlacementRequestWithdrawalReason? =
      PlacementRequestWithdrawalReason.entries.firstOrNull { it.apiValue == apiValue }
  }
}

@SuppressWarnings("TooManyFunctions")
interface Cas1PlacementRequestSummary {
  fun getRequestedPlacementDuration(): Int
  fun getRequestedPlacementArrivalDate(): LocalDate
  fun getId(): UUID
  fun getPersonCrn(): String
  fun getPersonTier(): String
  fun getApplicationId(): UUID
  fun getPlacementRequestStatus(): PlacementRequestStatus
  fun getApplicationSubmittedDate(): LocalDate
  fun getIsParole(): Boolean
  fun getBookingPremisesName(): String
  fun getBookingArrivalDate(): LocalDate
}
