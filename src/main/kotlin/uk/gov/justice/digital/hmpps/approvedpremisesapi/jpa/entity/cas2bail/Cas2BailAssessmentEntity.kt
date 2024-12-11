package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2BailAssessmentRepository : JpaRepository<Cas2BailAssessmentEntity, UUID> {
  fun findFirstByApplicationId(applicationId: UUID): Cas2BailAssessmentEntity?
}

@Entity
@Table(name = "cas_2_bail_assessments")
data class Cas2BailAssessmentEntity(
  @Id
  val id: UUID,

  @OneToOne
  val application: Cas2BailApplicationEntity,

  val createdAt: OffsetDateTime,

  var nacroReferralId: String? = null,

  var assessorName: String? = null,

  @OneToMany(mappedBy = "assessment")
  @OrderBy("createdAt DESC")
  var statusUpdates: MutableList<Cas2BailStatusUpdateEntity>? = null,
) {
  override fun toString() = "Cas2BailAssessmentEntity: $id"
}
