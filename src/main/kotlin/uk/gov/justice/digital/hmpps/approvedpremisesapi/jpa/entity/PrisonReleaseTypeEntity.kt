package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PrisonReleaseTypeRepository : JpaRepository<PrisonReleaseTypeEntity, UUID> {
  @Query("SELECT m FROM PrisonReleaseTypeEntity m WHERE m.serviceScope = :serviceName OR m.serviceScope = '*' ORDER BY m.sortOrder")
  fun findAllByServiceScope(serviceName: String): List<PrisonReleaseTypeEntity>
}

@Entity
@Table(name = "prison_release_types")
data class PrisonReleaseTypeEntity(
  @Id
  val id: UUID,
  val name: String,
  val isActive: Boolean,
  val serviceScope: String,
  val sortOrder: Int,
) {
  override fun toString() = "PrisonReleaseTypeEntity:$id"
}
