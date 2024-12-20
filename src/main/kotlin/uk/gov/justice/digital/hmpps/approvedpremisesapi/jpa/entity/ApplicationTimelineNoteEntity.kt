package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface ApplicationTimelineNoteRepository : JpaRepository<ApplicationTimelineNoteEntity, UUID> {

  fun findApplicationTimelineNoteEntitiesByApplicationId(applicationId: UUID): List<ApplicationTimelineNoteEntity>
}

@Entity
@Table(name = "application_timeline_notes")
data class ApplicationTimelineNoteEntity(
  @Id
  val id: UUID,
  val applicationId: UUID,
  @ManyToOne
  @JoinColumn(name = "created_by_user_id")
  val createdBy: UserEntity?,
  val createdAt: OffsetDateTime,
  val body: String,
  @Column(name = "cas1_space_booking_id")
  val cas1SpaceBookingId: UUID?,
)
