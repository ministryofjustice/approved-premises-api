package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class AssessmentAppealedFactory : Factory<AssessmentAppealed> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var appealId: Yielded<UUID> = { UUID.randomUUID() }
  private var appealUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var createdAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var createdBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var appealDetail: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }
  private var decision: Yielded<AppealDecision> = { randomOf(AppealDecision.entries) }
  private var decisionDetail: Yielded<String> = { randomStringMultiCaseWithNumbers(20) }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withAppealId(appealId: UUID) = apply {
    this.appealId = { appealId }
  }

  fun withAppealUrl(appealUrl: String) = apply {
    this.appealUrl = { appealUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withCreatedAt(createdAt: Instant) = apply {
    this.createdAt = { createdAt }
  }

  fun withCreatedBy(createdBy: StaffMember) = apply {
    this.createdBy = { createdBy }
  }

  fun withCreatedBy(configuration: StaffMemberFactory.() -> Unit) = apply {
    this.createdBy = { StaffMemberFactory().apply(configuration).produce() }
  }

  fun withAppealDetail(appealDetail: String) = apply {
    this.appealDetail = { appealDetail }
  }

  fun withDecision(decision: AppealDecision) = apply {
    this.decision = { decision }
  }

  fun withDecisionDetail(decisionDetail: String) = apply {
    this.decisionDetail = { decisionDetail }
  }

  override fun produce() = AssessmentAppealed(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    appealId = this.appealId(),
    appealUrl = this.appealUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    createdAt = this.createdAt(),
    createdBy = this.createdBy(),
    appealDetail = this.appealDetail(),
    decision = this.decision(),
    decisionDetail = this.decisionDetail(),
  )
}
