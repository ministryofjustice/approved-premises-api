package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPersonSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResultRoomSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.asOffenderDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.community.OffenderDetailSummary
import java.time.LocalDate

class BookingSearchTest : IntegrationTestBase() {
  @Test
  fun `Searching for bookings without JWT returns 401`() {
    webTestClient.get()
      .uri("/bookings/search")
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `Searching for Approved Premises bookings returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = createApprovedPremisesBookingEntities(userEntity, offenderDetails.case.asOffenderDetail())
        create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Approved Premises bookings with pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = createApprovedPremisesBookingEntities(userEntity, offenderDetails.case.asOffenderDetail())
        create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        allBookings.sortBy { it.createdAt }
        val firstPage = allBookings.subList(0, 10)
        val secondPage = allBookings.subList(10, allBookings.size)
        val expectedFirstPageResponse = getExpectedResponse(firstPage, offenderDetails.case.asOffenderDetail())
        val expectedSecondPageResponse = getExpectedResponse(secondPage, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedFirstPageResponse))

        webTestClient.get()
          .uri("/bookings/search?page=2")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.approvedPremises.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedSecondPageResponse))
      }
    }
  }

  @Test
  fun `Searching for Temporary Accommodation bookings returns 200 with correct body`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create15TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Results are filtered by booking status when query parameter is supplied`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationRegion(userEntity.probationRegion)
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val allBeds = mutableListOf<BedEntity>()
        allPremises.forEach { premises ->
          val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }

          rooms.forEach { room ->
            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            allBeds += bed
          }
        }

        val allBookings = mutableListOf<BookingEntity>()
        allBeds.forEachIndexed { index, bed ->
          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(bed.room.premises)
            withCrn(offenderDetails.case.crn)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          when (index % 5) {
            // Provisional
            0 -> {}
            // Confirmed
            1 -> {
              val confirmation = confirmationEntityFactory.produceAndPersist {
                withBooking(booking)
              }

              booking.confirmation = confirmation
            }
            // Active
            2 -> {
              val arrival = arrivalEntityFactory.produceAndPersist {
                withBooking(booking)
              }

              booking.arrival = arrival
            }
            // Closed
            3 -> {
              val departure = departureEntityFactory.produceAndPersist {
                withBooking(booking)
                withYieldedReason {
                  departureReasonEntityFactory.produceAndPersist()
                }
                withYieldedMoveOnCategory {
                  moveOnCategoryEntityFactory.produceAndPersist()
                }
              }

              booking.departures.add(departure)
            }
            // Cancelled
            4 -> {
              val cancellation = cancellationEntityFactory.produceAndPersist {
                withBooking(booking)
                withYieldedReason {
                  cancellationReasonEntityFactory.produceAndPersist()
                }
              }

              booking.cancellations.add(cancellation)
            }
          }

          allBookings += booking
        }

        val expectedBookings = allBookings.filter { it.cancellation != null }

        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?status=cancelled")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Results are ordered by the given field and sort order when the query parameters are supplied`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val expectedBookings = allBookings.sortedByDescending { it.departureDate }
        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Results are only returned for the user's probation region for Temporary Accommodation`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val expectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withProbationRegion(userEntity.probationRegion)
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val unexpectedPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(5) {
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist {
              withYieldedApArea {
                apAreaEntityFactory.produceAndPersist()
              }
            }
          }
          withYieldedLocalAuthorityArea {
            localAuthorityEntityFactory.produceAndPersist()
          }
        }

        val allPremises = expectedPremises + unexpectedPremises

        val allBeds = mutableListOf<BedEntity>()
        allPremises.forEach { premises ->
          val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
            withPremises(premises)
          }

          rooms.forEach { room ->
            val bed = bedEntityFactory.produceAndPersist {
              withRoom(room)
            }

            allBeds += bed
          }
        }

        val allBookings = mutableListOf<BookingEntity>()
        allBeds.forEach { bed ->
          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(bed.room.premises)
            withCrn(offenderDetails.case.crn)
            withBed(bed)
            withServiceName(ServiceName.temporaryAccommodation)
          }

          allBookings += booking
        }

        val expectedPremisesIds = expectedPremises.map { it.id }
        val expectedBookings = allBookings.filter { expectedPremisesIds.contains(it.premises.id) }

        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse))
      }
    }
  }

  @Test
  fun `Searching for Temporary Accommodation bookings with pagination with pagination returns 200 with correct subset of results`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        createApprovedPremisesBookingEntities(userEntity, offenderDetails.case.asOffenderDetail())
        val allBookings = create15TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val sortedBookings = allBookings.sortedByDescending { it.departureDate }
        val firstPage = sortedBookings.subList(0, 10)
        val secondPage = sortedBookings.subList(10, sortedBookings.size)
        val expectedFirstPageResponse = getExpectedResponse(firstPage, offenderDetails.case.asOffenderDetail())
        val expectedSecondPageResponse = getExpectedResponse(secondPage, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedFirstPageResponse))

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=2")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 2)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 2)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 15)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedSecondPageResponse))
      }
    }
  }

  @Test
  fun `Results are ordered by the created date and sorted descending order when the query parameters are supplied with Pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val expectedBookings = allBookings.sortedByDescending { it.createdAt }
        val expectedResponse = getExpectedResponse(expectedBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=createdAt&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 10)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Results are ordered by the created date and sorted ascending order when the query parameters are supplied with Pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        allBookings.sortBy { it.createdAt }
        val expectedResponse = getExpectedResponse(allBookings, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=ascending&sortField=createdAt&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 10)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Results are ordered by the start date and sorted descending order when the query parameters are supplied with Pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val sortedByDescending = allBookings.sortedByDescending { it.arrivalDate }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails.case.asOffenderDetail())
        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=startDate&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 10)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Results are ordered by the end date and sorted descending order when the query parameters are supplied with Pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val sortedByDescending = allBookings.sortedByDescending { it.departureDate }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=endDate&page=1")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 10)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Results are ordered by the person crn and sorted descending order when the query parameters are supplied with Pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val sortedByDescending = allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=crn&page=1&status=provisional")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 1)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 10)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `Get all results ordered by the person crn in descending order when the query parameters supplied without Pagination`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create15TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        val sortedByDescending = allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(sortedByDescending, offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=crn&status=provisional")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  @Test
  fun `No Results returned when searching for cancelled booking status and all existing bookings are confirmed`() {
    `Given a User` { userEntity, jwt ->
      `Given an Offender` { offenderDetails, _ ->
        val allBookings = create10TestTemporaryAccommodationBookings(userEntity, offenderDetails.case.asOffenderDetail())
        allBookings.sortedByDescending { it.crn }
        val expectedResponse = getExpectedResponse(emptyList(), offenderDetails.case.asOffenderDetail())

        webTestClient.get()
          .uri("/bookings/search?sortOrder=descending&sortField=crn&page=1&status=cancelled")
          .header("Authorization", "Bearer $jwt")
          .header("X-Service-Name", ServiceName.temporaryAccommodation.value)
          .exchange()
          .expectStatus()
          .isOk
          .expectHeader().valueEquals("X-Pagination-CurrentPage", 1)
          .expectHeader().valueEquals("X-Pagination-TotalPages", 0)
          .expectHeader().valueEquals("X-Pagination-TotalResults", 0)
          .expectHeader().valueEquals("X-Pagination-PageSize", 10)
          .expectBody()
          .json(objectMapper.writeValueAsString(expectedResponse), true)
      }
    }
  }

  private fun getExpectedResponse(expectedBookings: List<BookingEntity>, offenderDetails: OffenderDetailSummary): BookingSearchResults {
    return BookingSearchResults(
      resultsCount = expectedBookings.size,
      results = expectedBookings.map { booking ->
        BookingSearchResult(
          person = BookingSearchResultPersonSummary(
            name = "${offenderDetails.firstName} ${offenderDetails.surname}",
            crn = offenderDetails.otherIds.crn,
          ),
          booking = BookingSearchResultBookingSummary(
            id = booking.id,
            status = when {
              booking.cancellation != null -> BookingStatus.cancelled
              booking.departure != null -> BookingStatus.departed
              booking.arrival != null -> BookingStatus.arrived
              booking.nonArrival != null -> BookingStatus.notMinusArrived
              booking.confirmation != null -> BookingStatus.confirmed
              booking.service == ServiceName.approvedPremises.value -> BookingStatus.awaitingMinusArrival
              else -> BookingStatus.provisional
            },
            startDate = booking.arrivalDate,
            endDate = booking.departureDate,
            createdAt = booking.createdAt.toInstant(),
          ),
          premises = BookingSearchResultPremisesSummary(
            id = booking.premises.id,
            name = booking.premises.name,
            addressLine1 = booking.premises.addressLine1,
            addressLine2 = booking.premises.addressLine2,
            town = booking.premises.town,
            postcode = booking.premises.postcode,
          ),
          room = BookingSearchResultRoomSummary(
            id = booking.bed!!.room.id,
            name = booking.bed!!.room.name,
          ),
          bed = BookingSearchResultBedSummary(
            id = booking.bed!!.id,
            name = booking.bed!!.name,
          ),
        )
      },
    )
  }

  private fun create10TestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): MutableList<BookingEntity> {
    return createTestTemporaryAccommodationBookings(userEntity, offenderDetails, 5, 2)
  }

  private fun create15TestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): MutableList<BookingEntity> {
    return createTestTemporaryAccommodationBookings(userEntity, offenderDetails, 5, 3)
  }

  private fun createTestTemporaryAccommodationBookings(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
    numberOfPremises: Int,
    numberOfBedsInEachPremises: Int,
  ): MutableList<BookingEntity> {
    val allPremises = temporaryAccommodationPremisesEntityFactory.produceAndPersistMultiple(numberOfPremises) {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val allBeds = mutableListOf<BedEntity>()
    allPremises.forEach { premises ->
      val rooms = roomEntityFactory.produceAndPersistMultiple(numberOfBedsInEachPremises) {
        withPremises(premises)
      }

      rooms.forEach { room ->
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        allBeds += bed
      }
    }

    val allBookings = mutableListOf<BookingEntity>()
    allBeds.forEachIndexed { index, bed ->
      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(bed.room.premises)
        withCrn(offenderDetails.otherIds.crn)
        withBed(bed)
        withServiceName(ServiceName.temporaryAccommodation)
        withArrivalDate(LocalDate.now().minusDays((60 - index).toLong()))
        withDepartureDate(LocalDate.now().minusDays((30 - index).toLong()))
      }

      allBookings += booking
    }
    return allBookings
  }

  private fun createApprovedPremisesBookingEntities(
    userEntity: UserEntity,
    offenderDetails: OffenderDetailSummary,
  ): MutableList<BookingEntity> {
    val allPremises = approvedPremisesEntityFactory.produceAndPersistMultiple(5) {
      withProbationRegion(userEntity.probationRegion)
      withYieldedLocalAuthorityArea {
        localAuthorityEntityFactory.produceAndPersist()
      }
    }

    val allBeds = mutableListOf<BedEntity>()
    allPremises.forEach { premises ->
      val rooms = roomEntityFactory.produceAndPersistMultiple(3) {
        withPremises(premises)
      }

      rooms.forEach { room ->
        val bed = bedEntityFactory.produceAndPersist {
          withRoom(room)
        }

        allBeds += bed
      }
    }

    val allBookings = mutableListOf<BookingEntity>()
    allBeds.forEach { bed ->
      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(bed.room.premises)
        withCrn(offenderDetails.otherIds.crn)
        withBed(bed)
        withServiceName(ServiceName.approvedPremises)
      }

      allBookings += booking
    }
    return allBookings
  }
}
