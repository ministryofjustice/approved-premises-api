package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReferralRejectionReasonRepository : JpaRepository<ReferralRejectionReasonEntity, UUID> {
  @Query("SELECT m FROM ReferralRejectionReasonEntity m WHERE m.serviceScope = :serviceName OR m.serviceScope = '*' ORDER BY m.sortOrder")
  fun findAllByServiceScope(serviceName: String): List<ReferralRejectionReasonEntity>

  @Query("SELECT rr FROM ReferralRejectionReasonEntity rr WHERE rr.serviceScope = :serviceName AND rr.name = :name AND rr.isActive = true")
  fun findByNameAndActive(name: String, serviceName: String): ReferralRejectionReasonEntity?
}

@Entity
@Table(name = "referral_rejection_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class ReferralRejectionReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val sortOrder: Int,
) {
  override fun toString() = "ReferralRejectionReasonEntity:$id"
}
