package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.WithdrawPlacementRequestReason
import java.util.UUID

@Repository
interface DepartureReasonRepository : JpaRepository<DepartureReasonEntity, UUID> {

  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.serviceScope = :serviceName OR d.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<DepartureReasonEntity>

  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.serviceScope = :serviceName OR d.serviceScope = '*' AND d.isActive = true")
  fun findActiveByServiceScope(serviceName: String): List<DepartureReasonEntity>

  @Query("SELECT d FROM DepartureReasonEntity d WHERE d.isActive = true")
  fun findActive(): List<DepartureReasonEntity>
}

@Entity
@Table(name = "departure_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class DepartureReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val legacyDeliusReasonCode: String?,
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_reason_id")
  val parentReasonId: DepartureReasonEntity?,
) {
  override fun toString() = "DepartureReasonEntity:$id"

  companion object {
    fun valueOf(apiValue: WithdrawPlacementRequestReason): PlacementRequestWithdrawalReason? =
      PlacementRequestWithdrawalReason.entries.firstOrNull { it.apiValue == apiValue }
  }
}

fun DepartureReasonEntity.serviceScopeMatches(bookingService: String): Boolean {
  return when (serviceScope) {
    "*" -> true
    bookingService -> true
    else -> return false
  }
}
