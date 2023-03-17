package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Type
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface DomainEventRepository : JpaRepository<DomainEventEntity, UUID>

@Entity
@Table(name = "domain_events")
data class DomainEventEntity(
  @Id
  val id: UUID,
  val applicationId: UUID,
  val crn: String,
  @Enumerated(value = EnumType.STRING)
  val type: DomainEventType,
  val occurredAt: OffsetDateTime,
  val createdAt: OffsetDateTime,
  @Type(io.hypersistence.utils.hibernate.type.json.JsonType::class)
  val data: String
)

enum class DomainEventType {
  APPROVED_PREMISES_APPLICATION_SUBMITTED,
  APPROVED_PREMISES_APPLICATION_ASSESSED,
  APPROVED_PREMISES_BOOKING_MADE,
  APPROVED_PREMISES_PERSON_ARRIVED,
  APPROVED_PREMISES_PERSON_NOT_ARRIVED,
  APPROVED_PREMISES_PERSON_DEPARTED
}
