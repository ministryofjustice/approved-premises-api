package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawn
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.WithdrawnBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class ApplicationWithdrawnFactory : Factory<ApplicationWithdrawn> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var submittedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var withdrawnBy: Yielded<WithdrawnBy> = { WithdrawnByFactory().produce() }
  private var withdrawnAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }
  private var withdrawalReason: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var otherWithdrawalReason: Yielded<String?> = { randomStringMultiCaseWithNumbers(6) }

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

  fun withSubmittedAt(submittedAt: Instant) = apply {
    this.submittedAt = { submittedAt }
  }

  fun withWithdrawnAt(withdrawnAt: Instant) = apply {
    this.withdrawnAt = { withdrawnAt }
  }

  fun withWithdrawalReason(withdrawalReason: String) = apply {
    this.withdrawalReason = { withdrawalReason }
  }

  fun withOtherWithdrawalReason(otherWithdrawalReason: String?) = apply {
    this.otherWithdrawalReason = { otherWithdrawalReason }
  }

  override fun produce() = ApplicationWithdrawn(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    withdrawnBy = this.withdrawnBy(),
    withdrawnAt = this.withdrawnAt(),
    withdrawalReason = this.withdrawalReason(),
    otherWithdrawalReason = this.otherWithdrawalReason(),
  )
}
