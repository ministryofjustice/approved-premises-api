package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.Table
import org.locationtech.jts.geom.Point
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import java.time.LocalDate
import java.util.UUID

@Repository
interface PremisesRepository : JpaRepository<PremisesEntity, UUID> {
  companion object {
    private const val BED_COUNT_QUERY = """
     (
        SELECT CAST(COUNT(b.id) as int) 
        FROM BedEntity b
          JOIN b.room r
        WHERE 
            r.premises = p 
            AND b.endDate IS NULL OR b.endDate > CURRENT_DATE
    )
    """
  }

  @Query("SELECT p as premises, $BED_COUNT_QUERY as bedCount FROM PremisesEntity p")
  fun findAllWithBedCount(): List<PremisesWithBedCount>

  @Query(
    "SELECT p as premises, $BED_COUNT_QUERY as bedCount FROM PremisesEntity p WHERE p.probationRegion.id = :probationRegionId",
  )
  fun findAllByProbationRegion(probationRegionId: UUID): List<PremisesWithBedCount>

  @Query(
    """
        SELECT
          new uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesSummary(
            p.id, 
            p.name, 
            p.addressLine1, 
            p.addressLine2, 
            p.postcode, 
            pdu.name, 
            p.status,
            CAST(COALESCE((SELECT count(beds.id)
             FROM TemporaryAccommodationPremisesEntity tap
             INNER JOIN tap.rooms room 
             INNER JOIN room.beds beds 
             WHERE tap.id = p.id AND (beds.endDate IS NULL OR beds.endDate > CURRENT_DATE)
             GROUP BY tap.id
            ),0) as int),   
            la.name)
        FROM
          TemporaryAccommodationPremisesEntity p
          LEFT JOIN p.rooms r 
          LEFT JOIN r.beds b
          LEFT JOIN p.probationRegion pr
          LEFT JOIN p.probationDeliveryUnit pdu
          LEFT JOIN p.localAuthorityArea la
        WHERE pr.id = :regionId
        GROUP BY p.id, p.name, p.addressLine1, p.addressLine2, p.postcode, pdu.name, p.status, la.name
      """,
  )
  fun findAllTemporaryAccommodationSummary(regionId: UUID): List<TemporaryAccommodationPremisesSummary>

  @Query(
    """
    SELECT 
        new uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesSummary(
            p.id, 
            p.name, 
            p.addressLine1, 
            p.addressLine2, 
            p.postcode, 
            p.status, 
            CAST(COUNT(b) as int),
            p.apCode, 
            region.name,
            apArea.name
        ) 
        FROM ApprovedPremisesEntity p 
        LEFT JOIN p.rooms r 
        LEFT JOIN r.beds b on (b.endDate IS NULL OR b.endDate > CURRENT_DATE) 
        LEFT JOIN p.probationRegion region
        LEFT JOIN region.apArea apArea
        WHERE(cast(:probationRegionId as text) IS NULL OR region.id = :probationRegionId)
        AND(cast(:apAreaId as text) IS NULL OR apArea.id = :apAreaId)
        GROUP BY p.id, p.name, p.addressLine1, p.addressLine2, p.postcode, p.apCode, p.status, region.name, apArea.name
  """,
  )
  fun findAllApprovedPremisesSummary(probationRegionId: UUID?, apAreaId: UUID?): List<ApprovedPremisesSummary>

  @Query("SELECT p as premises, $BED_COUNT_QUERY as bedCount FROM PremisesEntity p WHERE TYPE(p) = :type")
  fun <T : PremisesEntity> findAllByType(type: Class<T>): List<PremisesWithBedCount>

  @Query(
    """
        SELECT 
          p as premises, 
          $BED_COUNT_QUERY as bedCount 
        FROM PremisesEntity p 
        WHERE p.probationRegion.id = :probationRegionId AND TYPE(p) = :type
    """,
  )
  fun <T : PremisesEntity> findAllByProbationRegionAndType(probationRegionId: UUID, type: Class<T>): List<PremisesWithBedCount>

  @Query("SELECT COUNT(p) = 0 FROM PremisesEntity p WHERE name = :name AND TYPE(p) = :type")
  fun <T : PremisesEntity> nameIsUniqueForType(name: String, type: Class<T>): Boolean

  @Query("SELECT p FROM PremisesEntity p WHERE name = :name AND TYPE(p) = :type")
  fun <T : PremisesEntity> findByName(name: String, type: Class<T>): PremisesEntity?

  @Query("SELECT p FROM ApprovedPremisesEntity p WHERE apCode = :apCode")
  fun findByApCode(apCode: String): ApprovedPremisesEntity?

  @Query("SELECT CAST(COUNT(b) as int) FROM PremisesEntity p JOIN p.rooms r JOIN r.beds b on (b.endDate IS NULL OR b.endDate >= CURRENT_DATE) WHERE r.premises = :premises")
  fun getBedCount(premises: PremisesEntity): Int

  @Query(
    """
  SELECT
  cast(booking.id as TEXT) as id,
  booking.arrival_date as arrivalDate,
  booking.departure_date as departureDate,
  booking.crn as crn,
  cast(bed.id as TEXT) as bedId,
  bed.name as bedName,
  bed.code as bedCode,
  case
  	when (SELECT count(id) from non_arrivals where booking_id = booking.id) > 0 then 'notMinusArrived'
    when (
    	(SELECT count(id) from arrivals where booking_id = booking.id) > 0 
      AND
      (SELECT count(id) from departures where booking_id = booking.id) = 0
    ) then 'arrived'
    when (
    	(SELECT count(id) from departures where booking_id = booking.id) > 0
    ) then 'departed'
    when (
    	(SELECT count(id) from cancellations where booking_id = booking.id) > 0
    ) then 'cancelled'
    when (
    	(SELECT count(id) from arrivals where booking_id = booking.id) = 0 
      AND
      (SELECT count(id) from non_arrivals where booking_id = booking.id) = 0
    ) then 'awaitingMinusArrival'
  end status
FROM
  bookings booking
  LEFT JOIN beds bed ON booking.bed_id = bed.id
where
  booking.premises_id = :premisesId
  """,
    nativeQuery = true,
  )
  fun getBookingSummariesForPremisesId(premisesId: UUID): List<BookingSummary>
}

@Repository
interface ApprovedPremisesRepository : JpaRepository<ApprovedPremisesEntity, UUID> {
  @Query(
    """
        SELECT
          new uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesBasicSummary(
              p.id,
              p.name,
              p.apCode,
              apArea.id,
              apArea.name,
              CAST(COUNT(b) as int),
              p.supportsSpaceBookings
              )
        FROM 
          ApprovedPremisesEntity p
          LEFT JOIN p.rooms r 
          LEFT JOIN r.beds b on (b.endDate IS NULL OR b.endDate > CURRENT_DATE) 
          LEFT JOIN p.probationRegion region
          LEFT JOIN region.apArea apArea
        WHERE 
          (:gender IS NULL OR p.gender = :gender)
          AND(cast(:apAreaId as text) IS NULL OR apArea.id = :apAreaId) 
          GROUP BY p.id, p.name, p.apCode, apArea.id, apArea.name 
      """,
  )
  fun findForSummaries(gender: ApprovedPremisesGender?, apAreaId: UUID?): List<ApprovedPremisesBasicSummary>

  fun findByQCode(qcode: String): ApprovedPremisesEntity?
}

@Entity
@Table(name = "premises")
@DiscriminatorColumn(name = "service")
@Inheritance(strategy = InheritanceType.JOINED)
abstract class PremisesEntity(
  @Id
  val id: UUID,
  var name: String,
  var addressLine1: String,
  var addressLine2: String?,
  var town: String?,
  var postcode: String,
  var longitude: Double?,
  var latitude: Double?,
  var notes: String,
  var emailAddress: String?,
  @ManyToOne
  @JoinColumn(name = "probation_region_id")
  var probationRegion: ProbationRegionEntity,
  @ManyToOne
  @JoinColumn(name = "local_authority_area_id")
  var localAuthorityArea: LocalAuthorityAreaEntity?,
  @OneToMany(mappedBy = "premises")
  val bookings: MutableList<BookingEntity>,
  @OneToMany(mappedBy = "premises")
  val lostBeds: MutableList<LostBedsEntity>,
  @OneToMany(mappedBy = "premises")
  val rooms: MutableList<RoomEntity>,
  @ManyToMany
  @JoinTable(
    name = "premises_characteristics",
    joinColumns = [JoinColumn(name = "premises_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  var characteristics: MutableList<CharacteristicEntity>,
  @Enumerated(value = EnumType.STRING)
  var status: PropertyStatus,
)

@SuppressWarnings("LongParameterList")
@Entity
@DiscriminatorValue("approved-premises")
@Table(name = "approved_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class ApprovedPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  addressLine2: String?,
  town: String?,
  postcode: String,
  longitude: Double?,
  latitude: Double?,
  notes: String,
  emailAddress: String?,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  var apCode: String,
  var qCode: String,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
  status: PropertyStatus,
  // TODO: Make not-null once Premises have had point added in all environments
  var point: Point?,
  @Enumerated(value = EnumType.STRING)
  var gender: ApprovedPremisesGender,
  var supportsSpaceBookings: Boolean,
  var managerDetails: String?,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  addressLine2,
  town,
  postcode,
  longitude,
  latitude,
  notes,
  emailAddress,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics,
  status,
)

enum class ApprovedPremisesGender {
  MAN,
  WOMAN,
}

@Entity
@DiscriminatorValue("temporary-accommodation")
@Table(name = "temporary_accommodation_premises")
@PrimaryKeyJoinColumn(name = "premises_id")
class TemporaryAccommodationPremisesEntity(
  id: UUID,
  name: String,
  addressLine1: String,
  addressLine2: String?,
  town: String?,
  postcode: String,
  longitude: Double?,
  latitude: Double?,
  notes: String,
  emailAddress: String?,
  probationRegion: ProbationRegionEntity,
  localAuthorityArea: LocalAuthorityAreaEntity?,
  bookings: MutableList<BookingEntity>,
  lostBeds: MutableList<LostBedsEntity>,
  rooms: MutableList<RoomEntity>,
  characteristics: MutableList<CharacteristicEntity>,
  status: PropertyStatus,
  @ManyToOne
  @JoinColumn(name = "probation_delivery_unit_id")
  var probationDeliveryUnit: ProbationDeliveryUnitEntity?,
  var turnaroundWorkingDayCount: Int,
) : PremisesEntity(
  id,
  name,
  addressLine1,
  addressLine2,
  town,
  postcode,
  longitude,
  latitude,
  notes,
  emailAddress,
  probationRegion,
  localAuthorityArea,
  bookings,
  lostBeds,
  rooms,
  characteristics,
  status,
)

data class ApprovedPremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val postcode: String,
  val status: PropertyStatus,
  val bedCount: Int,
  val apCode: String,
  val regionName: String,
  val apAreaName: String,
)

data class TemporaryAccommodationPremisesSummary(
  val id: UUID,
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val postcode: String,
  val pdu: String,
  val status: PropertyStatus,
  val bedCount: Int,
  val localAuthorityAreaName: String?,
)

data class ApprovedPremisesBasicSummary(
  val id: UUID,
  val name: String,
  val apCode: String,
  val apAreaId: UUID,
  val apAreaName: String,
  val bedCount: Int,
  val supportsSpaceBookings: Boolean,
)

interface BookingSummary {
  fun getID(): UUID
  fun getArrivalDate(): LocalDate
  fun getDepartureDate(): LocalDate
  fun getCrn(): String
  fun getBedId(): UUID?
  fun getBedName(): String?
  fun getBedCode(): String?
  fun getStatus(): BookingStatus
}

interface PremisesWithBedCount {
  fun getPremises(): PremisesEntity
  fun getBedCount(): Int
}
