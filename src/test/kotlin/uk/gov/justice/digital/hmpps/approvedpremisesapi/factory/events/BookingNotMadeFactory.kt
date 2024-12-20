package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeBookedBy
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMade
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class BookingNotMadeFactory : Factory<BookingNotMade> {
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(12) }
  private var attemptedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(5) }
  private var attemptedBy: Yielded<BookingMadeBookedBy> = { BookingMadeBookedByFactory().produce() }
  private var failureDescription: Yielded<String> = { randomStringMultiCaseWithNumbers(7) }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withDeliusEventNumber(deliusEventNumber: String) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withAttemptedAt(attemptedAt: Instant) = apply {
    this.attemptedAt = { attemptedAt }
  }

  fun withAttemptedBy(attemptedBy: BookingMadeBookedBy) = apply {
    this.attemptedBy = { attemptedBy }
  }

  fun withFailureDescription(failureDescription: String) = apply {
    this.failureDescription = { failureDescription }
  }

  override fun produce() = BookingNotMade(
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    attemptedAt = this.attemptedAt(),
    attemptedBy = this.attemptedBy(),
    failureDescription = this.failureDescription(),
  )
}
