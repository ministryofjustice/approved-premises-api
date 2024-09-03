package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Gender
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface PlacementRequirementsRepository : JpaRepository<PlacementRequirementsEntity, UUID> {
  fun findTopByApplicationOrderByCreatedAtDesc(application: ApplicationEntity): PlacementRequirementsEntity?
}

@Entity
@Table(name = "placement_requirements")
data class PlacementRequirementsEntity(
  @Id
  val id: UUID,
  val gender: Gender,
  val apType: ApType,

  @ManyToOne
  @JoinColumn(name = "postcode_district_id")
  val postcodeDistrict: PostCodeDistrictEntity,

  val radius: Int,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "application_id")
  val application: ApprovedPremisesApplicationEntity,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assessment_id")
  val assessment: ApprovedPremisesAssessmentEntity,

  @ManyToMany
  @JoinTable(
    name = "placement_requirements_essential_criteria",
    joinColumns = [JoinColumn(name = "placement_requirement_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val essentialCriteria: List<CharacteristicEntity>,

  @ManyToMany
  @JoinTable(
    name = "placement_requirements_desirable_criteria",
    joinColumns = [JoinColumn(name = "placement_requirement_id")],
    inverseJoinColumns = [JoinColumn(name = "characteristic_id")],
  )
  val desirableCriteria: List<CharacteristicEntity>,

  val createdAt: OffsetDateTime,
)
