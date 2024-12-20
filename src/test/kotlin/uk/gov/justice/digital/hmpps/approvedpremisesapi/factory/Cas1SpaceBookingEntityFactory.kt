package uk.gov.justice.digital.hmpps.approvedpremisesapi.factory

import io.github.bluegroundltd.kfactory.Factory
import io.github.bluegroundltd.kfactory.Yielded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ManagementInfoSource
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringUpperCase
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

class Cas1SpaceBookingEntityFactory : Factory<Cas1SpaceBookingEntity> {
  private var id: Yielded<UUID> = { UUID.randomUUID() }
  private var premises: Yielded<ApprovedPremisesEntity> = { ApprovedPremisesEntityFactory().withDefaults().produce() }
  private var placementRequest: Yielded<PlacementRequestEntity?> = { PlacementRequestEntityFactory().withDefaults().produce() }
  private var application: Yielded<ApprovedPremisesApplicationEntity?> = { ApprovedPremisesApplicationEntityFactory().withDefaults().produce() }
  private var offlineApplication: Yielded<OfflineApplicationEntity?> = { null }
  private var createdBy: Yielded<UserEntity?> = { UserEntityFactory().withDefaults().produce() }
  private var createdAt: Yielded<OffsetDateTime> = { OffsetDateTime.now() }
  private var expectedArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var expectedDepartureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var actualArrivalDate: Yielded<LocalDate?> = { null }
  private var actualArrivalTime: Yielded<LocalTime?> = { null }
  private var actualDepartureDate: Yielded<LocalDate?> = { null }
  private var actualDepartureTime: Yielded<LocalTime?> = { null }
  private var canonicalArrivalDate: Yielded<LocalDate> = { LocalDate.now() }
  private var canonicalDepartureDate: Yielded<LocalDate> = { LocalDate.now() }
  private var crn: Yielded<String> = { randomStringUpperCase(6) }
  private var keyWorkerStaffCode: Yielded<String?> = { null }
  private var keyWorkerName: Yielded<String?> = { null }
  private var keyWorkerAssignedAt: Yielded<Instant?> = { null }
  private var cancellationOccurredAt: Yielded<LocalDate?> = { null }
  private var cancellationRecordedAt: Yielded<Instant?> = { null }
  private var cancellationReason: Yielded<CancellationReasonEntity?> = { null }
  private var cancellationReasonNotes: Yielded<String?> = { null }
  private var departureReason: Yielded<DepartureReasonEntity?> = { null }
  private var departureMoveOnCategory: Yielded<MoveOnCategoryEntity?> = { null }
  private var departureNotes: Yielded<String?> = { null }
  private var criteria: Yielded<MutableList<CharacteristicEntity>> = { emptyList<CharacteristicEntity>().toMutableList() }
  private var nonArrivalConfirmedAt: Yielded<Instant?> = { null }
  private var nonArrivalReason: Yielded<NonArrivalReasonEntity?> = { null }
  private var nonArrivalNotes: Yielded<String?> = { null }
  private var migratedManagementInfoFrom: Yielded<ManagementInfoSource?> = { null }
  private var deliusEventNumber: Yielded<String?> = { null }

  fun withId(id: UUID) = apply {
    this.id = { id }
  }

  fun withPremises(premises: ApprovedPremisesEntity) = apply {
    this.premises = { premises }
  }

  fun withPremises(configuration: ApprovedPremisesEntityFactory.() -> Unit) = apply {
    this.premises = { ApprovedPremisesEntityFactory().apply(configuration).produce() }
  }

  fun withYieldedPremises(premises: Yielded<ApprovedPremisesEntity>) = apply {
    this.premises = premises
  }

  fun withPlacementRequest(placementRequest: PlacementRequestEntity?) = apply {
    this.placementRequest = { placementRequest }
  }

  fun withYieldedPlacementRequest(placementRequest: Yielded<PlacementRequestEntity>) = apply {
    this.placementRequest = placementRequest
  }

  fun withCreatedBy(createdBy: UserEntity?) = apply {
    this.createdBy = { createdBy }
  }

  fun withYieldedCreatedBy(createdBy: Yielded<UserEntity>) = apply {
    this.createdBy = createdBy
  }

  fun withCreatedAt(createdAt: OffsetDateTime) = apply {
    this.createdAt = { createdAt }
  }

  fun withExpectedArrivalDate(expectedArrivalDate: LocalDate) = apply {
    this.expectedArrivalDate = { expectedArrivalDate }
  }

  fun withExpectedDepartureDate(expectedDepartureDate: LocalDate) = apply {
    this.expectedDepartureDate = { expectedDepartureDate }
  }

  fun withCanonicalArrivalDate(canonicalArrivalDate: LocalDate) = apply {
    this.canonicalArrivalDate = { canonicalArrivalDate }
  }

  fun withCanonicalDepartureDate(canonicalDepartureDate: LocalDate) = apply {
    this.canonicalDepartureDate = { canonicalDepartureDate }
  }

  fun withActualArrivalDate(actualArrivalDate: LocalDate?) = apply {
    this.actualArrivalDate = { actualArrivalDate }
  }

  fun withActualArrivalTime(actualArrivalTime: LocalTime?) = apply {
    this.actualArrivalTime = { actualArrivalTime }
  }

  fun withActualDepartureDate(actualDepartureDate: LocalDate?) = apply {
    this.actualDepartureDate = { actualDepartureDate }
  }

  fun withActualDepartureTime(actualDepartureTime: LocalTime?) = apply {
    this.actualDepartureTime = { actualDepartureTime }
  }

  fun withApplication(application: ApprovedPremisesApplicationEntity?) = apply {
    this.application = { application }
  }

  fun withOfflineApplication(offlineApplication: OfflineApplicationEntity?) = apply {
    this.offlineApplication = { offlineApplication }
  }

  fun withCrn(crn: String) = apply {
    this.crn = { crn }
  }

  fun withKeyworkerName(keyWorkerName: String?) = apply {
    this.keyWorkerName = { keyWorkerName }
  }

  fun withKeyworkerStaffCode(keyWorkerStaffCode: String?) = apply {
    this.keyWorkerStaffCode = { keyWorkerStaffCode }
  }

  fun withKeyworkerAssignedAt(keyWorkerAssignedAt: Instant?) = apply {
    this.keyWorkerAssignedAt = { keyWorkerAssignedAt }
  }

  fun withCancellationOccurredAt(occurredAt: LocalDate?) = apply {
    this.cancellationOccurredAt = { occurredAt }
  }

  fun withCancellationRecordedAt(recordedAt: Instant?) = apply {
    this.cancellationRecordedAt = { recordedAt }
  }

  fun withCancellationReason(reason: CancellationReasonEntity?) = apply {
    this.cancellationReason = { reason }
  }

  fun withCancellationReasonNotes(reasonNotes: String?) = apply {
    this.cancellationReasonNotes = { reasonNotes }
  }

  fun withDepartureReason(departureReason: DepartureReasonEntity?) = apply {
    this.departureReason = { departureReason }
  }

  fun withMoveOnCategory(moveOnCategory: MoveOnCategoryEntity) = apply {
    this.departureMoveOnCategory = { moveOnCategory }
  }

  fun withCriteria(criteria: MutableList<CharacteristicEntity>) = apply {
    this.criteria = { criteria }
  }

  fun withNonArrivalConfirmedAt(nonArrivalConfirmedAt: Instant?) = apply {
    this.nonArrivalConfirmedAt = { nonArrivalConfirmedAt }
  }

  fun withNonArrivalNotes(nonArrivalNotes: String?) = apply {
    this.nonArrivalNotes = { nonArrivalNotes }
  }

  fun withNonArrivalReason(reason: NonArrivalReasonEntity?) = apply {
    this.nonArrivalReason = { reason }
  }

  fun withDeliusEventNumber(deliusEventNumber: String?) = apply {
    this.deliusEventNumber = { deliusEventNumber }
  }

  fun withCriteria(vararg criteria: CharacteristicEntity) = apply {
    this.criteria = { criteria.toMutableList() }
  }

  fun withDepartureNotes(departureNotes: String?) = apply {
    this.departureNotes = { departureNotes }
  }

  override fun produce() = Cas1SpaceBookingEntity(
    id = this.id(),
    premises = this.premises(),
    placementRequest = this.placementRequest(),
    createdBy = this.createdBy(),
    createdAt = this.createdAt(),
    expectedArrivalDate = this.expectedArrivalDate(),
    expectedDepartureDate = this.expectedDepartureDate(),
    actualArrivalDate = this.actualArrivalDate(),
    actualArrivalTime = this.actualArrivalTime(),
    actualDepartureDate = this.actualDepartureDate(),
    actualDepartureTime = this.actualDepartureTime(),
    canonicalArrivalDate = this.canonicalArrivalDate(),
    canonicalDepartureDate = this.canonicalDepartureDate(),
    crn = this.crn(),
    keyWorkerStaffCode = this.keyWorkerStaffCode(),
    keyWorkerName = this.keyWorkerName(),
    keyWorkerAssignedAt = this.keyWorkerAssignedAt(),
    application = this.application(),
    offlineApplication = this.offlineApplication(),
    cancellationOccurredAt = cancellationOccurredAt(),
    cancellationRecordedAt = cancellationRecordedAt(),
    cancellationReason = cancellationReason(),
    cancellationReasonNotes = cancellationReasonNotes(),
    departureReason = this.departureReason(),
    departureMoveOnCategory = this.departureMoveOnCategory(),
    departureNotes = this.departureNotes(),
    criteria = this.criteria(),
    nonArrivalConfirmedAt = this.nonArrivalConfirmedAt(),
    nonArrivalNotes = this.nonArrivalNotes(),
    nonArrivalReason = this.nonArrivalReason(),
    deliusEventNumber = this.deliusEventNumber(),
    migratedManagementInfoFrom = this.migratedManagementInfoFrom(),
  )
}
