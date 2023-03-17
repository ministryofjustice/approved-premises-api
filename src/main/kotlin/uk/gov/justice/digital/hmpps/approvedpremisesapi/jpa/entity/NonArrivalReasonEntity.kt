package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Repository
interface NonArrivalReasonRepository : JpaRepository<NonArrivalReasonEntity, UUID>

@Entity
@Table(name = "non_arrival_reasons")
data class NonArrivalReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean
) {
  override fun toString() = "NonArrivalReasonEntity:$id"
}
