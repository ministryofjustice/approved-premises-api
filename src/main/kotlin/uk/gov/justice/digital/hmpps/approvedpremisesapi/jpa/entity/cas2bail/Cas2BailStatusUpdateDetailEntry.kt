package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail

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
interface Cas2BailStatusUpdateDetailRepository : JpaRepository<Cas2BailStatusUpdateDetailEntity, UUID> {
  fun findFirstByStatusUpdateIdOrderByCreatedAtDesc(statusUpdateId: UUID): Cas2BailStatusUpdateDetailEntity?
}

@Entity
@Table(name = "cas_2_bail_status_update_details")
data class Cas2BailStatusUpdateDetailEntity(
  @Id
  val id: UUID,
  val statusDetailId: UUID,

  val label: String,

  @ManyToOne()
  @JoinColumn(name = "status_update_id")
  val statusUpdate: Cas2BailStatusUpdateEntity,

  @CreationTimestamp
  var createdAt: OffsetDateTime? = null,
) {
  override fun toString() = "Cas2BailStatusDetailEntity: $id"

  fun statusDetail(statusId: UUID, detailId: UUID): Cas2PersistedApplicationStatusDetail {
    return Cas2PersistedApplicationStatusFinder().getById(statusId).statusDetails?.find { detail -> detail.id == detailId }
      ?: error("Status detail with id $detailId not found")
  }
}
