package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.migration

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationDecision.ACCEPTED
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob.ManualLinkFix
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toUtcOffsetDateTime
import java.time.LocalDate
import java.time.OffsetDateTime

class Cas1FixPlacementApplicationLinksJobTest {

  private val placementApplicationRepository = mockk<PlacementApplicationRepository>()
  private val applicationRepository = mockk<ApplicationRepository>()
  private val placementRequestRepository = mockk<PlacementRequestRepository>()
  private val entityManager = mockk<EntityManager>()
  private val transactionTemplate = mockk<TransactionTemplate>()

  private val service = Cas1FixPlacementApplicationLinksJob(
    placementApplicationRepository,
    applicationRepository,
    placementRequestRepository,
    entityManager,
    transactionTemplate,
  )

  private val probationRegion = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val createdByUser = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val applicationWithNoArrivalDate = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .withArrivalDate(null)
    .produce()

  private val applicationWithArrivalDate = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .withArrivalDate(LocalDate.of(2022, 1, 2).toUtcOffsetDateTime())
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(applicationWithNoArrivalDate)
    .produce()

  val placementRequirements = PlacementRequirementsEntityFactory()
    .withApplication(applicationWithNoArrivalDate)
    .withAssessment(assessment)
    .produce()

  val logger = mockk<Logger>()

  @BeforeEach
  fun setupLogger() {
    service.log = logger
    every { logger.debug(any()) } returns Unit
    every { logger.info(any()) } returns Unit
    every { logger.error(any()) } returns Unit
  }

  @Test
  fun applyManualFix() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp1 = placementApp()
    every { placementApplicationRepository.findByIdOrNull(placementApp1.id) } returns placementApp1

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    val placementRequest2 = placementRequestForDates(LocalDate.of(2021, 1, 2), 10)
    val placementRequest3 = placementRequestForDates(LocalDate.of(2022, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests = mutableListOf(
      placementRequest1,
      placementRequest2,
      placementRequest3,
    )

    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyManualFix(
      ManualLinkFix(
        applicationId = applicationWithNoArrivalDate.id,
        placementApplicationId = placementApp1.id,
        placementRequestId = placementRequest3.id,
      ),
    )

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest3.id &&
            it.placementApplication == placementApp1
        },
      )
    }
  }

  @Test
  fun `applyAutomatedFixes nothing to do`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate
    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns emptyList()

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify { placementRequestRepository wasNot Called }
  }

  @Test
  fun `applyAutomatedFixes match placement app with single date to single placement request`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithSingleDate1)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithSingleDate1
        },
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `applyAutomatedFixes match placement app with single date to single placement request ignore existing matched placement app`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020, 1, 2), 10)

    val placementAppWithSingleDate2 = placementApp()
    addDates(placementAppWithSingleDate2, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    placementRequest2.placementApplication = placementAppWithSingleDate2
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)
    placementAppWithSingleDate2.placementRequests = mutableListOf(placementRequest2)

    every {
      placementApplicationRepository.findByApplication(applicationWithNoArrivalDate)
    } returns listOf(placementAppWithSingleDate1, placementAppWithSingleDate2)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithSingleDate1
        },
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `applyAutomatedFixes match placement app with single date to single placement request when application has initial date set`() {
    every { applicationRepository.findByIdOrNull(applicationWithArrivalDate.id) } returns applicationWithArrivalDate

    val placementApp = placementApp(application = applicationWithArrivalDate)
    addDates(placementApp, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10, application = applicationWithArrivalDate)
    applicationWithArrivalDate.placementRequests.add(placementRequest1)
    val placementRequest2 = placementRequestForDates(applicationWithArrivalDate.arrivalDate!!.toLocalDate(), 10, application = applicationWithArrivalDate)
    applicationWithArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithArrivalDate) } returns listOf(placementApp)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyAutomatedFixes(applicationWithArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementApp
        },
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `applyAutomatedFixes match multiple placement apps with single date to placement requests`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020, 1, 2), 10)

    val placementAppWithSingleDate2 = placementApp()
    addDates(placementAppWithSingleDate2, LocalDate.of(2020, 2, 3), 20)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020, 2, 3), 20)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithSingleDate1, placementAppWithSingleDate2)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithSingleDate1
        },
      )
    }

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest2.id &&
            it.placementApplication == placementAppWithSingleDate2
        },
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `applyAutomatedFixes match single placement apps with multiple dates to placement requests`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithMultipleDate1 = placementApp()
    addDates(placementAppWithMultipleDate1, LocalDate.of(2020, 1, 2), 10)
    addDates(placementAppWithMultipleDate1, LocalDate.of(2020, 2, 3), 20)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020, 2, 3), 20)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithMultipleDate1)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithMultipleDate1
        },
      )
    }

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest2.id &&
            it.placementApplication == placementAppWithMultipleDate1
        },
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `applyAutomatedFixes match placement app with single date to single placement request, ignore placement apps that aren't accepted`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithSingleDate1 = placementApp()
    addDates(placementAppWithSingleDate1, LocalDate.of(2020, 1, 2), 10)

    val placementAppWithoutDecisionIsIgnored = placementApp(decision = null)
    addDates(placementAppWithoutDecisionIsIgnored, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithSingleDate1, placementAppWithoutDecisionIsIgnored)
    every { placementRequestRepository.save(any()) } answers { it.invocation.args[0] as PlacementRequestEntity }

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      placementRequestRepository.save(
        match {
          it.id == placementRequest1.id &&
            it.placementApplication == placementAppWithSingleDate1
        },
      )
    }

    assertNoErrorsLogged()
  }

  @Test
  fun `applyAutomatedFixes error if placement application has a decision date, because any application with this populated should already be correctly linked to placement requests`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementAppWithDecisionDate = placementApp(decisionDate = OffsetDateTime.now())
    addDates(placementAppWithDecisionDate, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementAppWithDecisionDate)

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)
    verify {
      logger.error(
        "We should not be considering applications where all PlacementApplications have a non-null decisionMadeAt, because this was only set for " +
          "decisions made after linking PlacementApplications to PlacementRequests was automatically managed in code. This suggests an error in the migration logic. " +
          "Placement applications are [${placementAppWithDecisionDate.id} for date 2020-01-02 and duration 10]",
      )
    }
  }

  @Test
  fun `applyAutomatedFixes error if no corresponding placement request found`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp = placementApp()
    addDates(placementApp, LocalDate.of(2020, 1, 2), 10)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementApp)

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      logger.error(
        "No placement request found for placement app ${placementApp.id} for date 2020-01-02 and " +
          "duration 10 for application ${applicationWithNoArrivalDate.id}",
      )
    }
  }

  @Test
  fun `applyAutomatedFixes error if multiple potential placement requests found`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp = placementApp()
    addDates(placementApp, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementApp)

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      logger.error(
        "More than one potential placement request found for placement app ${placementApp.id} for " +
          "date 2020-01-02 and duration 10 for application ${applicationWithNoArrivalDate.id}",
      )
    }
  }

  @Test
  fun `applyAutomatedFixes error if application has no initial date and unmatched placement requests exist`() {
    every { applicationRepository.findByIdOrNull(applicationWithNoArrivalDate.id) } returns applicationWithNoArrivalDate

    val placementApp = placementApp()
    addDates(placementApp, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest1)

    val placementRequest2 = placementRequestForDates(LocalDate.of(2020, 2, 3), 20)
    applicationWithNoArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithNoArrivalDate) } returns listOf(placementApp)

    service.applyAutomatedFixes(applicationWithNoArrivalDate.id)

    verify {
      logger.error(
        "Application ${applicationWithNoArrivalDate.id} does not have an arrival date set on the initial application, " +
          "yet there are unmatched placement requests.",
      )
    }
  }

  @Test
  fun `applyAutomatedFixes error if application has an initial date and no unmatched placement requests exist after mapping`() {
    every { applicationRepository.findByIdOrNull(applicationWithArrivalDate.id) } returns applicationWithArrivalDate

    val placementApp = placementApp(application = applicationWithArrivalDate)
    addDates(placementApp, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10, application = applicationWithArrivalDate)
    applicationWithArrivalDate.placementRequests.add(placementRequest1)

    every { placementApplicationRepository.findByApplication(applicationWithArrivalDate) } returns listOf(placementApp)

    service.applyAutomatedFixes(applicationWithArrivalDate.id)

    verify {
      logger.error(
        "Application ${applicationWithArrivalDate.id} has an arrival date set on the initial application, " +
          "but after matching to placement apps, no placement requests remain that represents this date",
      )
    }
  }

  @Test
  fun `applyAutomatedFixes error if application has an initial date and remaining placement request has different start date`() {
    every { applicationRepository.findByIdOrNull(applicationWithArrivalDate.id) } returns applicationWithArrivalDate

    val placementApp = placementApp(application = applicationWithArrivalDate)
    addDates(placementApp, LocalDate.of(2020, 1, 2), 10)

    val placementRequest1 = placementRequestForDates(LocalDate.of(2020, 1, 2), 10, application = applicationWithArrivalDate)
    applicationWithArrivalDate.placementRequests.add(placementRequest1)
    val placementRequest2 = placementRequestForDates(applicationWithArrivalDate.arrivalDate!!.toLocalDate().minusMonths(1), 10, application = applicationWithArrivalDate)
    applicationWithArrivalDate.placementRequests.add(placementRequest2)

    every { placementApplicationRepository.findByApplication(applicationWithArrivalDate) } returns listOf(placementApp)

    service.applyAutomatedFixes(applicationWithArrivalDate.id)

    verify {
      logger.error(
        "Application ${applicationWithArrivalDate.id} has an arrival date set on the initial application, " +
          "but after matching to placement apps, the only remaining placement request doesn't have the " +
          "expected arrival date (expected 2022-01-02 was 2021-12-02)",
      )
    }
  }

  private fun assertNoErrorsLogged() {
    verify(exactly = 0) { logger.error(any()) }
  }

  private fun placementApp(
    application: ApprovedPremisesApplicationEntity = applicationWithNoArrivalDate,
    decision: PlacementApplicationDecision? = ACCEPTED,
    decisionDate: OffsetDateTime? = null,
  ) =
    PlacementApplicationEntityFactory()
      .withDefaults()
      .withApplication(application)
      .withDecision(decision)
      .withDecisionMadeAt(decisionDate)
      .produce()

  private fun placementRequestForDates(
    start: LocalDate,
    duration: Int,
    application: ApprovedPremisesApplicationEntity = applicationWithNoArrivalDate,
  ) = PlacementRequestEntityFactory()
    .withPlacementRequirements(placementRequirements)
    .withApplication(application)
    .withAssessment(assessment)
    .withPlacementApplication(null)
    .withExpectedArrival(start)
    .withDuration(duration)
    .produce()

  private fun addDates(placementApp: PlacementApplicationEntity, start: LocalDate, duration: Int) {
    placementApp.placementDates.add(
      PlacementDateEntityFactory()
        .withPlacementApplication(placementApp)
        .withExpectedArrival(start)
        .withDuration(duration)
        .produce(),
    )
  }
}
