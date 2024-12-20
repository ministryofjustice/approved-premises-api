package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OverbookingRange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.planning.SpacePlanningService.PremiseCapacitySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.DateRange
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
class Cas1PremisesServiceTest {

  @MockK
  lateinit var approvedPremisesRepository: ApprovedPremisesRepository

  @MockK
  lateinit var premisesService: PremisesService

  @MockK
  lateinit var outOfServiceBedService: Cas1OutOfServiceBedService

  @MockK
  lateinit var spacePlanningService: SpacePlanningService

  @InjectMockKs
  lateinit var service: Cas1PremisesService

  companion object CONSTANTS {
    val PREMISES_ID: UUID = UUID.randomUUID()
  }

  @Nested
  inner class GetPremisesSummary {

    @Test
    fun `premises not found return error`() {
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns null

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    }

    @Test
    fun success() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .withManagerDetails("manager details")
        .withSupportsSpaceBookings(true)
        .produce()

      val premisesCapacitySummary = PremiseCapacitySummary(
        premise = premises,
        range = DateRange(LocalDate.now(), LocalDate.now().plusWeeks(12)),
        byDay = emptyList(),
      )

      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56
      every { outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4
      every { spacePlanningService.capacity(premises, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val premisesSummaryInfo = result.value
      assertThat(premisesSummaryInfo.entity).isEqualTo(premises)
      assertThat(premisesSummaryInfo.bedCount).isEqualTo(56)
      assertThat(premisesSummaryInfo.outOfServiceBeds).isEqualTo(4)
      assertThat(premisesSummaryInfo.availableBeds).isEqualTo(52)
      assertThat(premisesSummaryInfo.overbookingSummary).isEmpty()
    }

    @Test
    fun shouldCalculateCorrectOverBookingSummary() {
      val premises = ApprovedPremisesEntityFactory()
        .withDefaults()
        .withId(PREMISES_ID)
        .withName("the name")
        .withApCode("the ap code")
        .withPostcode("LE11 1PO")
        .withManagerDetails("manager details")
        .withSupportsSpaceBookings(true)
        .produce()

      val premisesCapacitySummary = PremiseCapacitySummary(
        premise = premises,
        range = DateRange(LocalDate.now(), LocalDate.now().plusWeeks(12)),
        byDay = listOf(SpacePlanningService.PremiseCapacityForDay(LocalDate.of(2024, 11, 12), 5, 2, 7, emptyList())),
      )

      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premises

      every { premisesService.getBedCount(premises) } returns 56
      every { outOfServiceBedService.getCurrentOutOfServiceBedsCountForPremisesId(PREMISES_ID) } returns 4
      every { spacePlanningService.capacity(premises, any(), null) } returns premisesCapacitySummary

      val result = service.getPremisesSummary(PREMISES_ID)

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success

      val premisesSummaryInfo = result.value
      assertThat(premisesSummaryInfo.entity).isEqualTo(premises)
      assertThat(premisesSummaryInfo.bedCount).isEqualTo(56)
      assertThat(premisesSummaryInfo.outOfServiceBeds).isEqualTo(4)
      assertThat(premisesSummaryInfo.availableBeds).isEqualTo(52)
      assertThat(premisesSummaryInfo.overbookingSummary).isEqualTo(listOf(Cas1OverbookingRange(LocalDate.of(2024, 11, 12), LocalDate.of(2024, 11, 12))))
    }
  }

  @Nested
  inner class GetPremiseCapacity {

    @Test
    fun `premises not found return error`() {
      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns null

      val result = service.getPremiseCapacity(
        premisesId = PREMISES_ID,
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = null,
      )

      assertThat(result).isInstanceOf(CasResult.NotFound::class.java)
    }

    @Test
    fun `start before end date return error`() {
      every {
        approvedPremisesRepository.findByIdOrNull(PREMISES_ID)
      } returns ApprovedPremisesEntityFactory().withDefaults().produce()

      val result = service.getPremiseCapacity(
        premisesId = PREMISES_ID,
        startDate = LocalDate.of(2020, 1, 4),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = null,
      )

      assertThat(result).isInstanceOf(CasResult.GeneralValidationError::class.java)
      assertThat((result as CasResult.GeneralValidationError).message).isEqualTo("Start Date 2020-01-04 should be before End Date 2020-01-03")
    }

    @Test
    fun success() {
      val premise = ApprovedPremisesEntityFactory().withDefaults().withId(PREMISES_ID).produce()
      val excludeSpaceBookingId = UUID.randomUUID()

      every { approvedPremisesRepository.findByIdOrNull(PREMISES_ID) } returns premise

      val capacityResponse = mockk<PremiseCapacitySummary>()

      every {
        spacePlanningService.capacity(
          premises = premise,
          range = DateRange(
            LocalDate.of(2020, 1, 2),
            LocalDate.of(2020, 1, 3),
          ),
          excludeSpaceBookingId = excludeSpaceBookingId,
        )
      } returns capacityResponse

      val result = service.getPremiseCapacity(
        premisesId = PREMISES_ID,
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 1, 3),
        excludeSpaceBookingId = excludeSpaceBookingId,
      )

      assertThat(result).isInstanceOf(CasResult.Success::class.java)
      result as CasResult.Success
      assertThat(result.value).isEqualTo(capacityResponse)
    }
  }
}
