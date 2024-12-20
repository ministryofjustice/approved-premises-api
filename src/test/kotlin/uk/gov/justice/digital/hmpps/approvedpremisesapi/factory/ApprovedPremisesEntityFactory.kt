package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDouble
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
  private var apCode: Yielded<String> = { randomStringUpperCase(10) }
  private var postcode: Yielded<String> = { randomPostCode() }
  private var latitude: Yielded<Double> = { randomDouble(53.50, 54.99) }
  private var longitude: Yielded<Double> = { randomDouble(-1.56, 1.10) }
  private var addressLine1: Yielded<String> = { randomStringUpperCase(10) }
  private var addressLine2: Yielded<String> = { randomStringUpperCase(10) }
  private var town: Yielded<String> = { randomStringUpperCase(10) }
  private var notes: Yielded<String> = { randomStringUpperCase(15) }
  private var emailAddress: Yielded<String?> = { randomStringUpperCase(10) }
  private var service: Yielded<String> = { "CAS1" }
  private var qCode: Yielded<String> = { randomStringUpperCase(4) }
  private var characteristics: Yielded<MutableList<CharacteristicEntity>> = { mutableListOf() }
  private var status: Yielded<PropertyStatus> = { randomOf(PropertyStatus.values().asList()) }
  private var point: Yielded<Point>? = null
  private var gender: Yielded<ApprovedPremisesGender> = { ApprovedPremisesGender.MAN }
  private var rooms: Yielded<MutableList<RoomEntity>> = { mutableListOf() }
  private var supportsSpaceBookings: Yielded<Boolean> = { false }
  private var managerDetails: Yielded<String> = { randomStringUpperCase(10) }

  fun withDefaults() = apply {
    withDefaultProbationRegion()
    withDefaultLocalAuthorityArea()
  }

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

  fun withProbationRegion(probationRegion: ProbationRegionEntity) = apply {
    this.probationRegion = { probationRegion }
  }

  fun withDefaultProbationRegion() = withProbationRegion(
    ProbationRegionEntityFactory()
      .withDefaultApArea()
      .produce(),
  )

  fun withYieldedProbationRegion(probationRegion: Yielded<ProbationRegionEntity>) = apply {
    this.probationRegion = probationRegion
  }

  fun withLocalAuthorityArea(localAuthorityAreaEntity: LocalAuthorityAreaEntity) = apply {
    this.localAuthorityArea = { localAuthorityAreaEntity }
  }

  fun withDefaultLocalAuthorityArea() = withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())

  fun withYieldedLocalAuthorityArea(localAuthorityAreaEntity: Yielded<LocalAuthorityAreaEntity>) = apply {
    this.localAuthorityArea = localAuthorityAreaEntity
  }

  fun withQCode(qCode: String) = apply {
    this.qCode = { qCode }
  }

  fun withCharacteristics(characteristics: MutableList<CharacteristicEntity>) = apply {
    this.characteristics = { characteristics }
  }

  fun withCharacteristicsList(characteristics: List<CharacteristicEntity>) = withCharacteristics(characteristics.toMutableList())

  fun withStatus(status: PropertyStatus) = apply {
    this.status = { status }
  }

  fun withUnitTestControlTestProbationAreaAndLocalAuthority() = apply {
    this.withLocalAuthorityArea(
      LocalAuthorityEntityFactory()
        .withIdentifier("LOCALAUTHORITY")
        .produce(),
    )

    this.withProbationRegion(
      ProbationRegionEntityFactory()
        .withDeliusCode("REGION")
        .withApArea(
          ApAreaEntityFactory()
            .withIdentifier("APAREA")
            .produce(),
        )
        .produce(),
    )
  }

  fun withPoint(point: Point) = apply {
    this.point = { point }
  }

  fun withEmailAddress(emailAddress: String?) = apply {
    this.emailAddress = { emailAddress }
  }

  fun withGender(gender: ApprovedPremisesGender) = apply {
    this.gender = { gender }
  }

  fun withRooms(rooms: MutableList<RoomEntity>) = apply {
    this.rooms = { rooms }
  }

  fun withRooms(vararg rooms: RoomEntity) = apply {
    this.rooms = { rooms.toMutableList() }
  }

  fun withSupportsSpaceBookings(supportsSpaceBookings: Boolean) = apply {
    this.supportsSpaceBookings = { supportsSpaceBookings }
  }

  fun withManagerDetails(managerDetails: String) = apply {
    this.managerDetails = { managerDetails }
  }

  override fun produce(): ApprovedPremisesEntity = ApprovedPremisesEntity(
    id = this.id(),
    name = this.name(),
    apCode = this.apCode(),
    postcode = this.postcode(),
    latitude = this.latitude(),
    longitude = this.longitude(),
    probationRegion = this.probationRegion?.invoke() ?: throw RuntimeException("Must provide a probation region"),
    localAuthorityArea = this.localAuthorityArea?.invoke() ?: throw RuntimeException("Must provide a local authority area"),
    bookings = mutableListOf(),
    lostBeds = mutableListOf(),
    addressLine1 = this.addressLine1(),
    addressLine2 = this.addressLine2(),
    town = this.town(),
    notes = this.notes(),
    emailAddress = this.emailAddress(),
    qCode = this.qCode(),
    rooms = this.rooms(),
    characteristics = this.characteristics(),
    status = this.status(),
    point = this.point?.invoke() ?: GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), 4326)
      .createPoint(Coordinate(this.latitude(), this.longitude())),
    gender = this.gender(),
    supportsSpaceBookings = this.supportsSpaceBookings(),
    managerDetails = this.managerDetails(),
  )
}
