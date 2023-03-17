package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Repository
interface ProbationRegionRepository : JpaRepository<ProbationRegionEntity, UUID> {
  fun findByName(name: String): ProbationRegionEntity?
  fun findByDeliusCode(deliusCode: String): ProbationRegionEntity?
}

@Entity
@Table(name = "probation_regions")
data class ProbationRegionEntity(
  @Id
  val id: UUID,
  val name: String,
  @ManyToOne
  @JoinColumn(name = "ap_area_id")
  val apArea: ApAreaEntity,
  @OneToMany(mappedBy = "probationRegion")
  val premises: MutableList<PremisesEntity>,
  val deliusCode: String,
)
