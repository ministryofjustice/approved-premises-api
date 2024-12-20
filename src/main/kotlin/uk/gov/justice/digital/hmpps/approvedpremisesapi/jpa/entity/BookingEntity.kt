package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1.Cas1ApplicationFacade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.BookingSummaryForAvailability
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Objects
import java.util.UUID

@Repository
interface BookingRepository : JpaRepository<BookingEntity, UUID> {
  @Query(
    """
    SELECT
      b.arrival_date as arrivalDate,
      b.departure_date as departureDate,
      (
        SELECT
          count(1)
        from
          arrivals
        where
          booking_id = b.id
      ) > 0 as arrived,
      (
        SELECT
          count(1)
        from
          non_arrivals
        where
          booking_id = b.id
      ) > 0 as isNotArrived,
      (
        SELECT
          count(1)
        from
          cancellations
        where
          booking_id = b.id
      ) > 0 as cancelled
    from
      bookings b
    WHERE b.premises_id = :premisesId AND b.arrival_date <= :endDate AND b.departure_date >= :startDate
  """,
    nativeQuery = true,
  )
  fun findAllByPremisesIdAndOverlappingDate(
    premisesId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<BookingSummaryForAvailability>

  @Query(
    """
    SELECT
        bk.id as bookingId,
        bk.crn as crn,
        bk.arrival_date as arrivalDate,
        bk.departure_date as departureDate,
        bk.premises_id as premisesId,
        r.id as roomId,
        a.id as assessmentId
    FROM bookings bk
             INNER JOIN premises p ON bk.premises_id = p.id
             INNER JOIN beds b ON bk.bed_id = b.id
             INNER JOIN rooms r ON b.room_id = r.id
             LEFT JOIN applications ap ON bk.application_id = ap.id
             LEFT JOIN assessments a ON ap.id = a.application_id
             LEFT JOIN cancellations c ON bk.id = c.booking_id
    WHERE bk.premises_id IN (:premisesIds) AND bk.arrival_date <= :endDate AND bk.departure_date >= :startDate AND c.id IS NULL
    """,
    nativeQuery = true,
  )
  fun findAllNotCancelledByPremisesIdsAndOverlappingDate(
    premisesIds: List<UUID>,
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<OverlapBookingsSearchResult>

  @Query("SELECT b FROM BookingEntity b WHERE b.arrivalDate <= :endDate AND b.departureDate >= :startDate AND b.bed = :bed")
  fun findAllByOverlappingDateForBed(startDate: LocalDate, endDate: LocalDate, bed: BedEntity): List<BookingEntity>

  @Query("SELECT MAX(b.departureDate) FROM BookingEntity b WHERE b.premises.id = :premisesId")
  fun getHighestBookingDate(premisesId: UUID): LocalDate?

  @Modifying
  @Query(
    """
    UPDATE bookings SET 
    noms_number = :nomsNumber
    WHERE UPPER(crn) = UPPER(:crn) AND UPPER(noms_number) = UPPER(:oldNomsNumber)
    """,
    nativeQuery = true,
  )
  fun updateNomsByCrn(crn: String, oldNomsNumber: String, nomsNumber: String): Int

  @Query("SELECT DISTINCT(b.nomsNumber) FROM BookingEntity b WHERE b.nomsNumber IS NOT NULL")
  fun getDistinctNomsNumbers(): List<String>

  @Query("SELECT b FROM BookingEntity b WHERE b.bed.id IN :bedIds")
  fun findByBedIds(bedIds: List<UUID>): List<BookingEntity>

  /*
   * There is a bug in Hibernate that has been around since 2010. It is the reason why we have this awful line
   *
   * AND NOT EXISTS (SELECT na FROM NonArrivalEntity na WHERE na.booking = b )

   * * https://hibernate.atlassian.net/browse/HHH-4795

   * * https://stackoverflow.com/questions/52839973/hql-to-check-for-null-in-onetoone-relation
   * */
  @Query(
    "SELECT b FROM BookingEntity b " +
      "WHERE b.bed.id = :bedId " +
      "AND NOT EXISTS (SELECT na FROM NonArrivalEntity na WHERE na.booking = b ) " +
      "AND b.arrivalDate <= :date " +
      "AND SIZE(b.cancellations) = 0 " +
      "AND (CAST(:thisEntityId as org.hibernate.type.UUIDCharType) IS NULL OR b.id != :thisEntityId)",
  )
  fun findByBedIdAndArrivingBeforeDate(bedId: UUID, date: LocalDate, thisEntityId: UUID?): List<BookingEntity>

  /*
    This query is to find the closest booking to the start date for the current bedspace search
    The ClosestBooking is to get the closest booking to the bedspace search start date that is not cancelled
   */
  @Query(
    """
      WITH ClosestBooking AS
         (SELECT b.bed_id, MAX(b.departure_date) departure_date
          FROM bookings b
          LEFT JOIN cancellations c ON b.id = c.booking_id
          WHERE b.departure_date <= :date
            AND b.bed_id IN :bedIds
            AND c.id IS NULL
          GROUP BY b.bed_id)
          
      SELECT b.*,
      beds.name as bed_name,
      beds.room_id as bed_room_id,
      beds.code as bed_code,
      beds.created_at as bed_created_at,
      beds.end_date as bed_end_date
      FROM bookings b
      LEFT JOIN beds ON  b.bed_id = beds.id  
      INNER JOIN ClosestBooking  cb ON b.bed_id = cb.bed_id AND b.departure_date = cb.departure_date
      """,
    nativeQuery = true,
  )
  fun findClosestBookingBeforeDateForBeds(date: LocalDate, bedIds: List<UUID>): List<BookingEntity>

  fun findAllByCrn(crn: String): List<BookingEntity>

  @Query(
    "SELECT b From BookingEntity b WHERE b.application = :application AND (b.adhoc IS TRUE or b.adhoc IS NULL)",
  )
  fun findAllAdhocOrUnknownByApplication(application: ApplicationEntity): List<BookingEntity>

  fun findAllByApplication(application: ApplicationEntity): List<BookingEntity>

  companion object {
    private const val OFFENDERS_QUERY = """
       WITH offenders AS
         (SELECT distinct on (crn) crn,name
            FROM temporary_accommodation_applications taa
            INNER JOIN applications a ON a.id = taa.id
            ORDER BY crn,a.created_at desc)
    """
  }

  @Query(
    """
      $OFFENDERS_QUERY
      
      SELECT
        b.crn AS personCrn,
        offenders.name AS personName,
        Cast(b.id as varchar) bookingId,
        COALESCE(b.status, 'provisional') as bookingStatus,
        b.arrival_date AS bookingStartDate,
        b.departure_date AS bookingEndDate,
        b.created_at AS bookingCreatedAt,
        Cast(p.id as varchar) premisesId,
        p.name AS premisesName,
        p.address_line1 AS premisesAddressLine1,
        p.address_line2 AS premisesAddressLine2,
        p.town AS premisesTown,
        p.postcode AS premisesPostcode,
        Cast(r.id as varchar) roomId,
        r.name AS roomName,
        Cast(b2.id as varchar) bedId,
        b2.name AS bedName
      FROM bookings b
      LEFT JOIN beds b2 ON b.bed_id = b2.id
      LEFT JOIN rooms r ON b2.room_id = r.id
      LEFT JOIN premises p ON r.premises_id = p.id
      LEFT JOIN offenders on b.crn = offenders.crn
      WHERE b.service = 'temporary-accommodation'
      AND (:status is null or b.status = :status)
      AND (Cast(:probationRegionId as varchar) is null or p.probation_region_id = :probationRegionId)
      AND (:crnOrName is null OR lower(b.crn) = lower(:crnOrName) OR lower(offenders.name) LIKE CONCAT('%', lower(:crnOrName),'%'))
    """,
    countQuery = """
      $OFFENDERS_QUERY
      
      SELECT count(1)
      FROM bookings b
      LEFT JOIN beds b2 ON b.bed_id = b2.id
      LEFT JOIN rooms r ON b2.room_id = r.id
      LEFT JOIN premises p ON r.premises_id = p.id
      LEFT JOIN offenders on b.crn = offenders.crn
      WHERE b.service = 'temporary-accommodation'
      AND (:status is null or b.status = :status)
      AND (Cast(:probationRegionId as varchar) is null or p.probation_region_id = :probationRegionId)
      AND (:crnOrName is null OR lower(b.crn) = lower(:crnOrName) OR lower(offenders.name) LIKE CONCAT('%', lower(:crnOrName),'%'))
    """,
    nativeQuery = true,
  )
  fun findTemporaryAccommodationBookings(
    status: String?,
    probationRegionId: UUID?,
    crnOrName: String?,
    pageable: Pageable?,
  ): Page<BookingSearchResult>

  @Modifying
  @Query("UPDATE BookingEntity b set b.status = :status where b.id = :bookingId")
  fun updateBookingStatus(bookingId: UUID, status: BookingStatus)

  @Query(
    "SELECT * FROM bookings WHERE status IS NULL AND service='temporary-accommodation' ",
    nativeQuery = true,
  )
  fun findAllCas3bookingsWithNullStatus(pageable: Pageable?): Slice<BookingEntity>

  @Query(
    "SELECT b FROM BookingEntity b " +
      "WHERE b.bed.id = :bedId " +
      "AND NOT EXISTS (SELECT na FROM NonArrivalEntity na WHERE na.booking = b ) " +
      "AND b.departureDate >= :date " +
      "AND SIZE(b.cancellations) = 0 ",
  )
  fun findActiveOverlappingBookingByBed(bedId: UUID, date: LocalDate): List<BookingEntity>

  @Query(
    """
      SELECT id from bookings where premises_id = :premisesId 
    """,
    nativeQuery = true,
  )
  fun findAllIdsByPremisesId(premisesId: UUID): List<UUID>
}

@Entity
@Table(name = "bookings")
data class BookingEntity(
  @Id
  val id: UUID,
  var crn: String,
  var arrivalDate: LocalDate,
  var departureDate: LocalDate,
  @Deprecated(message = "This is a legacy CAS1-only field that is no longer captured and will be removed once bookings have been fully migrated to space bookings")
  var keyWorkerStaffCode: String?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var arrivals: MutableList<ArrivalEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var departures: MutableList<DepartureEntity>,
  @OneToOne(mappedBy = "booking", cascade = [ CascadeType.REMOVE ])
  var nonArrival: NonArrivalEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var cancellations: MutableList<CancellationEntity>,
  @OneToOne(mappedBy = "booking")
  var confirmation: ConfirmationEntity?,
  @OneToOne
  @JoinColumn(name = "application_id")
  var application: ApplicationEntity?,
  @OneToOne
  @JoinColumn(name = "offline_application_id")
  var offlineApplication: OfflineApplicationEntity?,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
  var extensions: MutableList<ExtensionEntity>,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY, cascade = [ CascadeType.REMOVE ])
  var dateChanges: MutableList<DateChangeEntity>,
  @ManyToOne
  @JoinColumn(name = "premises_id")
  var premises: PremisesEntity,
  @ManyToOne
  @JoinColumn(name = "bed_id")
  var bed: BedEntity?,
  var service: String,
  var originalArrivalDate: LocalDate,
  var originalDepartureDate: LocalDate,
  val createdAt: OffsetDateTime,
  @OneToMany(mappedBy = "booking", fetch = FetchType.LAZY)
  var turnarounds: MutableList<TurnaroundEntity>,
  var nomsNumber: String?,
  @OneToOne(mappedBy = "booking")
  var placementRequest: PlacementRequestEntity?,
  @Enumerated(value = EnumType.STRING)
  var status: BookingStatus?,
  val adhoc: Boolean? = null,
  @Version
  var version: Long = 1,
) {
  val departure: DepartureEntity?
    get() = departures.maxByOrNull { it.createdAt }

  val cancellation: CancellationEntity?
    get() = cancellations.maxByOrNull { it.createdAt }

  val turnaround: TurnaroundEntity?
    get() = turnarounds.maxByOrNull { it.createdAt }

  val isCancelled: Boolean
    get() = cancellation != null

  val arrival: ArrivalEntity?
    get() = arrivals.maxByOrNull { it.createdAt }

  val cas1ApplicationFacade: Cas1ApplicationFacade
    get() {
      if (offlineApplication == null && application !is ApprovedPremisesApplicationEntity) {
        error("Can only return CAS1 application facade for bookings linked to CAS1 applications")
      }
      return Cas1ApplicationFacade(application as ApprovedPremisesApplicationEntity?, offlineApplication)
    }

  fun isInCancellableStateCas1() = !isCancelled && !hasArrivals()

  fun isActive() = !isCancelled

  fun hasArrivals() = arrivals.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BookingEntity) return false

    if (id != other.id) return false
    if (crn != other.crn) return false
    if (arrivalDate != other.arrivalDate) return false
    if (departureDate != other.departureDate) return false
    if (keyWorkerStaffCode != other.keyWorkerStaffCode) return false
    if (nonArrival != other.nonArrival) return false
    if (confirmation != other.confirmation) return false
    if (originalArrivalDate != other.originalArrivalDate) return false
    if (originalDepartureDate != other.originalDepartureDate) return false
    if (createdAt != other.createdAt) return false

    return true
  }

  override fun hashCode() = Objects.hash(
    crn,
    arrivalDate,
    departureDate,
    keyWorkerStaffCode,
    nonArrival,
    confirmation,
    originalArrivalDate,
    originalDepartureDate,
    createdAt,
  )

  override fun toString() = "BookingEntity:$id"
}

@Suppress("TooManyFunctions")
interface BookingSearchResult {
  fun getPersonName(): String?
  fun getPersonCrn(): String
  fun getBookingStatus(): String
  fun getBookingId(): UUID
  fun getBookingStartDate(): LocalDate
  fun getBookingEndDate(): LocalDate
  fun getBookingCreatedAt(): Instant
  fun getPremisesId(): UUID
  fun getPremisesName(): String
  fun getPremisesAddressLine1(): String
  fun getPremisesAddressLine2(): String?
  fun getPremisesTown(): String?
  fun getPremisesPostcode(): String
  fun getRoomId(): UUID
  fun getRoomName(): String
  fun getBedId(): UUID
  fun getBedName(): String
}

interface OverlapBookingsSearchResult {
  val bookingId: UUID
  val crn: String
  val arrivalDate: LocalDate
  val departureDate: LocalDate
  val premisesId: UUID
  val roomId: UUID
  val assessmentId: UUID
}
