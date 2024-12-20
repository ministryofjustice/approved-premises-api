package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.DomainEventUrlConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedAssessedByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeBookedByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.DailyMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ConfiguredDomainEventWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventMigrationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService.MonthSpecificReportParams
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.ObjectMapperFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.util.UUID

class DailyMetricsReportGeneratorTest {
  private val objectMapper = ObjectMapperFactory.createRuntimeLikeObjectMapper()

  private val domainEventService = Cas1DomainEventService(
    objectMapper,
    mockk<DomainEventRepository>(),
    mockk<ConfiguredDomainEventWorker>(),
    mockk<UserService>(),
    false,
    mockk<DomainEventUrlConfig>(),
    mockk<Cas1DomainEventMigrationService>(),
  )

  private val dates = listOf(
    LocalDate.parse("2023-01-01"),
    LocalDate.parse("2023-01-02"),
    LocalDate.parse("2023-01-03"),
  )

  private val properties = MonthSpecificReportParams(
    month = 1,
    year = 2023,
  )

  private val user1 = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val user2 = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  private val user3 = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .withYieldedApArea { ApAreaEntityFactory().produce() }
        .produce()
    }
    .produce()

  @Test
  fun `it groups applications and domain events by date`() {
    val applications = mapOf(
      dates[0] to mapOf(
        user1 to (1..4).map { ApprovedPremisesApplicationMetricsSummaryDto(dates[0], user1.id.toString()) },
        user2 to (1..2).map { ApprovedPremisesApplicationMetricsSummaryDto(dates[0], user2.id.toString()) },
        user3 to (1..3).map { ApprovedPremisesApplicationMetricsSummaryDto(dates[0], user3.id.toString()) },
      ),
      dates[1] to mapOf(
        user1 to (1..4).map { ApprovedPremisesApplicationMetricsSummaryDto(dates[1], user2.id.toString()) },
      ),
      dates[2] to mapOf(
        user2 to (1..3).map { ApprovedPremisesApplicationMetricsSummaryDto(dates[2], user2.id.toString()) },
        user3 to (1..2).map { ApprovedPremisesApplicationMetricsSummaryDto(dates[2], user3.id.toString()) },
      ),
    )

    val applicationSubmittedEvents = mapOf(
      dates[0] to mapOf(
        user1 to createApplicationSubmittedEvents(user1, dates[0], 3),
        user2 to createApplicationSubmittedEvents(user2, dates[0], 1),
        user3 to createApplicationSubmittedEvents(user3, dates[0], 2),
      ),
      dates[1] to mapOf(
        user1 to createApplicationSubmittedEvents(user1, dates[1], 3),
      ),
      dates[2] to mapOf(
        user2 to createApplicationSubmittedEvents(user2, dates[2], 1),
        user3 to createApplicationSubmittedEvents(user3, dates[2], 2),
      ),
    )

    val assessmentCompletedEvents = mapOf(
      dates[0] to mapOf(
        user3 to createAssessmentCompletedEvents(user3, dates[0], 1),
      ),
      dates[1] to mapOf(
        user1 to createAssessmentCompletedEvents(user1, dates[1], 4),
        user2 to createAssessmentCompletedEvents(user2, dates[1], 1),
        user3 to createAssessmentCompletedEvents(user3, dates[1], 3),
      ),
    )

    val bookingMadeEvents = mapOf(
      dates[0] to mapOf(
        user1 to createBookingMadeEvents(user1, dates[0], 1),
      ),
      dates[1] to mapOf(
        user1 to createBookingMadeEvents(user1, dates[1], 3),
        user2 to createBookingMadeEvents(user2, dates[1], 3),
      ),
      dates[2] to mapOf(
        user1 to createBookingMadeEvents(user1, dates[2], 4),
      ),
    )

    val allApplicationSubmittedEvents =
      applicationSubmittedEvents.flatMap { events -> events.value.flatMap { it.value } }
    val allAssessmentCompletedEvents = assessmentCompletedEvents.flatMap { events -> events.value.flatMap { it.value } }
    val allBookingMadeEvents = bookingMadeEvents.flatMap { events -> events.value.flatMap { it.value } }

    val generator = DailyMetricsReportGenerator(
      listOf(
        allApplicationSubmittedEvents,
        allAssessmentCompletedEvents,
        allBookingMadeEvents,
      ).flatten(),
      applications.flatMap { a -> a.value.flatMap { it.value } },
      domainEventService,
    )
    val results = generator.createReport(dates, properties)

    dates.forEachIndexed { i, date ->
      assertThat(results[i]["date"]).isEqualTo(date)
      assertThat(results[i]["applicationsStarted"]).isEqualTo(countEntitiesForDate(applications, date))
      assertThat(results[i]["uniqueUsersStartingApplications"]).isEqualTo(countUniqueUsersForDate(applications, date))
      assertThat(results[i]["applicationsSubmitted"]).isEqualTo(countEntitiesForDate(applicationSubmittedEvents, date))
      assertThat(results[i]["uniqueUsersSubmittingApplications"]).isEqualTo(
        countUniqueUsersForDate(
          applicationSubmittedEvents,
          date,
        ),
      )
      assertThat(results[i]["assessmentsCompleted"]).isEqualTo(countEntitiesForDate(assessmentCompletedEvents, date))
      assertThat(results[i]["uniqueUsersCompletingAssessments"]).isEqualTo(
        countUniqueUsersForDate(
          assessmentCompletedEvents,
          date,
        ),
      )
      assertThat(results[i]["bookingsMade"]).isEqualTo(countEntitiesForDate(bookingMadeEvents, date))
      assertThat(results[i]["uniqueUsersMakingBookings"]).isEqualTo(countUniqueUsersForDate(bookingMadeEvents, date))
    }
  }

  private fun createApplicationSubmittedEvents(user: UserEntity, date: LocalDate, count: Int) =
    DomainEventEntityFactory()
      .withOccurredAt(date.toLocalDateTime())
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
      .withData(
        objectMapper.writeValueAsString(
          ApplicationSubmittedEnvelope(
            id = UUID.randomUUID(),
            timestamp = date.toLocalDateTime().toInstant(),
            eventType = EventType.applicationSubmitted,
            eventDetails = ApplicationSubmittedFactory()
              .withSubmittedByStaffMember(
                StaffMemberFactory()
                  .withStaffCode(
                    user.deliusStaffCode,
                  )
                  .produce(),
              )
              .produce(),
          ),
        ),
      ).produceMany().take(count).toList()

  private fun createAssessmentCompletedEvents(user: UserEntity, date: LocalDate, count: Int) =
    DomainEventEntityFactory()
      .withOccurredAt(date.toLocalDateTime())
      .withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      .withData(
        objectMapper.writeValueAsString(
          ApplicationAssessedEnvelope(
            id = UUID.randomUUID(),
            timestamp = date.toLocalDateTime().toInstant(),
            eventType = EventType.applicationAssessed,
            eventDetails = ApplicationAssessedFactory()
              .withAssessedBy(
                ApplicationAssessedAssessedByFactory()
                  .withStaffMember(
                    StaffMemberFactory()
                      .withStaffCode(
                        user.deliusStaffCode,
                      ).produce(),
                  ).produce(),
              )
              .withArrivalDate(date.toLocalDateTime().toInstant())
              .produce(),
          ),
        ),
      ).produceMany().take(count).toList()

  private fun createBookingMadeEvents(user: UserEntity, date: LocalDate, count: Int) = DomainEventEntityFactory()
    .withOccurredAt(date.toLocalDateTime())
    .withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
    .withData(
      objectMapper.writeValueAsString(
        BookingMadeEnvelope(
          id = UUID.randomUUID(),
          timestamp = date.toLocalDateTime().toInstant(),
          eventType = EventType.bookingMade,
          eventDetails = BookingMadeFactory()
            .withBookedBy(
              BookingMadeBookedByFactory()
                .withStaffMember(
                  StaffMemberFactory()
                    .withStaffCode(
                      user.deliusStaffCode,
                    ).produce(),
                ).produce(),
            )
            .produce(),
        ),
      ),
    ).produceMany().take(count).toList()

  private fun <T> countEntitiesForDate(events: Map<LocalDate, Map<UserEntity, List<T>>>, date: LocalDate) =
    events[date]?.flatMap { it.value }?.count() ?: 0

  private fun <T> countUniqueUsersForDate(events: Map<LocalDate, Map<UserEntity, List<T>>>, date: LocalDate) =
    events[date]?.keys?.count() ?: 0
}
