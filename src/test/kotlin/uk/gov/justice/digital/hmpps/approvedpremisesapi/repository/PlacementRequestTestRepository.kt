package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import java.util.UUID

@Repository
interface PlacementRequestTestRepository : JpaRepository<PlacementRequestEntity, UUID> {
  fun findByApplication(application: ApplicationEntity): PlacementRequestEntity?
  fun findAllByApplication(application: ApplicationEntity): List<PlacementRequestEntity>

  fun findAllByIsParoleAndReallocatedAtNullAndIsWithdrawnFalse(
    isParole: Boolean,
    pageable: Pageable?,
  ): Page<PlacementRequestEntity>
}
