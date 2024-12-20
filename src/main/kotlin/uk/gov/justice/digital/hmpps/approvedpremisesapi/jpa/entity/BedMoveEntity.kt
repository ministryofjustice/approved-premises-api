package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface BedMoveRepository : JpaRepository<BedMoveEntity, UUID>

@Entity
@Table(name = "bed_moves")
data class BedMoveEntity(
  @Id
  val id: UUID,

  @OneToOne
  @JoinColumn(name = "booking_id")
  val booking: BookingEntity,

  @OneToOne
  @JoinColumn(name = "previous_bed_id")
  val previousBed: BedEntity?,

  @OneToOne
  @JoinColumn(name = "new_bed_id")
  val newBed: BedEntity,

  var createdAt: OffsetDateTime,

  val notes: String?,
) {

  override fun toString() = "BedMovesEntity: $id"
}
