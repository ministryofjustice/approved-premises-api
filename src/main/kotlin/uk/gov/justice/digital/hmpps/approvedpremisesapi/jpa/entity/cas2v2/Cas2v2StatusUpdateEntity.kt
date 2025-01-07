package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2v2StatusUpdateRepository : JpaRepository<Cas2v2StatusUpdateEntity, UUID> {
  fun findFirstByApplicationIdOrderByCreatedAtDesc(applicationId: UUID): Cas2v2StatusUpdateEntity?

  @Query(
    "SELECT su FROM Cas2v2StatusUpdateEntity su WHERE su.assessment IS NULL",
  )
  fun findAllStatusUpdatesWithoutAssessment(pageable: Pageable?): Slice<Cas2v2StatusUpdateEntity>
}

@Entity
@Table(name = "cas_2_v2_status_updates")
data class Cas2v2StatusUpdateEntity(

  @Id
  val id: UUID,

  val statusId: UUID,
  val description: String,
  val label: String,

  @ManyToOne
  @JoinColumn(name = "assessor_id")
  val assessor: ExternalUserEntity,

  @ManyToOne
  @JoinColumn(name = "application_id")
  val application: Cas2v2ApplicationEntity,

  @ManyToOne
  @JoinColumn(name = "assessment_id")
  var assessment: Cas2v2AssessmentEntity? = null,

  @OneToMany(mappedBy = "statusUpdate")
  val statusUpdateDetails: List<Cas2v2StatusUpdateDetailEntity>? = null,

  @CreationTimestamp
  var createdAt: OffsetDateTime,
) {
  override fun toString() = "Cas2v2StatusEntity: $id"

  fun status(): Cas2PersistedApplicationStatus {
    return Cas2PersistedApplicationStatusFinder().getById(statusId)
  }
}
