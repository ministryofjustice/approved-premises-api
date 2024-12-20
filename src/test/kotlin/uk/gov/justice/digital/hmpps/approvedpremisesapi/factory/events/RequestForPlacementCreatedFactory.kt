package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RequestForPlacementCreatedFactory : Factory<RequestForPlacementCreated> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var deliusEventNumber: Yielded<String> = { randomStringMultiCaseWithNumbers(6) }
  private var createdAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var createdBy: Yielded<StaffMember> = { StaffMemberFactory().produce() }
  private var expectedArrival: Yielded<LocalDate> = { LocalDate.now() }
  private var duration: Yielded<Int> = { randomInt(0, 1000) }
  private var requestForPlacementType: Yielded<RequestForPlacementType> = { RequestForPlacementType.additionalPlacement }
  private var requestForPlacementId: Yielded<UUID> = { UUID.randomUUID() }

  fun withRequestForPlacementType(requestForPlacementType: RequestForPlacementType) = apply {
    this.requestForPlacementType = { requestForPlacementType }
  }

  fun withExpectedArrival(expectedArrival: LocalDate) = apply {
    this.expectedArrival = { expectedArrival }
  }

  fun withDuration(duration: Int) = apply {
    this.duration = { duration }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  override fun produce() = RequestForPlacementCreated(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    personReference = this.personReference(),
    deliusEventNumber = this.deliusEventNumber(),
    createdAt = this.createdAt(),
    createdBy = this.createdBy(),
    expectedArrival = this.expectedArrival(),
    duration = this.duration(),
    requestForPlacementType = this.requestForPlacementType(),
    requestForPlacementId = this.requestForPlacementId(),
  )
}
