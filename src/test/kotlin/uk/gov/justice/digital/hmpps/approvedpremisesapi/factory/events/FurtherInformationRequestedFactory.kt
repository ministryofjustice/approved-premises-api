package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequested
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class FurtherInformationRequestedFactory : Factory<FurtherInformationRequested> {
  private var assessmentId: Yielded<UUID> = { UUID.randomUUID() }
  private var assessmentUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var requestedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var requester: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var recipient: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var requestId: Yielded<UUID> = { UUID.randomUUID() }

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

  fun withRequestedAt(requestedAt: Instant) = apply {
    this.requestedAt = { requestedAt }
  }

  fun withRequester(requester: StaffMember) = apply {
    this.requester = { requester }
  }

  fun withRecipient(recipient: StaffMember) = apply {
    this.recipient = { recipient }
  }

  fun withRequestId(requestId: UUID) = apply {
    this.requestId = { requestId }
  }

  override fun produce() = FurtherInformationRequested(
    assessmentId = this.assessmentId(),
    assessmentUrl = this.assessmentUrl(),
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    requestedAt = this.requestedAt(),
    requester = this.requester(),
    recipient = this.recipient(),
    requestId = this.requestId(),
  )
}
