package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class PersonArrivedFactory : Factory<PersonArrived> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var applicationSubmittedOn: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var keyWorker: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var arrivedAt: Yielded<OffsetDateTime> = { OffsetDateTime.now().randomDateTimeBefore(5) }
  private var expectedDepartureOn: Yielded<LocalDate> = { LocalDate.now().minusDays(5) }
  private var notes: Yielded<String?> = { null }

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

  fun withBookingId(bookingId: UUID) = apply {
    this.bookingId = { bookingId }
  }

  fun withPremises(premises: Premises) = apply {
    this.premises = { premises }
  }

  fun withApplicationSubmittedOn(applicationSubmittedOn: OffsetDateTime) = apply {
    this.applicationSubmittedOn = { applicationSubmittedOn }
  }

  fun withKeyWorker(keyWorker: StaffMember) = apply {
    this.keyWorker = { keyWorker }
  }

  fun withArrivedAt(arrivedAt: OffsetDateTime) = apply {
    this.arrivedAt = { arrivedAt }
  }

  fun withExpectedDepartureOn(expectedDepartureOn: LocalDate) = apply {
    this.expectedDepartureOn = { expectedDepartureOn }
  }

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  override fun produce() = PersonArrived(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    premises = this.premises(),
    applicationSubmittedOn = this.applicationSubmittedOn(),
    keyWorker = this.keyWorker(),
    arrivedAt = this.arrivedAt(),
    expectedDepartureOn = this.expectedDepartureOn(),
    notes = this.notes()
  )
}
