package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class RequestForPlacementAssessedFactory : Factory<RequestForPlacementAssessed> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var placementApplicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var assessedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var decision: Yielded<RequestForPlacementAssessed.Decision> = { RequestForPlacementAssessed.Decision.accepted }
  private var decisionSummary: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var expectedArrival: Yielded<LocalDate> = { LocalDate.now() }
  private var duration: Yielded<Int> = { randomInt(0, 1000) }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPlacementApplicationId(placementApplicationId: UUID) = apply {
    this.placementApplicationId = { placementApplicationId }
  }

  fun withAssessedBy(staffMember: StaffMember) = apply {
    this.assessedBy = { staffMember }
  }

  fun withDecision(decision: RequestForPlacementAssessed.Decision) = apply {
    this.decision = { decision }
  }

  fun withDecisionSummary(decisionSummary: String?) = apply {
    this.decisionSummary = { decisionSummary }
  }

  fun withExpectedArrival(expectedArrival: LocalDate) = apply {
    this.expectedArrival = { expectedArrival }
  }

  fun withDuration(duration: Int) = apply {
    this.duration = { duration }
  }

  override fun produce() = RequestForPlacementAssessed(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    placementApplicationId = this.placementApplicationId(),
    assessedBy = this.assessedBy(),
    decision = this.decision(),
    decisionSummary = this.decisionSummary(),
    expectedArrival = this.expectedArrival(),
    duration = this.duration(),
  )
}
