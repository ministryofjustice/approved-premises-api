package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.PremisesApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.health.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.BadRequestProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.InternalServerErrorProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.KeyWorkerService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PersonService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.ArrivalTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PremisesTransformer
import java.util.UUID

@Service
class PremisesController(
  private val premisesService: PremisesService,
  private val personService: PersonService,
  private val keyWorkerService: KeyWorkerService,
  private val bookingService: BookingService,
  private val premisesTransformer: PremisesTransformer,
  private val bookingTransformer: BookingTransformer,
  private val arrivalTransformer: ArrivalTransformer
) : PremisesApiDelegate {
  override fun premisesGet(): ResponseEntity<List<Premises>> {
    return ResponseEntity.ok(
      premisesService.getAllPremises().map(premisesTransformer::transformJpaToApi)
    )
  }

  override fun premisesPremisesIdGet(premisesId: UUID): ResponseEntity<Premises> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(premisesTransformer.transformJpaToApi(premises))
  }

  override fun premisesPremisesIdBookingsGet(premisesId: UUID): ResponseEntity<List<Booking>> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    return ResponseEntity.ok(
      premises.bookings.map {
        val person = personService.getPerson(it.crn)
          ?: throw InternalServerErrorProblem("Unable to get Person via crn: ${it.crn}")
        bookingTransformer.transformJpaToApi(it, person)
      }
    )
  }

  override fun premisesPremisesIdBookingsPost(premisesId: UUID, body: NewBooking): ResponseEntity<Booking> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val validationErrors = mutableMapOf<String, String>()

    val person = personService.getPerson(body.crn)
    if (person == null) validationErrors["crn"] = "Invalid crn"

    // TODO: We will potentially need to check that if the JWT belongs to a person rather than a service account
    //       that the requester is the key worker and 403 otherwise?
    val keyWorker = keyWorkerService.getKeyWorker(body.keyWorkerId)
    if (keyWorker == null) validationErrors["keyWorkerId"] = "Invalid keyWorkerId"

    if (validationErrors.any()) {
      throw BadRequestProblem(validationErrors)
    }

    val booking = bookingService.createBooking(
      BookingEntity(
        id = UUID.randomUUID(),
        crn = person!!.crn,
        arrivalDate = body.expectedArrivalDate,
        departureDate = body.expectedDepartureDate,
        keyWorker = keyWorker!!,
        arrival = null,
        departure = null,
        nonArrival = null,
        cancellation = null,
        premises = premises
      )
    )

    return ResponseEntity.ok(bookingTransformer.transformJpaToApi(booking, person))
  }

  override fun premisesPremisesIdBookingsBookingIdArrivalsPost(
    premisesId: UUID,
    bookingId: UUID,
    body: NewArrival
  ): ResponseEntity<Arrival> {
    val premises = premisesService.getPremises(premisesId)
      ?: throw NotFoundProblem(premisesId, "Premises")

    val booking = bookingService.getBooking(bookingId)
      ?: throw NotFoundProblem(bookingId, "Booking")

    if (booking.premises.id != premises.id) {
      throw NotFoundProblem(bookingId, "Booking")
    }

    if (booking.arrival != null) {
      throw BadRequestProblem(errorDetail = "This Booking already has an Arrival set")
    }

    if (body.expectedDepartureDate.isBefore(body.arrivalDate)) {
      throw BadRequestProblem(mapOf("expectedDepartureDate" to "Cannot be before arrivalDate"))
    }

    val arrival = bookingService.createArrival(
      ArrivalEntity(
        id = UUID.randomUUID(),
        arrivalDate = body.arrivalDate,
        expectedDepartureDate = body.expectedDepartureDate,
        notes = body.notes,
        booking = booking
      )
    )

    return ResponseEntity.ok(arrivalTransformer.transformJpaToApi(arrival))
  }
}
