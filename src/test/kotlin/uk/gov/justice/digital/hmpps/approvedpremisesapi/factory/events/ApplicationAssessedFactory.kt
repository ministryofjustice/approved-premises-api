package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedAssessedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class ApplicationAssessedFactory : Factory<ApplicationAssessed> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var assessedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }
  private var assessedBy: Yielded<ApplicationAssessedAssessedBy> = { ApplicationAssessedAssessedByFactory().produce() }
  private var decision: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var decisionRationale: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }
  private var arrivalDate: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withAssessedAt(assessedAt: Instant) = apply {
    this.assessedAt = { assessedAt }
  }

  fun withAssessedBy(assessedBy: ApplicationAssessedAssessedBy) = apply {
    this.assessedBy = { assessedBy }
  }

  fun withDecision(decision: String) = apply {
    this.decision = { decision }
  }

  fun withDecisionRationale(decisionRationale: String?) = apply {
    this.decisionRationale = { decisionRationale }
  }

  fun withArrivalDate(arrivalDate: Instant) = apply {
    this.arrivalDate = { arrivalDate }
  }

  override fun produce() = ApplicationAssessed(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    assessedAt = this.assessedAt(),
    assessedBy = this.assessedBy(),
    decision = this.decision(),
    decisionRationale = this.decisionRationale(),
    arrivalDate = this.arrivalDate(),
  )
}
