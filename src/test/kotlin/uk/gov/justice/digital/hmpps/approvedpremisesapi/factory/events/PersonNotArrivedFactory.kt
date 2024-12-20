package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrived
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class PersonNotArrivedFactory : Factory<PersonNotArrived> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var bookingId: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<Premises> = { EventPremisesFactory().produce() }
  private var expectedArrivalOn: Yielded<LocalDate> = { LocalDate.now().minusDays(5) }
  private var recordedBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var notes: Yielded<String?> = { null }
  private var reason: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var legacyReasonCode: Yielded<String> = { randomOf(listOf("A", "B", "C", "D", "1H", "4I")) }

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

  fun withNotes(notes: String?) = apply {
    this.notes = { notes }
  }

  fun withExpectedArrivalOn(expectedArrivalOn: LocalDate) = apply {
    this.expectedArrivalOn = { expectedArrivalOn }
  }

  fun withRecordedBy(recordedBy: StaffMember) = apply {
    this.recordedBy = { recordedBy }
  }

  fun withReason(reason: String) = apply {
    this.reason = { reason }
  }

  fun withLegacyReasonCode(legacyReasonCode: String) = apply {
    this.legacyReasonCode = { legacyReasonCode }
  }

  override fun produce() = PersonNotArrived(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    bookingId = this.bookingId(),
    premises = this.premises(),
    notes = this.notes(),
    expectedArrivalOn = this.expectedArrivalOn(),
    recordedBy = this.recordedBy(),
    reason = this.reason(),
    legacyReasonCode = this.legacyReasonCode(),
  )
}
