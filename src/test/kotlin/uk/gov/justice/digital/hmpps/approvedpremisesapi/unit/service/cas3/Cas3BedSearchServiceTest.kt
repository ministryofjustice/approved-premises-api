package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OverlapBookingsSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BedSearchRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3BedSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CharacteristicNames
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationBedSearchResultOverlap
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3BedspaceSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.assertThat
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringMultiCaseWithNumbers
import java.time.LocalDate
import java.util.UUID

class Cas3BedSearchServiceTest {
  private val mockBedSearchRepository = mockk<BedSearchRepository>()
  private val mockCharacteristicService = mockk<CharacteristicService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockProbationDeliveryUnitRepository = mockk<ProbationDeliveryUnitRepository>()
  private val mockOffenderService = mockk<OffenderService>()

  private val bedSearchService = Cas3BedspaceSearchService(
    mockBedSearchRepository,
    mockBookingRepository,
    mockProbationDeliveryUnitRepository,
    mockCharacteristicService,
    mockWorkingDayService,
    mockOffenderService,
  )

  @Test
  fun `findBedspaces returns FieldValidationError when duration in days is less than 1`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    every {
      mockProbationDeliveryUnitRepository.existsById(probationDeliveryUnit.id)
    } returns true

    val result = bedSearchService.findBedspaces(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 0,
      probationDeliveryUnits = listOf(probationDeliveryUnit.id),
      propertyBedAttributes = null,
    )

    assertThat(result).isFieldValidationError("$.durationDays", "mustBeAtLeast1")
  }

  @Test
  fun `findBedspaces returns FieldValidationError when number of pdus is greater than pdus limit`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnitIds = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produceMany()
      .take(7)
      .map { it.id }
      .toList()

    val result = bedSearchService.findBedspaces(
      user = user,
      startDate = LocalDate.parse("2024-08-22"),
      durationInDays = 30,
      probationDeliveryUnits = probationDeliveryUnitIds,
      propertyBedAttributes = null,
    )
    assertThat(result).isFieldValidationError("$.probationDeliveryUnits", "maxNumberProbationDeliveryUnits")
  }

  @Test
  fun `findBedspaces returns FieldValidationError when a pdu does not exist`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnitIds = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produceMany()
      .take(3)
      .map { it.id }
      .toMutableList()

    val notExistPduId = UUID.randomUUID()
    probationDeliveryUnitIds.add(notExistPduId)

    every {
      mockProbationDeliveryUnitRepository.existsById(match { probationDeliveryUnitIds.contains(it) })
    } returns true

    every {
      mockProbationDeliveryUnitRepository.existsById(notExistPduId)
    } returns false

    val result = bedSearchService.findBedspaces(
      user = user,
      startDate = LocalDate.parse("2024-08-28"),
      durationInDays = 84,
      probationDeliveryUnits = probationDeliveryUnitIds,
      propertyBedAttributes = null,
    )
    assertThat(result).isFieldValidationError("$.probationDeliveryUnits[3]", "doesNotExist")
  }

  @Test
  fun `findBedspaces returns results from repository`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    val caseSummary = CaseSummaryFactory()
      .produce()

    val overlapBookingsSearchResult = TestOverlapBookingsSearchResult(
      bookingId = UUID.randomUUID(),
      crn = caseSummary.crn,
      arrivalDate = LocalDate.parse("2023-02-15"),
      departureDate = LocalDate.parse("2023-03-10"),
      premisesId = UUID.randomUUID(),
      roomId = UUID.randomUUID(),
      assessmentId = UUID.randomUUID(),
    )

    val repositorySearchResults = listOf(
      Cas3BedSearchResult(
        premisesId = overlapBookingsSearchResult.premisesId,
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedCount = 3,
        bookedBedCount = 0,
        roomId = overlapBookingsSearchResult.roomId,
        roomName = "Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Bed Name",
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
        overlaps = mutableListOf(
          TemporaryAccommodationBedSearchResultOverlap(
            name = caseSummary.name.forename,
            sex = caseSummary.gender,
            personType = PersonType.fullPerson,
            crn = overlapBookingsSearchResult.crn,
            days = 7,
            premisesId = overlapBookingsSearchResult.premisesId,
            roomId = overlapBookingsSearchResult.roomId,
            bookingId = overlapBookingsSearchResult.bookingId,
            assessmentId = overlapBookingsSearchResult.assessmentId,
          ),
        ),
      ),
    )

    every {
      mockBedSearchRepository.findTemporaryAccommodationBeds(
        startDate = LocalDate.parse("2023-03-22"),
        endDate = LocalDate.parse("2023-03-28"),
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        probationRegionId = user.probationRegion.id,
        premisesCharacteristicsIds = listOf(),
        roomCharacteristicsIds = listOf(),
      )
    } returns repositorySearchResults

    every { mockBookingRepository.findClosestBookingBeforeDateForBeds(any(), any()) } returns listOf()
    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers { it.invocation.args[0] as LocalDate }
    every { mockBookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(any(), any(), any()) } returns
      listOf(overlapBookingsSearchResult)
    every {
      mockProbationDeliveryUnitRepository.existsById(probationDeliveryUnit.id)
    } returns true
    every { mockOffenderService.getPersonSummaryInfoResults(setOf(caseSummary.crn), any()) } returns
      listOf(PersonSummaryInfoResult.Success.Full(caseSummary.crn, caseSummary))

    val result = bedSearchService.findBedspaces(
      user = user,
      probationDeliveryUnits = listOf(probationDeliveryUnit.id),
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      propertyBedAttributes = null,
    )

    assertThat(result).isSuccess().hasValueEqualTo(repositorySearchResults)
  }

  @Test
  fun `findBedspaces does not return results for beds that currently have turnarounds`() {
    val user = UserEntityFactory()
      .withUnitTestControlProbationRegion()
      .produce()

    val probationDeliveryUnit = ProbationDeliveryUnitEntityFactory()
      .withName(randomStringMultiCaseWithNumbers(10))
      .withProbationRegion(user.probationRegion)
      .produce()

    val expectedResults = listOf(
      Cas3BedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Premises Name",
        premisesAddressLine1 = "1 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedCount = 3,
        bookedBedCount = 0,
        roomId = UUID.randomUUID(),
        roomName = "Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Bed Name",
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
        overlaps = mutableListOf(),
      ),
    )

    // This bed is in a turnaround
    val unexpectedResults = listOf(
      Cas3BedSearchResult(
        premisesId = UUID.randomUUID(),
        premisesName = "Another Premises Name",
        premisesAddressLine1 = "2 Someplace",
        premisesAddressLine2 = null,
        premisesTown = null,
        premisesPostcode = "LA1111A",
        premisesCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "bedCharacteristicPropertyName",
            name = "Bed Characteristic Name",
          ),
        ),
        premisesNotes = "Premises notes",
        premisesBedCount = 3,
        bookedBedCount = 0,
        roomId = UUID.randomUUID(),
        roomName = "Another Room Name",
        bedId = UUID.randomUUID(),
        bedName = "Another Bed Name",
        probationDeliveryUnitName = probationDeliveryUnit.name,
        roomCharacteristics = mutableListOf(
          CharacteristicNames(
            propertyName = "roomCharacteristicPropertyName",
            name = "Room Characteristic Name",
          ),
        ),
        overlaps = mutableListOf(),
      ),
    )

    val repositorySearchResults = expectedResults + unexpectedResults

    every {
      mockProbationDeliveryUnitRepository.findByName(probationDeliveryUnit.name)
    } returns probationDeliveryUnit

    every {
      mockBedSearchRepository.findTemporaryAccommodationBeds(
        startDate = LocalDate.parse("2023-03-22"),
        endDate = LocalDate.parse("2023-03-28"),
        probationDeliveryUnits = listOf(probationDeliveryUnit.id),
        probationRegionId = user.probationRegion.id,
        premisesCharacteristicsIds = listOf(),
        roomCharacteristicsIds = listOf(),
      )
    } returns repositorySearchResults

    val expectedResultPremises = TemporaryAccommodationPremisesEntityFactory()
      .withId(expectedResults[0].premisesId)
      .withProbationRegion(user.probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val expectedResultRoom = RoomEntityFactory()
      .withId(expectedResults[0].roomId)
      .withPremises(expectedResultPremises)
      .produce()

    val expectedResultBed = BedEntityFactory()
      .withId(expectedResults[0].bedId)
      .withRoom(expectedResultRoom)
      .produce()

    val expectedResultBooking = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-12-19"))
      .withDepartureDate(LocalDate.parse("2023-03-19"))
      .withPremises(expectedResultPremises)
      .withBed(expectedResultBed)
      .produce()

    val expectedTurnaround = TurnaroundEntityFactory()
      .withBooking(expectedResultBooking)
      .withWorkingDayCount(2)
      .produce()

    expectedResultBooking.turnarounds = mutableListOf(expectedTurnaround)

    val unexpectedResultPremises = TemporaryAccommodationPremisesEntityFactory()
      .withId(unexpectedResults[0].premisesId)
      .withProbationRegion(user.probationRegion)
      .withLocalAuthorityArea(
        LocalAuthorityEntityFactory()
          .produce(),
      )
      .produce()

    val unexpectedResultRoom = RoomEntityFactory()
      .withId(unexpectedResults[0].roomId)
      .withPremises(unexpectedResultPremises)
      .produce()

    val unexpectedResultBed = BedEntityFactory()
      .withId(unexpectedResults[0].bedId)
      .withRoom(unexpectedResultRoom)
      .produce()

    val unexpectedResultBooking = BookingEntityFactory()
      .withArrivalDate(LocalDate.parse("2022-12-20"))
      .withDepartureDate(LocalDate.parse("2023-03-20"))
      .withPremises(unexpectedResultPremises)
      .withBed(unexpectedResultBed)
      .produce()

    val unexpectedTurnaround = TurnaroundEntityFactory()
      .withBooking(unexpectedResultBooking)
      .withWorkingDayCount(2)
      .produce()

    unexpectedResultBooking.turnarounds = mutableListOf(unexpectedTurnaround)

    every {
      mockBookingRepository.findClosestBookingBeforeDateForBeds(
        date = any(),
        bedIds = any(),
      )
    } returns listOf(
      expectedResultBooking,
      unexpectedResultBooking,
    )

    every { mockWorkingDayService.addWorkingDays(any(), any()) } answers {
      (it.invocation.args[0] as LocalDate).plusDays((it.invocation.args[1] as Int).toLong())
    }

    every { mockBookingRepository.findAllNotCancelledByPremisesIdsAndOverlappingDate(any(), any(), any()) } returns listOf()

    every { mockOffenderService.getPersonSummaryInfoResults(any(), any()) } returns listOf()

    every {
      mockProbationDeliveryUnitRepository.existsById(probationDeliveryUnit.id)
    } returns true

    val result = bedSearchService.findBedspaces(
      user = user,
      startDate = LocalDate.parse("2023-03-22"),
      durationInDays = 7,
      probationDeliveryUnits = listOf(probationDeliveryUnit.id),
      propertyBedAttributes = null,
    )
    assertThat(result).isSuccess().hasValueEqualTo(expectedResults)
  }

  @Suppress("LongParameterList")
  class TestOverlapBookingsSearchResult(
    override val bookingId: UUID,
    override val crn: String,
    override val arrivalDate: LocalDate,
    override val departureDate: LocalDate,
    override val premisesId: UUID,
    override val roomId: UUID,
    override val assessmentId: UUID,
  ) : OverlapBookingsSearchResult
}
