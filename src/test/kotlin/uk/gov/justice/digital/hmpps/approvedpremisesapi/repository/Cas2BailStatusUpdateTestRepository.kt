package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailStatusUpdateEntity
import java.util.UUID

@Repository
interface Cas2BailStatusUpdateTestRepository : JpaRepository<Cas2BailStatusUpdateEntity, UUID>
