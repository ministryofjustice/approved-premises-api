package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1.planning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.BedEnded
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.BedOutOfService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.Characteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningModelsFactory
import java.time.LocalDate

class SpacePlanningModelsFactoryTest {

  val factory = SpacePlanningModelsFactory()

  @Nested
  inner class AllBeds {

    @Test
    fun `all room and bed properties including active characteristics are correctly mapped`() {
      val characteristic1 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED).withIsActive(true).withModelScope("room").produce()
      val characteristic2 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ARSON_SUITABLE).withIsActive(true).withModelScope("room").produce()
      val characteristicSingleRoom = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_SINGLE_ROOM).withIsActive(true).withModelScope("room").produce()
      val characteristicDisabled = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ENSUITE).withIsActive(false).withModelScope("room").produce()
      val characteristicNotAllowed = CharacteristicEntityFactory().withPropertyName("not in allow list").withIsActive(true).withModelScope("room").produce()
      val characteristicPremise = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED).withIsActive(true).withModelScope("premises").produce()

      val roomEntity = RoomEntityFactory()
        .withDefaults()
        .withName("the room name")
        .withCharacteristics(characteristic1, characteristicDisabled, characteristic2, characteristicSingleRoom, characteristicPremise, characteristicNotAllowed)
        .produce()

      val bedEntity = BedEntityFactory()
        .withDefaults()
        .withName("the bed name")
        .withRoom(roomEntity)
        .produce().apply { roomEntity.beds.add(this) }

      val result = factory.allBeds(
        premises = ApprovedPremisesEntityFactory()
          .withDefaults()
          .withRooms(roomEntity)
          .produce(),
      )

      assertThat(result).hasSize(1)

      val bed = result[0]
      assertThat(bed.id).isEqualTo(bedEntity.id)
      assertThat(bed.label).isEqualTo("the bed name")

      val room = result[0].room
      assertThat(room.id).isEqualTo(roomEntity.id)
      assertThat(room.label).isEqualTo("the room name")

      val characteristics = room.characteristics
      assertThat(characteristics).hasSize(3)

      assertThat(characteristics).containsOnly(
        Characteristic(
          label = characteristic1.propertyName!!,
          propertyName = characteristic1.propertyName!!,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = characteristic2.propertyName!!,
          propertyName = characteristic2.propertyName!!,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = characteristicSingleRoom.propertyName!!,
          propertyName = characteristicSingleRoom.propertyName!!,
          weighting = 100,
          singleRoom = true,
        ),
      )
    }
  }

  @Nested
  inner class AllBedsDayState {

    @Test
    fun `no rooms defined, return empty list`() {
      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 1, 1),
        premises = ApprovedPremisesEntityFactory()
          .withDefaults()
          .withRooms(mutableListOf())
          .produce(),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `no beds defined, return empty list`() {
      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 1, 1),
        premises = ApprovedPremisesEntityFactory()
          .withDefaults()
          .withRooms(RoomEntityFactory().withDefaults().produce())
          .produce(),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `all room and bed properties including active characteristics are correctly mapped`() {
      val characteristic1 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED).withIsActive(true).withModelScope("room").produce()
      val characteristic2 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ARSON_SUITABLE).withIsActive(true).withModelScope("room").produce()
      val characteristicSingleRoom = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_SINGLE_ROOM).withIsActive(true).withModelScope("room").produce()
      val characteristicNotAllowed = CharacteristicEntityFactory().withPropertyName("not in allow list").withIsActive(true).withModelScope("room").produce()
      val characteristicDisabled = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ENSUITE).withIsActive(false).withModelScope("room").produce()
      val characteristicPremise = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED).withIsActive(true).withModelScope("premises").produce()

      val roomEntity = RoomEntityFactory()
        .withDefaults()
        .withName("the room name")
        .withCharacteristics(characteristic1, characteristicDisabled, characteristic2, characteristicSingleRoom, characteristicPremise, characteristicNotAllowed)
        .produce()

      val bedEntity = BedEntityFactory()
        .withDefaults()
        .withName("the bed name")
        .withRoom(roomEntity)
        .produce().apply { roomEntity.beds.add(this) }

      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 1, 1),
        premises = ApprovedPremisesEntityFactory()
          .withDefaults()
          .withRooms(roomEntity)
          .produce(),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).hasSize(1)

      val bedDayState = result[0]
      assertThat(bedDayState.day).isEqualTo(LocalDate.of(2020, 1, 1))
      assertThat(bedDayState.inactiveReason).isNull()

      val bed = bedDayState.bed
      assertThat(bed.id).isEqualTo(bedEntity.id)
      assertThat(bed.label).isEqualTo("the bed name")

      val room = bed.room
      assertThat(room.id).isEqualTo(roomEntity.id)
      assertThat(room.label).isEqualTo("the room name")

      val characteristics = room.characteristics
      assertThat(characteristics).hasSize(3)

      assertThat(characteristics).containsOnly(
        Characteristic(
          label = characteristic1.propertyName!!,
          propertyName = characteristic1.propertyName!!,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = characteristic2.propertyName!!,
          propertyName = characteristic2.propertyName!!,
          weighting = 100,
          singleRoom = false,
        ),
        Characteristic(
          label = characteristicSingleRoom.propertyName!!,
          propertyName = characteristicSingleRoom.propertyName!!,
          weighting = 100,
          singleRoom = true,
        ),
      )
    }

    @Test
    fun `mark beds with end date in the past as inactive`() {
      val roomEntity = RoomEntityFactory()
        .withDefaults()
        .withName("the room name")
        .produce()

      val bed1EntityActive = BedEntityFactory()
        .withDefaults()
        .withName("the active bed name")
        .withEndDate(LocalDate.of(2020, 4, 5))
        .produce().apply {
          roomEntity.beds.add(this)
        }

      val bed2EntityEndedYesterday = BedEntityFactory()
        .withDefaults()
        .withName("the ended bed name")
        .withEndDate(LocalDate.of(2020, 4, 3))
        .produce().apply {
          roomEntity.beds.add(this)
        }

      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 4, 4),
        premises = ApprovedPremisesEntityFactory()
          .withDefaults()
          .withRooms(roomEntity)
          .produce(),
        outOfServiceBedRecordsToConsider = emptyList(),
      )

      assertThat(result).hasSize(2)

      val activeBedDayState = result[0]
      assertThat(activeBedDayState.inactiveReason).isNull()
      assertThat(activeBedDayState.bed.id).isEqualTo(bed1EntityActive.id)
      assertThat(activeBedDayState.bed.label).isEqualTo("the active bed name")

      val inactiveBedDayState = result[1]
      assertThat(inactiveBedDayState.inactiveReason).isInstanceOf(BedEnded::class.java)
      assertThat(inactiveBedDayState.bed.id).isEqualTo(bed2EntityEndedYesterday.id)
      assertThat(inactiveBedDayState.bed.label).isEqualTo("the ended bed name")
    }

    @Test
    fun `exclude out of service beds`() {
      val roomEntity = RoomEntityFactory()
        .withDefaults()
        .withName("the room name")
        .produce()

      val bed1EntityActive = BedEntityFactory()
        .withDefaults()
        .withName("the active bed name")
        .produce().apply {
          roomEntity.beds.add(this)
        }

      val bed2EntityOutOfService = BedEntityFactory()
        .withDefaults()
        .withName("the oosb bed name")
        .produce().apply {
          roomEntity.beds.add(this)
        }

      val result = factory.allBedsDayState(
        day = LocalDate.of(2020, 4, 4),
        premises = ApprovedPremisesEntityFactory()
          .withDefaults()
          .withRooms(roomEntity)
          .produce(),
        outOfServiceBedRecordsToConsider = listOf(
          Cas1OutOfServiceBedEntityFactory()
            .withBed(bed1EntityActive)
            .produce().apply {
              revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
                .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
                .withStartDate(LocalDate.of(2020, 4, 1))
                .withEndDate(LocalDate.of(2020, 4, 3))
                .withOutOfServiceBed(this)
                .produce()
            },
          Cas1OutOfServiceBedEntityFactory()
            .withBed(bed2EntityOutOfService)
            .produce().apply {
              revisionHistory += Cas1OutOfServiceBedRevisionEntityFactory()
                .withDetailType(Cas1OutOfServiceBedRevisionType.INITIAL)
                .withStartDate(LocalDate.of(2020, 4, 3))
                .withEndDate(LocalDate.of(2020, 4, 5))
                .withOutOfServiceBed(this)
                .produce()
            },
        ),
      )

      assertThat(result).hasSize(2)

      val activeBedDayState = result[0]
      assertThat(activeBedDayState.inactiveReason).isNull()
      assertThat(activeBedDayState.bed.id).isEqualTo(bed1EntityActive.id)
      assertThat(activeBedDayState.bed.label).isEqualTo("the active bed name")

      val inactiveBedDayState = result[1]
      assertThat(inactiveBedDayState.inactiveReason).isInstanceOf(BedOutOfService::class.java)
      assertThat(inactiveBedDayState.bed.id).isEqualTo(bed2EntityOutOfService.id)
      assertThat(inactiveBedDayState.bed.label).isEqualTo("the oosb bed name")
    }
  }

  @Nested
  inner class SpaceBookingsForDay {

    @Test
    fun `no space bookings defined, return empty list`() {
      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = emptyList(),
      )

      assertThat(result).isEmpty()
    }

    @Test
    fun `all booking properties including active room characteristics are correctly mapped`() {
      val characteristic1 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED).withIsActive(true).withModelScope("room").produce()
      val characteristic2 = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ARSON_SUITABLE).withIsActive(true).withModelScope("room").produce()
      val characteristicSingleRoom = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_SINGLE_ROOM).withModelScope("room").withIsActive(true).produce()
      val characteristicNotAllowed = CharacteristicEntityFactory().withPropertyName("not in allow list").withIsActive(true).withModelScope("room").produce()
      val characteristicDisabled = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_ENSUITE).withIsActive(false).withModelScope("room").produce()
      val characteristicPremise = CharacteristicEntityFactory().withPropertyName(CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED).withIsActive(true).withModelScope("premises").produce()

      val booking1 = Cas1SpaceBookingEntityFactory()
        .withCrn("booking1")
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .withCriteria(mutableListOf(characteristic1, characteristic2, characteristicPremise))
        .produce()

      val booking2 = Cas1SpaceBookingEntityFactory()
        .withCrn("booking2")
        .withCanonicalArrivalDate(LocalDate.of(2020, 4, 4))
        .withCanonicalDepartureDate(LocalDate.of(2020, 4, 5))
        .withCriteria(mutableListOf(characteristicSingleRoom, characteristicDisabled, characteristicPremise, characteristicNotAllowed))
        .produce()

      val result = factory.spaceBookingsForDay(
        day = LocalDate.of(2020, 4, 4),
        spaceBookingsToConsider = listOf(booking1, booking2),
      )

      assertThat(result).hasSize(2)
      assertThat(result).containsOnly(
        SpaceBooking(
          id = booking1.id,
          label = "booking1",
          requiredRoomCharacteristics = setOf(
            Characteristic(
              label = characteristic1.propertyName!!,
              propertyName = characteristic1.propertyName!!,
              weighting = 100,
              singleRoom = false,
            ),
            Characteristic(
              label = characteristic2.propertyName!!,
              propertyName = characteristic2.propertyName!!,
              weighting = 100,
              singleRoom = false,
            ),
          ),
        ),
        SpaceBooking(
          id = booking2.id,
          label = "booking2",
          requiredRoomCharacteristics = setOf(
            Characteristic(
              label = characteristicSingleRoom.propertyName!!,
              propertyName = characteristicSingleRoom.propertyName!!,
              weighting = 100,
              singleRoom = true,
            ),
          ),
        ),
      )
    }
  }
}
