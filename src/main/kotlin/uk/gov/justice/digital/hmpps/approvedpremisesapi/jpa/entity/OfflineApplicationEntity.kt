package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

@Repository
interface OfflineApplicationRepository : JpaRepository<OfflineApplicationEntity, UUID> {
  fun findAllByService(name: String): List<OfflineApplicationEntity>
  fun findAllByServiceAndCrn(name: String, crn: String): List<OfflineApplicationEntity>
}

/**
 * Offline applications are only used by CAS1
 *
 * Offline applications were created in the following circumstances:
 *
 * 1. When bulk loading bookings already created in Delius into the approved premises, as part of go-live migrations
 * 2. When a manual (adhoc) booking was required for a CRN that didn't have an existing application in [ApprovedPremisesApplicationEntity]
 *
 * As creation of manual bookings is no longer supported, Offline Applications should no longer be created
 */
@Entity
@Table(name = "offline_applications")
data class OfflineApplicationEntity(
  @Id
  val id: UUID,
  val crn: String,
  val service: String,
  val createdAt: OffsetDateTime,
  val eventNumber: String?,
)
