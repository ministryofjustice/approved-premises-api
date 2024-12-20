package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens

import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomDateBefore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenABooking(
  crn: String,
  application: ApplicationEntity,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureReason: DepartureReasonEntity? = null,
  departureNotes: String? = null,
  departureMoveOnCategory: MoveOnCategoryEntity? = null,
  nonArrivalReason: NonArrivalReasonEntity? = null,
  nonArrivalConfirmedAt: OffsetDateTime? = null,
  nonArrivalNotes: String? = null,
  adhoc: Boolean = false,
  actualArrivalDate: LocalDateTime? = null,
  actualDepartureDate: LocalDateTime? = null,
): BookingEntity {
  val booking = bookingEntityFactory.produceAndPersist {
    withCrn(crn)
    withPremises(premises ?: approvedPremisesEntityFactory.produceAndPersist())
    withApplication(application)
    withArrivalDate(arrivalDate)
    withDepartureDate(departureDate)
    withAdhoc(adhoc)
  }

  actualArrivalDate?.let {
    booking.arrivals.add(
      arrivalEntityFactory.produceAndPersist {
        withBooking(booking)
        withArrivalDate(actualArrivalDate.toLocalDate())
        withArrivalDateTime(actualArrivalDate.toInstant(ZoneOffset.UTC))
      },
    )
  }

  actualDepartureDate?.let {
    booking.departures.add(
      departureEntityFactory.produceAndPersist {
        withBooking(booking)
        withDateTime(actualDepartureDate.atOffset(ZoneOffset.UTC))
        withReason(departureReason ?: departureReasonEntityFactory.produceAndPersist())
        withMoveOnCategory(departureMoveOnCategory ?: moveOnCategoryEntityFactory.produceAndPersist())
        withNotes(departureNotes)
      },
    )
  }

  nonArrivalReason?.let {
    booking.nonArrival = nonArrivalEntityFactory.produceAndPersist {
      withBooking(booking)
      withReason(it)
      withCreatedAt(nonArrivalConfirmedAt ?: OffsetDateTime.now())
      withNotes(nonArrivalNotes ?: "default notes")
    }
  }

  return booking
}

@SuppressWarnings("LongParameterList")
fun IntegrationTestBase.givenABookingForAnOfflineApplication(
  crn: String,
  offlineApplication: OfflineApplicationEntity,
  premises: PremisesEntity? = null,
  arrivalDate: LocalDate = LocalDate.now().randomDateBefore(14),
  departureDate: LocalDate = LocalDate.now().randomDateBefore(14),
) = bookingEntityFactory.produceAndPersist {
  withCrn(crn)
  withPremises(premises ?: approvedPremisesEntityFactory.produceAndPersist())
  withApplication(null)
  withOfflineApplication(offlineApplication)
  withArrivalDate(arrivalDate)
  withDepartureDate(departureDate)
  withAdhoc(true)
}
