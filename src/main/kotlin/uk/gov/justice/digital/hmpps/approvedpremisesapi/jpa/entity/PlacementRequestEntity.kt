package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Service
@Suppress("FunctionNaming")
@Repository
interface PlacementRequestRepository : JpaRepository<PlacementRequestEntity, UUID> {
  fun findByApplication(application: ApprovedPremisesApplicationEntity): List<PlacementRequestEntity>

  @Query(
    """
      SELECT p FROM PlacementRequestEntity p
      JOIN p.application a
      LEFT OUTER JOIN a.apArea apArea
      WHERE
        p.allocatedToUser.id = :userId AND
        ((cast(:apAreaId as org.hibernate.type.UUIDCharType) IS NULL) OR apArea.id = :apAreaId) AND
        p.reallocatedAt IS NULL AND 
        p.isWithdrawn IS FALSE
    """,
  )
  fun findOpenRequestsAssignedToUser(
    userId: UUID,
    apAreaId: UUID?,
    pageable: Pageable?,
  ): Page<PlacementRequestEntity>

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
      left join applications application on application.id = pq.application_id
      left join approved_premises_applications apa on apa.id = pq.application_id
      left join ap_areas area on area.id = apa.ap_area_id
    where
      pq.reallocated_at IS NULL 
      AND (:status IS NULL OR pq.is_withdrawn IS FALSE)
      AND (:status IS NULL OR (
        CASE
          WHEN (
            SELECT
              COUNT(booking)
            from
              bookings booking
              left join cancellations c on c.booking_id = booking.id
            WHERE
              booking.id = pq.booking_id
              AND c.id IS NULL
          ) > 0 THEN 'matched'
          WHEN (
            SELECT
              COUNT(bnm)
            from
              booking_not_mades bnm
            WHERE
              bnm.placement_request_id = pq.id
          ) > 0 THEN 'unableToMatch'
          ELSE 'notMatched'
        END
      ) = :#{#status?.toString()})
      AND (:crn IS NULL OR (SELECT COUNT(1) FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crn)) = 1)
      AND (
        :crnOrName IS NULL OR 
        (
            ((SELECT COUNT(1) FROM applications a WHERE a.id = pq.application_id AND a.crn = UPPER(:crnOrName)) = 1)
            OR
            ((SELECT COUNT(1) FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.name LIKE UPPER('%' || :crnOrName || '%')) = 1)
        )
      )
      AND (:tier IS NULL OR (SELECT COUNT(1) FROM approved_premises_applications apa WHERE apa.id = pq.application_id AND apa.risk_ratings -> 'tier' -> 'value' ->> 'level' = :tier) = 1) 
      AND (CAST(:arrivalDateFrom AS date) IS NULL OR pq.expected_arrival >= :arrivalDateFrom) 
      AND (CAST(:arrivalDateTo AS date) IS NULL OR pq.expected_arrival <= :arrivalDateTo)
      AND (
        :requestType IS NULL OR 
        (
            (:#{#requestType?.toString()} = 'parole' AND pq.is_parole IS TRUE)
            OR
            (:#{#requestType?.toString()} = 'standardRelease' AND pq.is_parole IS FALSE)
        )
      )
      AND ((CAST(:apAreaId AS pg_catalog.uuid) IS NULL) OR area.id = :apAreaId)
  """,
    nativeQuery = true,
  )
  fun allForDashboard(
    status: PlacementRequestStatus? = null,
    crn: String? = null,
    crnOrName: String? = null,
    tier: String? = null,
    arrivalDateFrom: LocalDate? = null,
    arrivalDateTo: LocalDate? = null,
    requestType: PlacementRequestRequestType? = null,
    apAreaId: UUID? = null,
    pageable: Pageable? = null,
  ): Page<PlacementRequestEntity>
}

@Entity
@Table(name = "placement_requests")
data class PlacementRequestEntity(
  @Id
  val id: UUID,
  val expectedArrival: LocalDate,
  val duration: Int,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  val assessment: AssessmentEntity,

  @OneToOne
  @JoinColumn(name = "placement_application_id")
  val placementApplication: PlacementApplicationEntity?,

  val createdAt: OffsetDateTime,

  val notes: String?,

  @ManyToOne
  @JoinColumn(name = "booking_id")
  var booking: BookingEntity?,

  @ManyToOne
  @JoinColumn(name = "allocated_to_user_id")
  var allocatedToUser: UserEntity?,

  @OneToMany(mappedBy = "placementRequest")
  var bookingNotMades: MutableList<BookingNotMadeEntity>,

  var reallocatedAt: OffsetDateTime?,

  @ManyToOne
  @JoinColumn(name = "placement_requirements_id")
  var placementRequirements: PlacementRequirementsEntity,

  var isParole: Boolean,
  var isWithdrawn: Boolean,

  @Enumerated(value = EnumType.STRING)
  var withdrawalReason: PlacementRequestWithdrawalReason?,
) {
  fun isInWithdrawableState() = isActive()

  fun hasActiveBooking() = booking != null && booking?.cancellations.isNullOrEmpty()

  fun expectedDeparture() = expectedArrival.plusDays(duration.toLong())

  fun isReallocated() = reallocatedAt != null

  fun isActive() = !isWithdrawn && !isReallocated()

  fun isForApplicationsArrivalDate() = placementApplication == null
}

enum class PlacementRequestWithdrawalReason {
  DUPLICATE_PLACEMENT_REQUEST,
  ALTERNATIVE_PROVISION_IDENTIFIED,
  WITHDRAWN_BY_PP,
  CHANGE_IN_CIRCUMSTANCES,
  CHANGE_IN_RELEASE_DECISION,
  NO_CAPACITY_DUE_TO_LOST_BED,
  NO_CAPACITY_DUE_TO_PLACEMENT_PRIORITISATION,
  NO_CAPACITY,
  ERROR_IN_PLACEMENT_REQUEST,
  RELATED_APPLICATION_WITHDRAWN,
  RELATED_PLACEMENT_APPLICATION_WITHDRAWN,
}
