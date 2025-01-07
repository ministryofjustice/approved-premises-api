package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

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
interface Cas2v2AssessmentRepository : JpaRepository<Cas2v2AssessmentEntity, UUID> {
  fun findFirstByApplicationId(applicationId: UUID): Cas2v2AssessmentEntity?
}

@Entity
@Table(name = "cas_2_v2_assessments")
data class Cas2v2AssessmentEntity(
  @Id
  val id: UUID,

  @OneToOne
  val application: Cas2v2ApplicationEntity,

  val createdAt: OffsetDateTime,

  var nacroReferralId: String? = null,

  var assessorName: String? = null,

  @OneToMany(mappedBy = "assessment")
  @OrderBy("createdAt DESC")
  var statusUpdates: MutableList<Cas2v2StatusUpdateEntity>? = null,
) {
  override fun toString() = "Cas2v2AssessmentEntity: $id"
}
