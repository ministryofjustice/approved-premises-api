package uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Some CAS1 entities can be linked to either an [ApprovedPremisesApplicationEntity],
 * or the now deprecated [OfflineApplicationEntity]
 *
 * This class provides a way to simplify access to fields from these types when either may be in use
 */
class Cas1ApplicationFacade(
  val application: ApprovedPremisesApplicationEntity?,
  val offlineApplication: OfflineApplicationEntity?,
) {
  val id: UUID
    get() = application?.id ?: offlineApplication!!.id

  val submittedAt: OffsetDateTime
    get() = application?.submittedAt ?: offlineApplication!!.createdAt

  val eventNumber: String?
    get() = application?.eventNumber ?: offlineApplication!!.eventNumber
}
