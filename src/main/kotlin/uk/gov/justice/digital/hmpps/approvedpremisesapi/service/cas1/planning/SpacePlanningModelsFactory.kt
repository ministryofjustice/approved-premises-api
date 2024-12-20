package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningModelsFactory.Constants.DEFAULT_CHARACTERISTIC_WEIGHT
import java.time.LocalDate

@Service
class SpacePlanningModelsFactory {
  object Constants {
    const val DEFAULT_CHARACTERISTIC_WEIGHT = 100
  }

  fun allBeds(
    premises: ApprovedPremisesEntity,
  ) = premises.rooms.flatMap { it.beds }.map { it.toBed() }

  fun allBedsDayState(
    day: LocalDate,
    premises: ApprovedPremisesEntity,
    outOfServiceBedRecordsToConsider: List<Cas1OutOfServiceBedEntity>,
  ) = premises.rooms.flatMap { it.beds }
    .map { bedEntity ->
      BedDayState(
        bed = bedEntity.toBed(),
        day = day,
        inactiveReason = bedEntity.getInactiveReason(day, outOfServiceBedRecordsToConsider),
      )
    }

  fun spaceBookingsForDay(
    day: LocalDate,
    spaceBookingsToConsider: List<Cas1SpaceBookingEntity>,
  ): List<SpaceBooking> =
    spaceBookingsToConsider
      .filter { it.isResident(day) }
      .map { booking ->
        SpaceBooking(
          id = booking.id,
          label = booking.crn,
          requiredRoomCharacteristics = toRoomCharacteristics(booking.criteria),
        )
      }

  private fun BedEntity.getInactiveReason(day: LocalDate, outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>): BedInactiveReason? {
    val outOfServiceRecord = this.findOutOfServiceRecord(day, outOfServiceBedRecords)
    return if (outOfServiceRecord != null) {
      BedOutOfService(outOfServiceRecord.reason.name)
    } else if (!this.isActive(day)) {
      BedEnded(this.endDate!!)
    } else {
      null
    }
  }

  private fun BedEntity.findOutOfServiceRecord(
    day: LocalDate,
    outOfServiceBedRecords: List<Cas1OutOfServiceBedEntity>,
  ) = outOfServiceBedRecords.firstOrNull { it.isApplicable(day, candidate = this) }

  private fun BedEntity.toBed() = Bed(
    id = this.id,
    label = this.name,
    room = Room(
      id = this.room.id,
      label = this.room.name,
      characteristics = toRoomCharacteristics(this.room.characteristics),
    ),
  )

  private fun toRoomCharacteristics(characteristicEntities: List<CharacteristicEntity>) = characteristicEntities
    .asSequence()
    .filter { it.isActive }
    .filter { it.isModelScopeRoom() }
    .filter { Cas1SpaceBookingEntity.Constants.CRITERIA_CHARACTERISTIC_PROPERTY_NAMES_OF_INTEREST.contains(it.propertyName) }
    .map { toCharacteristic(it) }
    .toSet()

  private fun toCharacteristic(characteristicEntity: CharacteristicEntity) = Characteristic(
    label = characteristicEntity.propertyName!!,
    propertyName = characteristicEntity.propertyName!!,
    weighting = DEFAULT_CHARACTERISTIC_WEIGHT,
    singleRoom = characteristicEntity.propertyName == CAS1_PROPERTY_NAME_SINGLE_ROOM,
  )
}
