package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacityForDay
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacitySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCharacteristicAvailability
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremiseCapacitySummaryTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1PremisesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class Cas1PremiseCapacitySummaryTransformerTest {

  @MockK
  lateinit var cas1PremisesTransformer: Cas1PremisesTransformer

  @InjectMockKs
  lateinit var transformer: Cas1PremiseCapacitySummaryTransformer

  @Test
  fun toCas1PremiseCapacitySummary() {
    val premise = ApprovedPremisesEntityFactory().withDefaults().produce()

    val inputPremiseSummaryInfo = mockk<Cas1PremisesService.Cas1PremisesSummaryInfo>()
    val inputCapacitySummary = PremiseCapacitySummary(
      premise = premise,
      range = DateRange(
        LocalDate.of(2020, 1, 2),
        LocalDate.of(2021, 3, 4),
      ),
      byDay = listOf(
        PremiseCapacityForDay(
          day = LocalDate.of(2020, 1, 3),
          totalBedCount = 5,
          availableBedCount = 3,
          bookingCount = 4,
          characteristicAvailability = listOf(
            PremiseCharacteristicAvailability(
              characteristicPropertyName = "isSingle",
              availableBedCount = 10,
              bookingCount = 4,
            ),
            PremiseCharacteristicAvailability(
              characteristicPropertyName = "isWheelchairDesignated",
              availableBedCount = 20,
              bookingCount = 8,
            ),
          ),
        ),

      ),
    )

    val transformedPremiseSummaryInfo = mockk<Cas1PremisesSummary>()
    every {
      cas1PremisesTransformer.toPremiseSummary(any())
    } returns transformedPremiseSummaryInfo

    val result = transformer.toCas1PremiseCapacitySummary(
      premiseSummaryInfo = inputPremiseSummaryInfo,
      premiseCapacity = inputCapacitySummary,
    )

    assertThat(result.premise).isEqualTo(transformedPremiseSummaryInfo)
    assertThat(result.startDate).isEqualTo(LocalDate.of(2020, 1, 2))
    assertThat(result.endDate).isEqualTo(LocalDate.of(2021, 3, 4))

    assertThat(result.capacity).hasSize(1)

    val capacityForDay = result.capacity[0]
    assertThat(capacityForDay.date).isEqualTo(LocalDate.of(2020, 1, 3))
    assertThat(capacityForDay.totalBedCount).isEqualTo(5)
    assertThat(capacityForDay.availableBedCount).isEqualTo(3)
    assertThat(capacityForDay.bookingCount).isEqualTo(4)

    assertThat(capacityForDay.characteristicAvailability).hasSize(2)

    val characteristicAvailability1 = capacityForDay.characteristicAvailability[0]
    assertThat(characteristicAvailability1.characteristic).isEqualTo(Cas1SpaceBookingCharacteristic.IS_SINGLE)
    assertThat(characteristicAvailability1.availableBedsCount).isEqualTo(10)
    assertThat(characteristicAvailability1.bookingsCount).isEqualTo(4)

    val characteristicAvailability2 = capacityForDay.characteristicAvailability[1]
    assertThat(characteristicAvailability2.characteristic).isEqualTo(Cas1SpaceBookingCharacteristic.IS_WHEELCHAIR_DESIGNATED)
    assertThat(characteristicAvailability2.availableBedsCount).isEqualTo(20)
    assertThat(characteristicAvailability2.bookingsCount).isEqualTo(8)
  }
}
