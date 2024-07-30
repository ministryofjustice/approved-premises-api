package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import org.hibernate.annotations.OrderBy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Repository
interface Cas2AssessmentRepository : JpaRepository<Cas2AssessmentEntity, UUID> {
  fun findFirstByApplicationId(applicationId: UUID): Cas2AssessmentEntity?
}

@Entity
@Table(name = "cas_2_assessments")
data class Cas2AssessmentEntity(
  @Id
  val id: UUID,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: Cas2ApplicationEntity,

  val createdAt: OffsetDateTime,

  var nacroReferralId: String? = null,

  var assessorName: String? = null,

  @OneToMany(mappedBy = "assessment")
  @OrderBy(clause = "createdAt DESC")
  var statusUpdates: MutableList<Cas2StatusUpdateEntity>? = null,
) {
  override fun toString() = "Cas2AssessmentEntity: $id"
}
