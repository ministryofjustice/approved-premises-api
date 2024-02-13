package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Repository
interface CancellationReasonRepository : JpaRepository<CancellationReasonEntity, UUID> {
  companion object Constants {
    val CAS1_RELATED_PLACEMENT_APP_WITHDRAWN_ID: UUID = UUID.fromString("0e068767-c62e-43b5-866d-f0fb1d02ad83")
    val CAS1_RELATED_PLACEMENT_REQ_WITHDRAWN_ID: UUID = UUID.fromString("0a115fa4-6fd0-4b23-8e31-e6d1769c3985")
    val CAS1_RELATED_APP_WITHDRAWN_ID: UUID = UUID.fromString("bcb90030-b2d3-47d1-b289-a8b8c8898576")
  }

  @Query("SELECT c FROM CancellationReasonEntity c WHERE c.serviceScope = :serviceName OR c.serviceScope = '*'")
  fun findAllByServiceScope(serviceName: String): List<CancellationReasonEntity>

  fun findByNameAndServiceScope(name: String, serviceScope: String): CancellationReasonEntity?
}

@Entity
@Table(name = "cancellation_reasons")
data class CancellationReasonEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
) {
  override fun toString() = "CancellationReasonEntity:$id"
}
