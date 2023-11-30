package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.reporting.generator

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.count
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProfileFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonSummaryInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RiskWithStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.RoshRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.toBookingsReportDataAndPersonInfo
import java.time.LocalDate
import java.time.OffsetDateTime

class BookingsReportGeneratorTest {
  private val reportGenerator = BookingsReportGenerator()

  @Test
  fun `The probation region name is returned in the report`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withName("East of England")
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withYieldedLocalAuthorityArea {
            LocalAuthorityEntityFactory()
              .withName("St Albans")
              .produce()
          }
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::probationRegion]).isEqualTo("East of England")
    assertThat(actual[0][BookingsReportRow::localAuthority]).isEqualTo("St Albans")
  }

  @Test
  fun `The CRN is returned in the report`() {
    val booking = BookingEntityFactory()
      .withCrn("X123456")
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::crn]).isEqualTo("X123456")
  }

  @Test
  fun `The 'offer accepted' column is true in the report if the booking was confirmed`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.confirmation = ConfirmationEntityFactory()
      .withBooking(booking)
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::offerAccepted]).isTrue
  }

  @Test
  fun `The 'offer accepted' column is false in the report if the booking was not confirmed`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::offerAccepted]).isFalse
  }

  @Test
  fun `The 'is cancelled' column is true and the cancellation reason is returned in the report if the booking was cancelled`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.cancellations = mutableListOf(
      CancellationEntityFactory()
        .withYieldedReason {
          CancellationReasonEntityFactory()
            .withName("House exploded")
            .produce()
        }
        .withBooking(booking)
        .produce(),
    )

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::isCancelled]).isTrue
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::cancellationReason]).isEqualTo("House exploded")
  }

  @Test
  fun `The 'is cancelled' column is false and no cancellation reason is returned in the report if the booking was not cancelled`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::isCancelled]).isFalse
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::cancellationReason]).isNull()
  }

  @Test
  fun `The start and end dates are returned from the arrival in the report if it exists`() {
    val today = LocalDate.now()
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(today)
      .withExpectedDepartureDate(today.plusDays(84L))
      .withBooking(booking)
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::startDate]).isEqualTo(today)
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::endDate]).isEqualTo(today.plusDays(84L))
  }

  @Test
  fun `The start and end dates are not returned in the report if the booking has no arrival`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::startDate]).isNull()
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::endDate]).isNull()
  }

  @Test
  fun `The actual end date is returned in the report if the booking has a departure`() {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withDateTime(now)
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce(),
    )

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualEndDate]).isEqualTo(today)
  }

  @Test
  fun `The actual end date is not returned in the report if the booking has no departure`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualEndDate]).isNull()
  }

  @Test
  fun `The number of nights stayed so far is returned from the arrival in the report if it exists`() {
    val today = LocalDate.now()
    val arrivalDate = today.minusDays(37L)

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(arrivalDate)
      .withExpectedDepartureDate(arrivalDate.plusDays(84L))
      .withBooking(booking)
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::currentNightsStayed]).isEqualTo(37)
  }

  @Test
  fun `The number of nights stayed so far is not returned in the report if the booking has no arrival`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::currentNightsStayed]).isNull()
  }

  @Test
  fun `The number of nights stayed so far is not returned in the report if the booking has a departure`() {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()
    val expectedDepartureDate = today.minusDays(3L)
    val arrivalDate = expectedDepartureDate.minusDays(84L)

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(arrivalDate)
      .withExpectedDepartureDate(expectedDepartureDate)
      .withBooking(booking)
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withDateTime(now)
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce(),
    )

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::currentNightsStayed]).isNull()
  }

  @Test
  fun `The actual number of nights stayed is returned in the report if the booking has a departure`() {
    val now = OffsetDateTime.now()
    val today = now.toLocalDate()
    val expectedDepartureDate = today.minusDays(3L)
    val arrivalDate = expectedDepartureDate.minusDays(84L)

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.arrival = ArrivalEntityFactory()
      .withArrivalDate(arrivalDate)
      .withExpectedDepartureDate(expectedDepartureDate)
      .withBooking(booking)
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withDateTime(now)
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce(),
    )

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualNightsStayed]).isEqualTo(86L)
  }

  @Test
  fun `The actual number of nights stayed is returned in the report if the booking has no departure`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.approvedPremises)
      .withYieldedPremises {
        ApprovedPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.approvedPremises, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::actualNightsStayed]).isNull()
  }

  @Test
  fun `The accommodation outcome is returned from the departure in the report if it exists`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    booking.departures = mutableListOf(
      DepartureEntityFactory()
        .withYieldedReason {
          DepartureReasonEntityFactory()
            .produce()
        }
        .withYieldedMoveOnCategory {
          MoveOnCategoryEntityFactory()
            .withName("Joined the Space Force")
            .produce()
        }
        .withYieldedDestinationProvider {
          DestinationProviderEntityFactory()
            .produce()
        }
        .withBooking(booking)
        .produce(),
    )

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::accommodationOutcome]).isEqualTo("Joined the Space Force")
  }

  @Test
  fun `The accommodation outcome is not returned in the report if the booking has no departure`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::accommodationOutcome]).isNull()
  }

  @Test
  fun `The referral columns are empty if there is no application for the booking`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::referralId]).isNull()
    assertThat(actual[0][BookingsReportRow::referralDate]).isNull()
    assertThat(actual[0][BookingsReportRow::riskOfSeriousHarm]).isNull()
    assertThat(actual[0][BookingsReportRow::sexOffender]).isNull()
    assertThat(actual[0][BookingsReportRow::needForAccessibleProperty]).isNull()
    assertThat(actual[0][BookingsReportRow::historyOfArsonOffence]).isNull()
    assertThat(actual[0][BookingsReportRow::dutyToReferMade]).isNull()
    assertThat(actual[0][BookingsReportRow::dateDutyToReferMade]).isNull()
    assertThat(actual[0][BookingsReportRow::isReferralEligibleForCas3]).isNull()
    assertThat(actual[0][BookingsReportRow::referralEligibilityReason]).isNull()
  }

  @Test
  fun `The referral columns are returned in the report when there is an application for the booking`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce()
      }
      .withProbationRegion(probationRegion)
      .withSubmittedAt(OffsetDateTime.now())
      .withRiskRatings {
        withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
      }
      .withIsRegisteredSexOffender(true)
      .withNeedsAccessibleProperty(true)
      .withHasHistoryOfArson(false)
      .withIsDutyToReferSubmitted(true)
      .withDutyToReferSubmissionDate(LocalDate.now())
      .withIsEligible(true)
      .withEligiblilityReason("Some reason")
      .withData(
        """
               {
                "accommodation-referral-details": {
                    "dtr-submitted": {
                      "dtrSubmitted": "yes"
                    },
                    "dtr-details": {
                      "reference": "1",
                      "localAuthorityAreaName": "Aberdeen City",
                      "date-year": "2023",
                      "date-month": "2",
                      "date-day": "1",
                      "date": "2023-02-01"
                    },
                    "crs-submitted": {
                      "crsSubmitted": "yes"
                    },
                    "other-accommodation-options": {
                      "otherOptions": "no",
                      "otherOptionsDetail": ""
                  }
                }
              }
            """,
      )
      .produce()

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withProbationRegion(probationRegion)
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .withApplication(application)
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::referralId]).isEqualTo(application.id.toString())
    assertThat(actual[0][BookingsReportRow::referralDate]).isEqualTo(application.submittedAt!!.toLocalDate())
    assertThat(actual[0][BookingsReportRow::riskOfSeriousHarm]).isEqualTo("High")
    assertThat(actual[0][BookingsReportRow::sexOffender]).isTrue
    assertThat(actual[0][BookingsReportRow::needForAccessibleProperty]).isTrue
    assertThat(actual[0][BookingsReportRow::historyOfArsonOffence]).isFalse
    assertThat(actual[0][BookingsReportRow::dutyToReferMade]).isTrue
    assertThat(actual[0][BookingsReportRow::dateDutyToReferMade]).isEqualTo(LocalDate.now())
    assertThat(actual[0][BookingsReportRow::isReferralEligibleForCas3]).isTrue
    assertThat(actual[0][BookingsReportRow::referralEligibilityReason]).isEqualTo("Some reason")
    assertThat(actual[0][BookingsReportRow::dutyToReferLocalAuthorityAreaName]).isEqualTo("Aberdeen City")
  }

  @Test
  fun `The referral columns are returned in the report when the application don't have dtr details`() {
    val probationRegion = ProbationRegionEntityFactory()
      .withYieldedApArea { ApAreaEntityFactory().produce() }
      .produce()

    val application = TemporaryAccommodationApplicationEntityFactory()
      .withYieldedCreatedByUser {
        UserEntityFactory()
          .withProbationRegion(probationRegion)
          .produce()
      }
      .withProbationRegion(probationRegion)
      .withSubmittedAt(OffsetDateTime.now())
      .withRiskRatings {
        withRoshRisks(
          RiskWithStatus(
            value = RoshRisks(
              overallRisk = "High",
              riskToChildren = "Medium",
              riskToPublic = "Low",
              riskToKnownAdult = "High",
              riskToStaff = "High",
              lastUpdated = null,
            ),
          ),
        )
      }
      .withIsRegisteredSexOffender(true)
      .withNeedsAccessibleProperty(true)
      .withHasHistoryOfArson(false)
      .withIsDutyToReferSubmitted(true)
      .withDutyToReferSubmissionDate(LocalDate.now())
      .withIsEligible(true)
      .withEligiblilityReason("Some reason")
      .withData(
        """
             {
              "sentence-information": {
                    "offending-summary": {
                      "summary": "s"
                    },
                    "sentence-type": {
                      "sentenceType": "standardDeterminate"
                    },
                    "sentence-length": {
                      "years": "",
                      "months": "1",
                      "weeks": "",
                      "days": ""
                    },
                    "sentence-expiry": {
                      "sentenceExpiryDate": "2024-01-01",
                      "sentenceExpiryDate-year": "2024",
                      "sentenceExpiryDate-month": "1",
                      "sentenceExpiryDate-day": "1"
                    },
                    "release-type": {
                      "releaseTypes": [
                        "licence"
                      ],
                      "licenceStartDate": "2024-01-01",
                      "licenceStartDate-year": "2024",
                      "licenceStartDate-month": "1",
                      "licenceStartDate-day": "1",
                      "licenceEndDate": "2024-01-10",
                      "licenceEndDate-year": "2024",
                      "licenceEndDate-month": "1",
                      "licenceEndDate-day": "10",
                      "pssStartDate-year": "",
                      "pssStartDate-month": "",
                      "pssStartDate-day": "",
                      "pssEndDate-year": "",
                      "pssEndDate-month": "",
                      "pssEndDate-day": ""
                    }
              }
             }
          """,
      )
      .produce()

    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withProbationRegion(probationRegion)
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .withApplication(application)
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::referralId]).isEqualTo(application.id.toString())
    assertThat(actual[0][BookingsReportRow::referralDate]).isEqualTo(application.submittedAt!!.toLocalDate())
    assertThat(actual[0][BookingsReportRow::riskOfSeriousHarm]).isEqualTo("High")
    assertThat(actual[0][BookingsReportRow::sexOffender]).isTrue
    assertThat(actual[0][BookingsReportRow::needForAccessibleProperty]).isTrue
    assertThat(actual[0][BookingsReportRow::historyOfArsonOffence]).isFalse
    assertThat(actual[0][BookingsReportRow::dutyToReferMade]).isTrue
    assertThat(actual[0][BookingsReportRow::dateDutyToReferMade]).isEqualTo(LocalDate.now())
    assertThat(actual[0][BookingsReportRow::isReferralEligibleForCas3]).isTrue
    assertThat(actual[0][BookingsReportRow::referralEligibilityReason]).isEqualTo("Some reason")
    assertThat(actual[0][BookingsReportRow::dutyToReferLocalAuthorityAreaName]).isNull()
  }

  @Test
  fun `The personal details columns are empty if there is no person information available for the booking`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo(),
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::personName]).isNull()
    assertThat(actual[0][BookingsReportRow::pncNumber]).isNull()
    assertThat(actual[0][BookingsReportRow::gender]).isNull()
    assertThat(actual[0][BookingsReportRow::ethnicity]).isNull()
    assertThat(actual[0][BookingsReportRow::dateOfBirth]).isNull()
  }

  @Test
  fun `The personal details columns are returned when there is person information available for the booking`() {
    val booking = BookingEntityFactory()
      .withServiceName(ServiceName.temporaryAccommodation)
      .withYieldedPremises {
        TemporaryAccommodationPremisesEntityFactory()
          .withYieldedProbationRegion {
            ProbationRegionEntityFactory()
              .withYieldedApArea { ApAreaEntityFactory().produce() }
              .produce()
          }
          .withLocalAuthorityArea(LocalAuthorityEntityFactory().produce())
          .produce()
      }
      .produce()

    val pnc = "PNC Number"
    val caseSummary = CaseSummaryFactory()
      .withName(
        NameFactory()
          .withForename("Johannes")
          .withMiddleNames(listOf())
          .withSurname("Kepler")
          .produce(),
      )
      .withGender("Male")
      .withProfile(
        ProfileFactory()
          .withEthnicity("Other White")
          .produce(),
      )
      .withDateOfBirth(LocalDate.parse("1571-12-27"))
      .withPnc(pnc)
      .produce()

    val actual = reportGenerator.createReport(
      listOf(booking).toBookingsReportDataAndPersonInfo { crn ->
        PersonSummaryInfoResult.Success.Full(crn, caseSummary)
      },
      BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4),
    )
    assertThat(actual.count()).isEqualTo(1)
    assertThat(actual[0][BookingsReportRow::personName]).isEqualTo("Johannes Kepler")
    assertThat(actual[0][BookingsReportRow::pncNumber]).isEqualTo(pnc)
    assertThat(actual[0][BookingsReportRow::gender]).isEqualTo("Male")
    assertThat(actual[0][BookingsReportRow::ethnicity]).isEqualTo("Other White")
    assertThat(actual[0][BookingsReportRow::dateOfBirth]).isEqualTo("1571-12-27")
  }
}
