package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import java.util.UUID

@Repository
interface Cas2v2StatusUpdateTestRepository : JpaRepository<Cas2v2StatusUpdateEntity, UUID>
