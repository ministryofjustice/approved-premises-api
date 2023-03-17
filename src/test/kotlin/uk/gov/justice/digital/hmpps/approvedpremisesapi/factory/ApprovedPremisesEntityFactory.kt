package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDouble
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomInt
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomOf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomPostCode
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.util.UUID

class ApprovedPremisesEntityFactory : Factory<ApprovedPremisesEntity> {
  private var probationRegion: Yielded<ProbationRegionEntity>? = null
  private var localAuthorityArea: Yielded<LocalAuthorityAreaEntity>? = null
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var name: Yielded<String> = { randomStringMultiCaseWithNumbers(8) }
  private var apCode: Yielded<String> = { randomStringUpperCase(5) }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var latitude: Yielded<Double> = { randomDouble(53.50, 54.99) }
  private var longitude: Yielded<Double> = { randomDouble(-1.56, 1.10) }
  private var totalBeds: Yielded<Int> = { randomInt(1, 100) }
  private var addressLine1: Yielded<String> = { randomStringUpperCase(10) }
  private var addressLine2: Yielded<String> = { randomStringUpperCase(10) }
  private var town: Yielded<String> = { randomStringUpperCase(10) }
  private var notes: Yielded<String> = { randomStringUpperCase(15) }
  private var service: Yielded<String> = { "CAS1" }
  private var qCode: Yielded<String> = { randomStringUpperCase(4) }
  private var status: Yielded<PropertyStatus> = { randomOf(PropertyStatus.values().asList()) }
  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withName(name: String) = apply {
    this.name = { name }
  }

  fun withApCode(apCode: String) = apply {
    this.apCode = { apCode }
  }

  fun withAddressLine1(addressLine1: String) = apply {
    this.addressLine1 = { addressLine1 }
  }

  fun withAddressLine2(addressLine2: String) = apply {
    this.addressLine2 = { addressLine2 }
  }

  fun withTown(town: String) = apply {
    this.town = { town }
  }

  fun withNotes(notes: String) = apply {
    this.notes = { notes }
  }

  fun withService(service: String) = apply {
    this.service = { service }
  }

  fun withPostcode(postcode: String) = apply {
    this.postcode = { postcode }
  }

  fun withLatitude(latitude: Double) = apply {
    this.latitude = { latitude }
  }
  fun withLongitude(longitude: Double) = apply {
    this.longitude = { longitude }
  }

  fun withTotalBeds(totalBeds: Int) = apply {
    this.totalBeds = { totalBeds }
  }

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withLocalAuthorityArea(localAuthorityAreaEntity: LocalAuthorityAreaEntity) = apply {
    this.localAuthorityArea = { localAuthorityAreaEntity }
  }

  fun withYieldedLocalAuthorityArea(localAuthorityAreaEntity: Yielded<LocalAuthorityAreaEntity>) = apply {
    this.localAuthorityArea = localAuthorityAreaEntity
  }

  fun withQCode(qCode: String) = apply {
this.qCode = { qCode };
  }

  fun withStatus(status: PropertyStatus) = apply {
    this.status = { status }
  }

  fun withUnitTestControlTestProbationAreaAndLocalAuthority() = apply {
    this.withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .withIdentifier("LOCALAUTHORITY")
        .produce()
    )

    this.withProbationRegion(
      ProbationRegionEntityFactory()
        .withDeliusCode("REGION")
        .withApArea(
          ApAreaEntityFactory()
            .withIdentifier("APAREA")
            .produce()
        )
        .produce()
    )
  }

  override fun produce(): ApprovedPremisesEntity = ApprovedPremisesEntity(
    id = this.id(),
    name = this.name(),
    apCode = this.apCode(),
    postcode = this.postcode(),
    latitude = this.latitude(),
    longitude = this.longitude(),
    totalBeds = this.totalBeds(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("Must provide a probation region"),
    localAuthorityArea = this.localAuthorityArea?.invoke() ?: throw RuntimeException("Must provide a local authority area"),
    bookings = mutableListOf(),
    lostBeds = mutableListOf(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    town = this.town(),
    notes = this.notes(),
    qCode = this.qCode(),
    rooms = mutableListOf(),
    characteristics = mutableListOf(),
    status = this.status()
  )
}
