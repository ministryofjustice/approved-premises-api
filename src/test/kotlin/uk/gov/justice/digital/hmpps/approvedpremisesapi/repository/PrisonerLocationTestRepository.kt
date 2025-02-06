package uk.gov.justice.digital.hmpps.approvedpremisesapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PrisonerLocationEntity
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface PrisonerLocationTestRepository : JpaRepository<PrisonerLocationEntity, UUID> {
  fun updateEndDateOfLatest(nomsNumber: String, endDate: OffsetDateTime)
}
