package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import java.time.LocalDate
import java.util.UUID

class TurnaroundTest : InitialiseDatabasePerClassTestBase() {
  @Test
  fun `Create Turnaround returns 404 Not Found if the premises was not found`() {
    `Given a User` { _, jwt ->
      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/premises/${UUID.randomUUID()}/bookings/${UUID.randomUUID()}/turnarounds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTurnaround(
            workingDays = 2,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Create Turnaround returns 404 Not Found if the booking was not found on the premises`() {
    `Given a User` { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(userEntity.probationRegion)
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
      }

      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${UUID.randomUUID()}/turnarounds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTurnaround(
            workingDays = 2,
          ),
        )
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Test
  fun `Create Turnaround returns 400 Bad Request if the number of working days is not a positive integer`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(userEntity.probationRegion)
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
      }

      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/turnarounds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTurnaround(
            workingDays = 0,
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody()
        .jsonPath("invalid-params[0].propertyName").isEqualTo("$.workingDays")
        .jsonPath("invalid-params[0].errorType").isEqualTo("isNotAPositiveInteger")
    }
  }

  @Test
  fun `Create Turnaround returns 409 Conflict if the turnaround overlaps with an existing booking`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(userEntity.probationRegion)
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.of(2023, 2, 3))
        withDepartureDate(LocalDate.of(2023, 5, 3))
      }

      val conflictingBooking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.of(2023, 5, 5))
        withDepartureDate(LocalDate.of(2023, 8, 5))
      }

      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/turnarounds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTurnaround(
            workingDays = 2,
          ),
        )
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.CONFLICT)
        .expectBody()
        .jsonPath("title").isEqualTo("Conflict")
        .jsonPath("status").isEqualTo(409)
        .jsonPath("detail").isEqualTo("A Booking already exists for dates from 2023-05-05 to 2023-08-05 which overlaps with the desired dates: ${conflictingBooking.id}")
    }
  }

  @Test
  fun `Create Turnaround returns 409 Conflict if the turnaround overlaps with an existing lost bed`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(userEntity.probationRegion)
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withArrivalDate(LocalDate.of(2023, 2, 3))
        withDepartureDate(LocalDate.of(2023, 5, 3))
      }

      val conflictingLostBed = lostBedsEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
        withStartDate(LocalDate.of(2023, 5, 5))
        withEndDate(LocalDate.of(2023, 5, 19))
        withYieldedReason {
          lostBedReasonEntityFactory.produceAndPersist()
        }
      }

      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/turnarounds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTurnaround(
            workingDays = 2,
          ),
        )
        .exchange()
        .expectStatus()
        .isEqualTo(HttpStatus.CONFLICT)
        .expectBody()
        .jsonPath("title").isEqualTo("Conflict")
        .jsonPath("status").isEqualTo(409)
        .jsonPath("detail").isEqualTo("A Lost Bed already exists for dates from 2023-05-05 to 2023-05-19 which overlaps with the desired dates: ${conflictingLostBed.id}")
    }
  }

  @Test
  fun `Create Turnaround returns 200 OK with the created turnaround`() {
    `Given a User`(roles = listOf(UserRole.CAS3_ASSESSOR)) { userEntity, jwt ->
      val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
        withProbationRegion(userEntity.probationRegion)
        withYieldedLocalAuthorityArea {
          localAuthorityEntityFactory.produceAndPersist()
        }
      }

      val room = roomEntityFactory.produceAndPersist {
        withPremises(premises)
      }

      val bed = bedEntityFactory.produceAndPersist {
        withRoom(room)
      }

      val booking = bookingEntityFactory.produceAndPersist {
        withPremises(premises)
        withBed(bed)
      }

      govUKBankHolidaysApiMockSuccessfullCallWithEmptyResponse()

      webTestClient.post()
        .uri("/premises/${premises.id}/bookings/${booking.id}/turnarounds")
        .header("Authorization", "Bearer $jwt")
        .bodyValue(
          NewTurnaround(
            workingDays = 2,
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(booking.id.toString())
        .jsonPath("$.workingDays").isEqualTo(2)
    }
  }
}
