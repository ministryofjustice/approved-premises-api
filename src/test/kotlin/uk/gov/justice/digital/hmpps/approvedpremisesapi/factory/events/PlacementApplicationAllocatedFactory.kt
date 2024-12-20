package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.DatePeriod
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocated
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateTimeBefore
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.Instant
import java.util.UUID

class PlacementApplicationAllocatedFactory : Factory<PlacementApplicationAllocated> {
  private var applicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var applicationUrl: Yielded<String> = { randomStringMultiCaseWithNumbers(10) }
  private var placementApplicationId: Yielded<UUID> = { UUID.randomUUID() }
  private var personReference: Yielded<PersonReference> = { PersonReferenceFactory().produce() }
  private var allocatedAt: Yielded<Instant> = { Instant.now().randomDateTimeBefore(7) }
  private var placementDates: Yielded<List<DatePeriod>> = { emptyList() }
  private var allocatedTo: Yielded<StaffMember?> = { StaffMemberFactory().produce() }
  private var allocatedBy: Yielded<StaffMember?> = { StaffMemberFactory().produce() }

  fun withApplicationId(applicationId: UUID) = apply {
    this.applicationId = { applicationId }
  }

  fun withApplicationUrl(applicationUrl: String) = apply {
    this.applicationUrl = { applicationUrl }
  }

  fun withPlacementApplicationId(placementApplicationId: UUID) = apply {
    this.placementApplicationId = { placementApplicationId }
  }

  fun withPersonReference(personReference: PersonReference) = apply {
    this.personReference = { personReference }
  }

  fun withPersonReference(configuration: PersonReferenceFactory.() -> Unit) = apply {
    this.personReference = { PersonReferenceFactory().apply(configuration).produce() }
  }

  fun withAllocatedAt(allocatedAt: Instant) = apply {
    this.allocatedAt = { allocatedAt }
  }

  fun withPlacementDates(placementDates: List<DatePeriod>) = apply {
    this.placementDates = { placementDates }
  }

  fun withAllocatedTo(allocatedTo: StaffMember?) = apply {
    this.allocatedTo = { allocatedTo }
  }

  fun withAllocatedTo(configuration: StaffMemberFactory.() -> Unit) = apply {
    this.allocatedTo = { StaffMemberFactory().apply(configuration).produce() }
  }

  fun withAllocatedBy(allocatedBy: StaffMember?) = apply {
    this.allocatedBy = { allocatedBy }
  }

  fun withAllocatedBy(configuration: StaffMemberFactory.() -> Unit) = apply {
    this.allocatedBy = { StaffMemberFactory().apply(configuration).produce() }
  }

  override fun produce() = PlacementApplicationAllocated(
    applicationId = this.applicationId(),
    applicationUrl = this.applicationUrl(),
    placementApplicationId = this.placementApplicationId(),
    personReference = this.personReference(),
    allocatedAt = this.allocatedAt(),
    placementDates = this.placementDates(),
    allocatedTo = this.allocatedTo(),
    allocatedBy = this.allocatedBy(),
  )
}
