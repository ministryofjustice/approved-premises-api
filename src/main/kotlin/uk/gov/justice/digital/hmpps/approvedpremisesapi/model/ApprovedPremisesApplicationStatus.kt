package uk.gov.justice.digital.hmpps.approvedpremisesapi.model

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus as ApiApprovedPremisesApplicationStatus

enum class ApprovedPremisesApplicationStatus(val apiValue: ApiApprovedPremisesApplicationStatus) {
  STARTED(ApiApprovedPremisesApplicationStatus.started),
  SUBMITTED(ApiApprovedPremisesApplicationStatus.submitted),
  REJECTED(ApiApprovedPremisesApplicationStatus.rejected),
  AWAITING_ASSESSMENT(ApiApprovedPremisesApplicationStatus.awaitingAssesment),
  UNALLOCATED_ASSESSMENT(ApiApprovedPremisesApplicationStatus.unallocatedAssesment),
  ASSESSMENT_IN_PROGRESS(ApiApprovedPremisesApplicationStatus.assesmentInProgress),

  /**
   * An application has been assessed and a [PlacementRequestEntity] has been created which requires a [BookingEntity].
   *
   * Note - If a [PlacementApplicationEntity] is assessed the application _will not_ enter this state
   * (it will remain as PENDING_PLACEMENT_REQUEST)
   */
  AWAITING_PLACEMENT(ApiApprovedPremisesApplicationStatus.awaitingPlacement),

  /**
   * A [BookingEntity] has been created for a [PlacementRequestEntity]
   */
  PLACEMENT_ALLOCATED(ApiApprovedPremisesApplicationStatus.placementAllocated),
  INAPPLICABLE(ApiApprovedPremisesApplicationStatus.inapplicable),
  WITHDRAWN(ApiApprovedPremisesApplicationStatus.withdrawn),
  REQUESTED_FURTHER_INFORMATION(ApiApprovedPremisesApplicationStatus.requestedFurtherInformation),

  /**
   * An application has been assessed. Because no arrival date was defined,
   * one or more [PlacementApplicationEntity]s are required
   */
  PENDING_PLACEMENT_REQUEST(ApiApprovedPremisesApplicationStatus.pendingPlacementRequest),

  EXPIRED(ApiApprovedPremisesApplicationStatus.expired),
  ;

  companion object {
    fun valueOf(apiValue: ApiApprovedPremisesApplicationStatus): ApprovedPremisesApplicationStatus =
      ApprovedPremisesApplicationStatus.entries.first { it.apiValue == apiValue }
  }

  fun toCas1Status(): Cas1ApplicationStatus {
    return when (this) {
      EXPIRED -> Cas1ApplicationStatus.expired
      STARTED -> Cas1ApplicationStatus.started
      SUBMITTED -> Cas1ApplicationStatus.submitted
      REJECTED -> Cas1ApplicationStatus.rejected
      AWAITING_ASSESSMENT -> Cas1ApplicationStatus.awaitingAssesment
      UNALLOCATED_ASSESSMENT -> Cas1ApplicationStatus.unallocatedAssesment
      ASSESSMENT_IN_PROGRESS -> Cas1ApplicationStatus.assesmentInProgress
      AWAITING_PLACEMENT -> Cas1ApplicationStatus.awaitingPlacement
      PLACEMENT_ALLOCATED -> Cas1ApplicationStatus.placementAllocated
      INAPPLICABLE -> Cas1ApplicationStatus.inapplicable
      WITHDRAWN -> Cas1ApplicationStatus.withdrawn
      REQUESTED_FURTHER_INFORMATION -> Cas1ApplicationStatus.requestedFurtherInformation
      PENDING_PLACEMENT_REQUEST -> Cas1ApplicationStatus.pendingPlacementRequest
    }
  }
}
