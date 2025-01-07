package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface Cas2v2StatusUpdateDetailRepository : JpaRepository<Cas2v2StatusUpdateDetailEntity, UUID> {
  fun findFirstByStatusUpdateIdOrderByCreatedAtDesc(statusUpdateId: UUID): Cas2v2StatusUpdateDetailEntity?
}

@Entity
@Table(name = "cas_2_v2_status_update_details")
data class Cas2v2StatusUpdateDetailEntity(
    @Id
  val id: UUID,
    val statusDetailId: UUID,

    val label: String,

    @ManyToOne()
  @JoinColumn(name = "status_update_id")
  val statusUpdate: Cas2v2StatusUpdateEntity,

    @CreationTimestamp
  var createdAt: OffsetDateTime? = null,
) {
  override fun toString() = "Cas2v2StatusDetailEntity: $id"

  fun statusDetail(statusId: UUID, detailId: UUID): Cas2PersistedApplicationStatusDetail {
    return Cas2PersistedApplicationStatusFinder().getById(statusId).statusDetails?.find { detail -> detail.id == detailId }
      ?: error("Status detail with id $detailId not found")
  }
}
