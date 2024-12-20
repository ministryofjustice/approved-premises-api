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
interface CancellationReasonRepository : JpaRepository<CancellationReasonEntity, UUID> {
  companion object Constants {
    val CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID: UUID = UUID.fromString("0e068767-c62e-43b5-866d-f0fb1d02ad83")
    val CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID: UUID = UUID.fromString("0a115fa4-6fd0-4b23-8e31-e6d1769c3985")
    val CAS1_RELATED_APP_WITHDRAWN_ID: UUID = UUID.fromString("bcb90030-b2d3-47d1-b289-a8b8c8898576")
    val CAS1_RELATED_OTHER_ID: UUID = UUID.fromString("1d6f3c6e-3a86-49b4-bfca-2513a078aba3")
  }

  @Query("SELECT c FROM CancellationReasonEntity c WHERE c.serviceScope = :serviceName OR c.serviceScope = '*' ORDER by c.sortOrder ASC, c.name ASC")
  fun findAllByServiceScope(serviceName: String): List<CancellationReasonEntity>
}

@Entity
@Table(name = "cancellation_reasons")
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
data class CancellationReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val sortOrder: Int,
) {
  override fun toString() = "CancellationReasonEntity:$id"
}

fun CancellationReasonEntity.serviceScopeMatches(bookingService: String): Boolean {
  return when (serviceScope) {
    "*" -> true
    bookingService -> true
    else -> return false
  }
}
