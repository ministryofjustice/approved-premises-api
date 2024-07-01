package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas3

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.TransitionalAccommodationReferralReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingsReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportData
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TransitionalAccommodationReferralReportRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas3.Cas3ReportService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.io.ByteArrayOutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Cas3ReportServiceTest {
  private val mockOffenderService = mockk<OffenderService>()
  private val mockUserService = mockk<UserService>()
  private val mockTransitionalAccommodationReferralReportRowRepository =
    mockk<TransitionalAccommodationReferralReportRepository>()
  private val mockBookingsReportRepository = mockk<BookingsReportRepository>()
  private val mockLostBedsRepository = mockk<LostBedsRepository>()
  private val mockBookingTransformer = mockk<BookingTransformer>()
  private val mockWorkingDayService = mockk<WorkingDayService>()
  private val mockBookingRepository = mockk<BookingRepository>()
  private val mockBedRepository = mockk<BedRepository>()

  private val cas3ReportService = Cas3ReportService(
    mockOffenderService,
    mockUserService,
    mockTransitionalAccommodationReferralReportRowRepository,
    mockBookingsReportRepository,
    mockLostBedsRepository,
    mockBookingTransformer,
    mockWorkingDayService,
    mockBookingRepository,
    mockBedRepository,
    2,
  )

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report with required information`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportData()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(any(), any(), any()) } returns listOf(testTransitionalAccommodationReferralReportData)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate,
        endDate,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report for 3 months`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 4, 1)
    val probationRegionId = UUID.randomUUID()
    val testTransitionalAccommodationReferralReportData = createDBReferralReportData()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(any(), any(), any()) } returns listOf(testTransitionalAccommodationReferralReportData)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    val cas3ReportServiceWithThreeMonths = Cas3ReportService(
      mockOffenderService,
      mockUserService,
      mockTransitionalAccommodationReferralReportRowRepository,
      mockBookingsReportRepository,
      mockLostBedsRepository,
      mockBookingTransformer,
      mockWorkingDayService,
      mockBookingRepository,
      mockBedRepository,
      3,
    )

    cas3ReportServiceWithThreeMonths.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate,
        endDate,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate empty report`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every { mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(any(), any(), any()) } returns emptyList()
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns emptyList()

    cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate,
        endDate,
        probationRegionId,
      )
    }
    verify(exactly = 1) { mockUserService.getUserForRequest() }
    verify(exactly = 0) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report and call offender service 2 times when crn search limit exceed`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        any(),
        any(),
        any(),
      )
    } returns listOf(
      createDBReferralReportData("crn1"),
      createDBReferralReportData("crn2"),
      createDBReferralReportData("crn3"),
    )
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate,
        endDate,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 2) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createCas3ApplicationReferralsReport successfully generate report fail when offender service call fails`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = TransitionalAccommodationReferralReportProperties(probationRegionId, startDate, endDate)

    every {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        any(),
        any(),
        any(),
      )
    } returns listOf(
      createDBReferralReportData("crn1"),
      createDBReferralReportData("crn2"),
      createDBReferralReportData("crn3"),
    )
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn1", "crn2"), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )
    every { mockOffenderService.getOffenderSummariesByCrns(setOf("crn3"), any()) } throws RuntimeException("some exception")

    Assertions.assertThatExceptionOfType(RuntimeException::class.java)
      .isThrownBy {
        cas3ReportService.createCas3ApplicationReferralsReport(properties, ByteArrayOutputStream())
      }

    verify {
      mockTransitionalAccommodationReferralReportRowRepository.findAllReferrals(
        startDate,
        endDate,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 2) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createBookingsReport successfully generate report with required information`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val bookingsReportData = createBookingReportData("P131431")
    val properties = BookingsReportProperties(ServiceName.temporaryAccommodation, probationRegionId, startDate, endDate)

    every { mockBookingsReportRepository.findAllByOverlappingDate(startDate, endDate, ServiceName.temporaryAccommodation.value, probationRegionId) } returns listOf(bookingsReportData)
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    cas3ReportService.createBookingsReport(properties, ByteArrayOutputStream())

    verify {
      mockBookingsReportRepository.findAllByOverlappingDate(
        startDate,
        endDate,
        ServiceName.temporaryAccommodation.value,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 1) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  @Test
  fun `createBookingsReport successfully generate report with required information and call offender service 2 times`() {
    val startDate = LocalDate.of(2024, 1, 1)
    val endDate = LocalDate.of(2024, 1, 31)
    val probationRegionId = UUID.randomUUID()
    val properties = BookingsReportProperties(ServiceName.temporaryAccommodation, probationRegionId, startDate, endDate)

    every {
      mockBookingsReportRepository.findAllByOverlappingDate(
        startDate,
        endDate,
        ServiceName.temporaryAccommodation.value,
        probationRegionId,
      )
    } returns listOf(
      createBookingReportData("P131431"),
      createBookingReportData("P131432"),
      createBookingReportData("P131433"),
    )
    every { mockUserService.getUserForRequest() } returns UserEntityFactory().withUnitTestControlProbationRegion().produce()
    every { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) } returns listOf(
      PersonSummaryInfoResult.Success.Full("", CaseSummaryFactory().produce()),
    )

    cas3ReportService.createBookingsReport(properties, ByteArrayOutputStream())

    verify {
      mockBookingsReportRepository.findAllByOverlappingDate(
        startDate,
        endDate,
        ServiceName.temporaryAccommodation.value,
        probationRegionId,
      )
    }
    verify { mockUserService.getUserForRequest() }
    verify(exactly = 2) { mockOffenderService.getOffenderSummariesByCrns(any<Set<String>>(), any()) }
  }

  private fun createDBReferralReportData(): TestTransitionalAccommodationReferralReportData {
    return createDBReferralReportData("crn")
  }
  private fun createDBReferralReportData(crn: String) = TestTransitionalAccommodationReferralReportData(
    UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
    LocalDate.now(), crn, LocalDate.now(), "riskOfSeriousHarm",
    registeredSexOffender = false,
    historyOfSexualOffence = false,
    concerningSexualBehaviour = true,
    needForAccessibleProperty = true,
    historyOfArsonOffence = false,
    concerningArsonBehaviour = true,
    dutyToReferMade = true,
    dateDutyToReferMade = LocalDate.now(),
    probationRegionName = "region",
    dutyToReferLocalAuthorityAreaName = "name",
    dutyToReferOutcome = "outcome",
    assessmentDecision = null,
    referralRejectionReason = null,
    assessmentSubmittedDate = LocalDate.now(),
    referralEligibleForCas3 = true,
    referralEligibilityReason = "reason",
    accommodationRequiredDate = Timestamp.valueOf(LocalDateTime.now()),
    prisonNameOnCreation = null,
    personReleaseDate = null,
    town = null,
    postCode = null,
    pdu = null,
    prisonReleaseTypes = null,
    updatedReleaseDate = null,
    updatedAccommodationRequiredFromDate = null,
  )

  private fun createBookingReportData(crn: String) = TestBookingsReportData(
    bookingId = UUID.randomUUID().toString(),
    referralId = null,
    referralDate = null,
    riskOfSeriousHarm = null,
    registeredSexOffender = null,
    historyOfSexualOffence = null,
    concerningSexualBehaviour = null,
    needForAccessibleProperty = null,
    historyOfArsonOffence = null,
    concerningArsonBehaviour = null,
    dutyToReferMade = null,
    dateDutyToReferMade = null,
    referralEligibleForCas3 = null,
    referralEligibilityReason = null,
    probationRegionName = "some-region",
    localAuthorityAreaName = null,
    crn = crn,
    confirmationId = null,
    cancellationId = null,
    cancellationReason = null,
    startDate = null,
    endDate = null,
    actualEndDate = null,
    accommodationOutcome = null,
    dutyToReferLocalAuthorityAreaName = null,
    pdu = null,
    town = null,
    postCode = null,
  )

  @Suppress("LongParameterList")
  class TestTransitionalAccommodationReferralReportData(
    override val assessmentId: String,
    override val referralId: String,
    override val bookingId: String?,
    override val referralCreatedDate: LocalDate,
    override val crn: String,
    override val referralSubmittedDate: LocalDate?,
    override val riskOfSeriousHarm: String?,
    override val registeredSexOffender: Boolean?,
    override val historyOfSexualOffence: Boolean?,
    override val concerningSexualBehaviour: Boolean?,
    override val needForAccessibleProperty: Boolean?,
    override val historyOfArsonOffence: Boolean?,
    override val concerningArsonBehaviour: Boolean?,
    override val dutyToReferMade: Boolean?,
    override val dateDutyToReferMade: LocalDate?,
    override val probationRegionName: String,
    override val dutyToReferLocalAuthorityAreaName: String?,
    override val dutyToReferOutcome: String?,
    override val assessmentDecision: String?,
    override val referralRejectionReason: String?,
    override val assessmentSubmittedDate: LocalDate?,
    override val referralEligibleForCas3: Boolean?,
    override val referralEligibilityReason: String?,
    override val accommodationRequiredDate: Timestamp?,
    override val prisonNameOnCreation: String?,
    override val personReleaseDate: LocalDate?,
    override val town: String?,
    override val postCode: String?,
    override val pdu: String?,
    override val prisonReleaseTypes: String?,
    override val updatedReleaseDate: LocalDate?,
    override val updatedAccommodationRequiredFromDate: LocalDate?,
  ) : TransitionalAccommodationReferralReportData

  @Suppress("LongParameterList")
  class TestBookingsReportData(
    override val bookingId: String,
    override val referralId: String?,
    override val referralDate: LocalDate?,
    override val riskOfSeriousHarm: String?,
    override val registeredSexOffender: Boolean?,
    override val historyOfSexualOffence: Boolean?,
    override val concerningSexualBehaviour: Boolean?,
    override val needForAccessibleProperty: Boolean?,
    override val historyOfArsonOffence: Boolean?,
    override val concerningArsonBehaviour: Boolean?,
    override val dutyToReferMade: Boolean?,
    override val dateDutyToReferMade: LocalDate?,
    override val referralEligibleForCas3: Boolean?,
    override val referralEligibilityReason: String?,
    override val probationRegionName: String,
    override val localAuthorityAreaName: String?,
    override val crn: String,
    override val confirmationId: String?,
    override val cancellationId: String?,
    override val cancellationReason: String?,
    override val startDate: LocalDate?,
    override val endDate: LocalDate?,
    override val actualEndDate: Timestamp?,
    override val accommodationOutcome: String?,
    override val dutyToReferLocalAuthorityAreaName: String?,
    override val pdu: String?,
    override val town: String?,
    override val postCode: String?,
  ) : BookingsReportData
}
