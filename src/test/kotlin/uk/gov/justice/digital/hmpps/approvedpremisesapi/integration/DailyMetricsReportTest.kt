package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedAssessedByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationAssessedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.ApplicationSubmittedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeBookedByFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.BookingMadeFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.events.StaffMemberFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.DailyMetricsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.ApprovedPremisesApplicationMetricsSummaryDto
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.DailyMetricReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1ReportService.MonthSpecificReportParams
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toLocalDateTime
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

class DailyMetricsReportTest : IntegrationTestBase() {

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  @Test
  fun `Get daily metrics report for returns 403 Forbidden if user does not have access`() {
    givenAUser(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/dailyMetrics?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @ParameterizedTest
  @EnumSource(ServiceName::class, names = ["approvedPremises"], mode = EnumSource.Mode.EXCLUDE)
  fun `Get daily metrics report for returns not allowed if the service is not Approved Premises`(serviceName: ServiceName) {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      webTestClient.get()
        .uri("/cas1/reports/dailyMetrics?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", serviceName.value)
        .exchange()
        .expectStatus()
        .is4xxClientError
    }
  }

  @Test
  fun `Get daily metrics report for returns a report for the given month`() {
    givenAUser(roles = listOf(UserRole.CAS1_REPORT_VIEWER)) { _, jwt ->
      val month = 4
      val year = 2023

      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(givenAProbationRegion())
      }

      val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val applications = listOf(
        approvedPremisesApplicationEntityFactory.produceAndPersist {
          withCreatedAt(
            LocalDate.of(year, month, 3).toLocalDateTime(),
          )
          withApplicationSchema(applicationSchema)
          withCreatedByUser(user)
        },
      )

      approvedPremisesApplicationEntityFactory.produceAndPersist {
        withCreatedAt(
          LocalDate.of(2023, 5, 1).toLocalDateTime(),
        )
        withApplicationSchema(applicationSchema)
        withCreatedByUser(user)
      }

      val domainEvents = listOf(
        domainEventFactory.produceAndPersist {
          withOccurredAt(
            LocalDate.of(year, month, 1).toLocalDateTime(),
          )
          withType(DomainEventType.APPROVED_PREMISES_APPLICATION_SUBMITTED)
          withData(
            objectMapper.writeValueAsString(
              ApplicationSubmittedEnvelope(
                id = UUID.randomUUID(),
                timestamp = LocalDate.of(year, month, 1).toLocalDateTime().toInstant(),
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
          )
        },
        domainEventFactory.produceAndPersist {
          withOccurredAt(
            LocalDate.of(year, month, 1).toLocalDateTime(),
          )
          withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
          withData(
            objectMapper.writeValueAsString(
              ApplicationAssessedEnvelope(
                id = UUID.randomUUID(),
                timestamp = LocalDate.of(year, month, 1).toLocalDateTime().toInstant(),
                eventType = EventType.applicationSubmitted,
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
                  .withArrivalDate(LocalDate.of(year, month, 1).toLocalDateTime().toInstant())
                  .produce(),
              ),
            ),
          )
        },
        domainEventFactory.produceAndPersist {
          withOccurredAt(
            LocalDate.of(year, month, 1).toLocalDateTime(),
          )
          withType(DomainEventType.APPROVED_PREMISES_BOOKING_MADE)
          withData(
            objectMapper.writeValueAsString(
              BookingMadeEnvelope(
                id = UUID.randomUUID(),
                timestamp = LocalDate.of(year, month, 1).toLocalDateTime().toInstant(),
                eventType = EventType.applicationSubmitted,
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
          )
        },
      )

      domainEventFactory.produceAndPersist {
        withOccurredAt(
          LocalDate.of(2023, 5, 1).toLocalDateTime(),
        )
        withType(DomainEventType.APPROVED_PREMISES_APPLICATION_ASSESSED)
      }

      val startDate = LocalDate.of(year, month, 1)
      val endDate = startDate.with(TemporalAdjusters.lastDayOfMonth())

      val datesForMonth = listOf(
        LocalDate.of(year, month, 1),
        LocalDate.of(year, month, 2),
        LocalDate.of(year, month, 3),
        LocalDate.of(year, month, 4),
        LocalDate.of(year, month, 5),
        LocalDate.of(year, month, 6),
        LocalDate.of(year, month, 7),
        LocalDate.of(year, month, 8),
        LocalDate.of(year, month, 9),
        LocalDate.of(year, month, 10),
        LocalDate.of(year, month, 11),
        LocalDate.of(year, month, 12),
        LocalDate.of(year, month, 13),
        LocalDate.of(year, month, 14),
        LocalDate.of(year, month, 15),
        LocalDate.of(year, month, 16),
        LocalDate.of(year, month, 17),
        LocalDate.of(year, month, 18),
        LocalDate.of(year, month, 19),
        LocalDate.of(year, month, 20),
        LocalDate.of(year, month, 21),
        LocalDate.of(year, month, 22),
        LocalDate.of(year, month, 23),
        LocalDate.of(year, month, 24),
        LocalDate.of(year, month, 25),
        LocalDate.of(year, month, 26),
        LocalDate.of(year, month, 27),
        LocalDate.of(year, month, 28),
        LocalDate.of(year, month, 29),
        LocalDate.of(year, month, 30),
      )

      val expectedApplications = applications.map {
        ApprovedPremisesApplicationMetricsSummaryDto(
          it.createdAt.toLocalDate(),
          it.createdByUser.id.toString(),
        )
      }

      val expectedDataFrame = DailyMetricsReportGenerator(domainEvents, expectedApplications, domainEventService)
        .createReport(
          datesForMonth,
          MonthSpecificReportParams(
            year,
            month,
          ),
        )

      webTestClient.get()
        .uri("/cas1/reports/dailyMetrics?year=$year&month=$month")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader().valuesMatch(
          "content-disposition",
          "attachment; filename=\"daily-metrics-$year-${month.toString().padStart(2, '0')}-[0-9_]+.xlsx\"",
        )
        .expectBody()
        .consumeWith {
          val actual = DataFrame
            .readExcel(it.responseBody!!.inputStream())
            .convertTo<DailyMetricReportRow>(ExcessiveColumns.Remove)

          assertThat(actual).isEqualTo(expectedDataFrame)
        }
    }
  }
}
