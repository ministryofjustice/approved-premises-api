package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NonArrivalReasonRepository : JpaRepository<NonArrivalReasonEntity, UUID> {
  fun findByName(name: String): NonArrivalReasonEntity?
  fun findByLegacyDeliusReasonCode(it: String): NonArrivalReasonEntity?
}

@Entity
@Table(name = "non_arrival_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class NonArrivalReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val legacyDeliusReasonCode: String?,
) {
  override fun toString() = "NonArrivalReasonEntity:$id"
}
