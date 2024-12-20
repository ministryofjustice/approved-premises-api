package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class AssessmentAllocatedFactory : Factory<AssessmentAllocated> {
  private var assessmentId: Yielded<UUID> = { UUID.randomUUID() }
  private var assessmentUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var allocatedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var allocatedBy: Yielded<StaffMember?> = { StaffMemberFactory().produce() }
  private var allocatedTo: Yielded<StaffMember?> = { StaffMemberFactory().produce() }

  fun withAssessmentId(assessmentId: UUID) = apply {
    this.assessmentId = { assessmentId }
  }

  fun withAssessmentUrl(assessmentUrl: String) = apply {
    this.assessmentUrl = { assessmentUrl }
  }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withAllocatedAt(allocatedAt: Instant) = apply {
    this.allocatedAt = { allocatedAt }
  }

  fun withAllocatedBy(allocatedBy: StaffMember?) = apply {
    this.allocatedBy = { allocatedBy }
  }

  fun withAllocatedTo(allocatedTo: StaffMember?) = apply {
    this.allocatedTo = { allocatedTo }
  }

  override fun produce() = AssessmentAllocated(
    assessmentId = this.assessmentId(),
    assessmentUrl = this.assessmentUrl(),
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    allocatedAt = this.allocatedAt(),
    allocatedBy = this.allocatedBy(),
    allocatedTo = this.allocatedTo(),
  )
}
