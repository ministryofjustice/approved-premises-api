package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.ExcessiveColumns
import org.jetbrains.kotlinx.dataframe.api.convertTo
import org.jetbrains.kotlinx.dataframe.io.readExcel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LostBedsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUsageReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BedUtilisationReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.generator.BookingsReportGenerator
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUsageReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BedUtilisationReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.model.BookingsReportRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUsageReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BedUtilisationReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.reporting.properties.BookingsReportProperties
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.WorkingDayCountService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.time.LocalDate
import java.util.UUID

class ReportsTest : IntegrationTestBase() {
  @Autowired
  lateinit var bookingTransformer: BookingTransformer

  @Autowired
  lateinit var realBookingRepository: BookingRepository

  @Autowired
  lateinit var realLostBedsRepository: LostBedsRepository

  @Autowired
  lateinit var realWorkingDayCountService: WorkingDayCountService

  @Test
  fun `Get bookings report for all regions returns 403 Forbidden if user does not have all regions access`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?year=2023&month=4")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bookings report for a region returns 403 Forbidden if user cannot access the specified region`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bookings report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bookings report returns 400 if month is provided and not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/reports/bookings?probationRegionId=${user.probationRegion.id}&year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get bookings report returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(userEntity.probationRegion)
        }

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 4, 5))
          withDepartureDate(LocalDate.of(2023, 4, 7))
        }

        bookings[1].let { it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) } }
        bookings[2].let {
          it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) }
          it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        val expectedDataFrame = BookingsReportGenerator()
          .createReport(bookings, BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4))

        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bookings report returns OK for CAS3_REPORTER`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(userEntity.probationRegion)
        }

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 4, 5))
          withDepartureDate(LocalDate.of(2023, 4, 7))
        }

        bookings[1].let { it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) } }
        bookings[2].let {
          it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) }
          it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        val expectedDataFrame = BookingsReportGenerator()
          .createReport(bookings, BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4))

        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bookings report returns OK for CAS3_REPORTER for all region`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(userEntity.probationRegion)
        }

        val bookings = bookingEntityFactory.produceAndPersistMultiple(5) {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 4, 5))
          withDepartureDate(LocalDate.of(2023, 4, 7))
        }

        bookings[1].let { it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) } }
        bookings[2].let {
          it.arrival = arrivalEntityFactory.produceAndPersist { withBooking(it) }
          it.extensions = extensionEntityFactory.produceAndPersistMultiple(1) { withBooking(it) }.toMutableList()
          it.departures = departureEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedDestinationProvider { destinationProviderEntityFactory.produceAndPersist() }
            withYieldedReason { departureReasonEntityFactory.produceAndPersist() }
            withYieldedMoveOnCategory { moveOnCategoryEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[3].let {
          it.cancellations = cancellationEntityFactory.produceAndPersistMultiple(1) {
            withBooking(it)
            withYieldedReason { cancellationReasonEntityFactory.produceAndPersist() }
          }.toMutableList()
        }
        bookings[4].let {
          it.nonArrival = nonArrivalEntityFactory.produceAndPersist {
            withBooking(it)
            withYieldedReason { nonArrivalReasonEntityFactory.produceAndPersist() }
          }
        }

        val expectedDataFrame = BookingsReportGenerator()
          .createReport(bookings, BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4))

        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bookings report returns 403 Forbidden for CAS3_REPORTER with service-name as approved-premises`() {
    `Given a User`(roles = listOf(UserRole.CAS3_REPORTER)) { userEntity, jwt ->

      webTestClient.get()
        .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.approvedPremises.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bookings report returns OK with only Bookings with at least one day in month when year and month are specified`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(userEntity.probationRegion)
        }

        val shouldNotBeIncludedBookings = mutableListOf<BookingEntity>()
        val shouldBeIncludedBookings = mutableListOf<BookingEntity>()

        // Straddling start of month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 3, 29))
          withDepartureDate(LocalDate.of(2023, 4, 1))
        }

        // Straddling end of month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 4, 2))
          withDepartureDate(LocalDate.of(2023, 4, 3))
        }

        // Entirely within month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 4, 30))
          withDepartureDate(LocalDate.of(2023, 5, 15))
        }

        // Encompassing month
        shouldBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 3, 28))
          withDepartureDate(LocalDate.of(2023, 5, 28))
        }

        // Before month
        shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 3, 28))
          withDepartureDate(LocalDate.of(2023, 3, 30))
        }

        // After month
        shouldNotBeIncludedBookings += bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.of(2023, 5, 1))
          withDepartureDate(LocalDate.of(2023, 5, 3))
        }

        val expectedDataFrame = BookingsReportGenerator()
          .createReport(shouldBeIncludedBookings, BookingsReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4))

        webTestClient.get()
          .uri("/reports/bookings?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BookingsReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bed usage report for all regions returns 403 Forbidden if user does not have all regions access`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/bed-usage?year=2023&month=4&")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bed usage report for a region returns 403 Forbidden if user cannot access the specified region`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bed usage report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bed usage report returns 400 if month is not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/reports/bed-usage?probationRegionId=${user.probationRegion.id}&year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get bed usage report returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(userEntity.probationRegion)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.parse("2023-04-05"))
          withDepartureDate(LocalDate.parse("2023-04-15"))
        }

        val expectedDataFrame = BedUsageReportGenerator(
          bookingTransformer,
          realBookingRepository,
          realLostBedsRepository,
          realWorkingDayCountService,
        )
          .createReport(listOf(bed), BedUsageReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4))

        webTestClient.get()
          .uri("/reports/bed-usage?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedUsageReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }

  @Test
  fun `Get bed utilisation report for all regions returns 403 Forbidden if user does not have all regions access`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/bed-utilisation?year=2023&month=4&")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bed utilisation report for a region returns 403 Forbidden if user cannot access the specified region`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { _, jwt ->
      webTestClient.get()
        .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${UUID.randomUUID()}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bed utilisation report returns 403 Forbidden for Temporary Accommodation if a user does not have the CAS3_ASSESSOR role`() {
    `Given a User` { user, jwt ->
      webTestClient.get()
        .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${user.probationRegion.id}")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isForbidden
    }
  }

  @Test
  fun `Get bed utilisation report returns 400 if month is not within 1-12`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { user, jwt ->
      webTestClient.get()
        .uri("/reports/bed-utilisation?probationRegionId=${user.probationRegion.id}&year=2023&month=-1")
        .header("Authorization", "Bearer $jwt")
        .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("$.detail").isEqualTo("month must be between 1 and 12")
    }
  }

  @Test
  fun `Get bed utilisation report returns OK with correct body`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      `Given an Offender` { offenderDetails, inmateDetails ->
        val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withProbationRegion(userEntity.probationRegion)
        }

        val room = roomEntityFactory.produceAndPersist {
          withPremises(premises)
        }

        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        GovUKBankHolidaysAPI_mockSuccessfullCallWithEmptyResponse()

        bookingEntityFactory.produceAndPersist {
          withPremises(premises)
          withBed(bed)
          withServiceName(ServiceName.temporaryAccommodation)
          withCrn(offenderDetails.case.crn)
          withArrivalDate(LocalDate.parse("2023-04-05"))
          withDepartureDate(LocalDate.parse("2023-04-15"))
        }

        val expectedDataFrame = BedUtilisationReportGenerator(
          realBookingRepository,
          realLostBedsRepository,
          realWorkingDayCountService,
        )
          .createReport(listOf(bed), BedUtilisationReportProperties(ServiceName.temporaryAccommodation, null, 2023, 4))

        webTestClient.get()
          .uri("/reports/bed-utilisation?year=2023&month=4&probationRegionId=${userEntity.probationRegion.id}")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .consumeWith {
            val actual = DataFrame
              .readExcel(it.responseBody!!.inputStream())
              .convertTo<BedUtilisationReportRow>(ExcessiveColumns.Remove)
            Assertions.assertThat(actual).isEqualTo(expectedDataFrame)
          }
      }
    }
  }
}
