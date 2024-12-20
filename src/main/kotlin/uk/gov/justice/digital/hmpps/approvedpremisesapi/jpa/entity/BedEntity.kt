package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface BedRepository : JpaRepository<BedEntity, UUID> {
  fun findByCode(bedCode: String): BedEntity?

  fun findByCodeAndRoomId(bedCode: String, roomId: UUID): BedEntity?

  @Query(nativeQuery = true)
  fun findAllBedsForPremises(premisesId: UUID): List<DomainBedSummary>

  @Query(nativeQuery = true)
  fun getDetailById(id: UUID): DomainBedSummary?

  @Query(
    """
    SELECT b 
    FROM BedEntity b 
    WHERE b.id = :bedId AND 
    b.endDate IS NOT NULL AND b.endDate < :endDate
  """,
  )
  fun findArchivedBedByBedIdAndDate(bedId: UUID, endDate: LocalDate): BedEntity?
}

const val BED_SUMMARY_QUERY =
  """
    select b.id as id,
      cast(b.name as text) as name,
      cast(r.name as text) as roomName,
      r.id as roomId,
      (
        select count(booking.id)
        from bookings booking
          left join cancellations cancellation
            on booking.id = cancellation.booking_id
          left join non_arrivals non_arrival 
            on non_arrival.booking_id = booking.id
        where booking.bed_id = b.id
          and booking.arrival_date <= CURRENT_DATE
          and booking.departure_date >= CURRENT_DATE
          and cancellation IS NULL
          and non_arrival IS NULL
      ) > 0 as bedBooked,
      (
        select count(lost_bed.id)
        from lost_beds lost_bed
          left join lost_bed_cancellations cancellation
            on lost_bed.id = cancellation.lost_bed_id
        where lost_bed.bed_id = b.id
          and lost_bed.start_date <= CURRENT_DATE
          and lost_bed.end_date >= CURRENT_DATE
          and cancellation IS NULL
      ) > 0 as bedOutOfService
      from beds b
           join rooms r on b.room_id = r.id 
  """

@NamedNativeQuery(
  name = "BedEntity.findAllBedsForPremises",
  query =
  """
    $BED_SUMMARY_QUERY
    where r.premises_id = cast(?1 as UUID) and (b.end_date is null or b.end_date > CURRENT_DATE)
  """,
  resultSetMapping = "DomainBedSummaryMapping",
)
@NamedNativeQuery(
  name = "BedEntity.getDetailById",
  query =
  """
    $BED_SUMMARY_QUERY
    where b.id = cast(?1 as UUID)
  """,
  resultSetMapping = "DomainBedSummaryMapping",
)
@SqlResultSetMapping(
  name = "DomainBedSummaryMapping",
  classes = [
    ConstructorResult(
      targetClass = DomainBedSummary::class,
      columns = [
        ColumnResult(name = "id", type = UUID::class),
        ColumnResult(name = "name"),
        ColumnResult(name = "roomId", type = UUID::class),
        ColumnResult(name = "roomName"),
        ColumnResult(name = "bedBooked"),
        ColumnResult(name = "bedOutOfService"),
      ],
    ),
  ],
)
@Entity
@Table(name = "beds")
data class BedEntity(
  @Id
  val id: UUID,
  var name: String,
  val code: String?,
  @ManyToOne
  @JoinColumn(name = "room_id")
  val room: RoomEntity,
  var endDate: LocalDate?,
  @CreationTimestamp
  var createdAt: OffsetDateTime?,
) {
  fun isActive(now: LocalDate): Boolean {
    val endDateConst = endDate
    return endDateConst == null || endDateConst.isAfter(now)
  }
  override fun toString() = "BedEntity: $id"
}

open class DomainBedSummary(
  val id: UUID,
  val name: String,
  val roomId: UUID,
  val roomName: String,
  val bedBooked: Boolean,
  val bedOutOfService: Boolean,
)
