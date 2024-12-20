package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateSpaceBookingSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateSpaceBookingSeedJobCsvRow
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1UpdateSpaceBookingSeedJobTest {

  private companion object {
    val BOOKING_ID: UUID = UUID.fromString("46335983-2742-4736-8b6d-9113629a5286")
  }

  private val arsonSuitableCharacteristic = CharacteristicEntityFactory()
    .withPropertyName(CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE)
    .produce()

  private val stepFreeCharacteristic = CharacteristicEntityFactory()
    .withPropertyName(CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED)
    .produce()

  private val otherCharacteristic = CharacteristicEntityFactory()
    .withPropertyName("otherCharacteristic")
    .produce()

  @MockK
  private lateinit var cas1SpaceBookingRepository: Cas1SpaceBookingRepository

  @MockK
  private lateinit var characteristicRepository: CharacteristicRepository

  @InjectMockKs
  private lateinit var seedJob: Cas1UpdateSpaceBookingSeedJob

  @Test
  fun `error if space booking not found`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns null

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateSpaceBookingSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          updateEventNumber = false,
          updateCriteria = false,
        ),
      )
    }.hasMessageContaining("Could not find space booking with id 46335983-2742-4736-8b6d-9113629a5286")
  }

  @Test
  fun `error if no updates requested`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns Cas1SpaceBookingEntityFactory().produce()

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateSpaceBookingSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          updateEventNumber = false,
          updateCriteria = false,
        ),
      )
    }.hasMessageContaining("Nothing to do")
  }

  @Test
  fun `update event number error if no event number specified`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns Cas1SpaceBookingEntityFactory().produce()

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateSpaceBookingSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          updateEventNumber = true,
          eventNumber = null,
          updateCriteria = false,
        ),
      )
    }.hasMessageContaining("No event number specified")
  }

  @Test
  fun `update event number error if linked to an application`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns Cas1SpaceBookingEntityFactory()
      .withApplication(ApprovedPremisesApplicationEntityFactory().withDefaults().produce())
      .produce()

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateSpaceBookingSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          updateEventNumber = true,
          eventNumber = "2",
          updateCriteria = false,
        ),
      )
    }.hasMessageContaining("Cannot update the event number for a booking linked to an application")
  }

  @Test
  fun `update event number error if linked to an offline application with an event number`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns Cas1SpaceBookingEntityFactory()
      .withApplication(null)
      .withOfflineApplication(OfflineApplicationEntityFactory().withEventNumber("1").produce())
      .produce()

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateSpaceBookingSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          updateEventNumber = true,
          eventNumber = "2",
          updateCriteria = false,
        ),
      )
    }.hasMessageContaining("Cannot update the event number for a booking linked to an offline application with an event number")
  }

  @Test
  fun `update event number`() {
    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withDeliusEventNumber("1")
      .withApplication(null)
      .withOfflineApplication(OfflineApplicationEntityFactory().withEventNumber(null).produce())
      .produce()

    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns spaceBooking
    every { cas1SpaceBookingRepository.save(spaceBooking) } returns spaceBooking

    seedJob.processRow(
      Cas1UpdateSpaceBookingSeedJobCsvRow(
        spaceBookingId = BOOKING_ID,
        updateEventNumber = true,
        eventNumber = "2",
        updateCriteria = false,
      ),
    )

    assertThat(spaceBooking.deliusEventNumber).isEqualTo("2")
  }

  @Test
  fun `update criteria error if unexpected criteria included`() {
    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns Cas1SpaceBookingEntityFactory().produce()

    assertThatThrownBy {
      seedJob.processRow(
        Cas1UpdateSpaceBookingSeedJobCsvRow(
          spaceBookingId = BOOKING_ID,
          updateEventNumber = false,
          updateCriteria = true,
          criteria = listOf(
            "unexpectedProperty1",
            CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE,
            CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
            "unexpectedProperty2",
          ),
        ),
      )
    }.hasMessageContaining("The following criteria are not supported - [unexpectedProperty1, unexpectedProperty2]")
  }

  @Test
  fun `update criteria, removing some existing`() {
    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withCriteria(otherCharacteristic, stepFreeCharacteristic)
      .produce()

    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns spaceBooking
    every {
      characteristicRepository.findAllWherePropertyNameIn(
        listOf(
          CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE,
          CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
        ),
        ServiceName.approvedPremises.value,
      )
    } returns listOf(arsonSuitableCharacteristic, stepFreeCharacteristic)
    every { cas1SpaceBookingRepository.save(spaceBooking) } returns spaceBooking

    seedJob.processRow(
      Cas1UpdateSpaceBookingSeedJobCsvRow(
        spaceBookingId = BOOKING_ID,
        updateEventNumber = false,
        updateCriteria = true,
        criteria = listOf(
          CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ARSON_SUITABLE,
          CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_STEP_FREE_DESIGNATED,
        ),
      ),
    )

    assertThat(spaceBooking.criteria).hasSize(2)
    assertThat(spaceBooking.criteria).containsOnly(arsonSuitableCharacteristic, stepFreeCharacteristic)
  }

  @Test
  fun `update criteria, remove all criteria`() {
    val spaceBooking = Cas1SpaceBookingEntityFactory()
      .withCriteria(otherCharacteristic, stepFreeCharacteristic)
      .produce()

    every { cas1SpaceBookingRepository.findByIdOrNull(BOOKING_ID) } returns spaceBooking
    every { cas1SpaceBookingRepository.save(spaceBooking) } returns spaceBooking

    seedJob.processRow(
      Cas1UpdateSpaceBookingSeedJobCsvRow(
        spaceBookingId = BOOKING_ID,
        updateEventNumber = false,
        updateCriteria = true,
        criteria = emptyList(),
      ),
    )

    assertThat(spaceBooking.criteria).isEmpty()
  }
}
