package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingSearchService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingSearchResultTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.BookingTransformer
import java.util.UUID

@RequestMapping("/bookings", produces = [MediaType.APPLICATION_JSON_VALUE])
@RestController
class BookingsController(
  private val bookingSearchService: BookingSearchService,
  private val bookingSearchResultTransformer: BookingSearchResultTransformer,
  private val bookingService: BookingService,
  private val bookingTransformer: BookingTransformer,
) {

  @Operation(summary = "Get a booking by ID")
  @GetMapping("/{bookingId}")
  fun getBooking(
    @PathVariable bookingId: UUID,
  ): ResponseEntity<Booking> {
    val bookingAndPersons = when (val result = bookingService.getBooking(bookingId)) {
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(bookingId, result.entityType!!)
      is AuthorisableActionResult.Success -> result.entity
    }

    val apiBooking = bookingTransformer.transformJpaToApi(
      bookingAndPersons.booking,
      bookingAndPersons.personInfo,
    )

    return ResponseEntity.ok(apiBooking)
  }

  @Operation(summary = "Searches for bookings with the given parameters")
  @GetMapping("/search")
  fun getBookingSearchResults(
    @RequestParam(value = "status") status: BookingStatus?,
    @RequestParam(value = "sortOrder", defaultValue = "ascending") sortOrder: SortOrder,
    @RequestParam(value = "sortField", defaultValue = "createdAt") sortField: BookingSearchSortField,
    @RequestParam(value = "page") page: Int?,
    @RequestParam(value = "crnOrName") crnOrName: String?,
  ): ResponseEntity<BookingSearchResults> {
    val (results, metadata) = bookingSearchService.findBookings(status, sortOrder, sortField, page, crnOrName)

    return ResponseEntity.ok()
      .headers(metadata?.toHeaders())
      .body(
        bookingSearchResultTransformer.transformDomainToApi(results),
      )
  }
}

//    this is what the search stuff would look like as copied in from the generated code.

//  }
//  @Operation(
//    tags = ["default"],
//    summary = "Searches for bookings with the given parameters",
//    operationId = "bookingsSearchGet",
//    description = """""",
//
//    responses = [
//      ApiResponse(
//        responseCode = "200",
//        description = "successful operation",
//        content = [Content(schema = Schema(implementation = BookingSearchResults::class))]
//      ),
//      ApiResponse(
//        responseCode = "400",
//        description = "invalid params",
//        content = [Content(schema = Schema(implementation = ValidationError::class))]
//      ),
//      ApiResponse(
//        responseCode = "401",
//        description = "not authenticated",
//        content = [Content(schema = Schema(implementation = Problem::class))]
//      ),
//      ApiResponse(
//        responseCode = "403",
//        description = "unauthorised",
//        content = [Content(schema = Schema(implementation = Problem::class))]
//      ),
//      ApiResponse(
//        responseCode = "500",
//        description = "unexpected error",
//        content = [Content(schema = Schema(implementation = Problem::class))]
//      )
//    ]
//  )
//  @RequestMapping(
//    method = [RequestMethod.GET],
//    value = ["/bookings/search"],
//    produces = ["application/json", "application/problem+json"]
//  )
//  fun bookingsSearchGet(
//    @Parameter(
//      description = "If provided, only search for bookings with the given status",
//      schema = Schema(allowableValues = ["arrived", "awaiting-arrival", "not-arrived", "departed", "cancelled", "provisional", "confirmed", "closed"])
//    ) @RequestParam(value = "status", required = false) status: BookingStatus?,
//    @Parameter(
//      description = "If provided, return results in the given order",
//      schema = Schema(allowableValues = ["ascending", "descending"])
//    ) @RequestParam(value = "sortOrder", required = false) sortOrder: SortOrder?,
//    @Parameter(
//      description = "If provided, return results ordered by the given field name",
//      schema = Schema(allowableValues = ["name", "crn", "startDate", "endDate", "createdAt"])
//    ) @RequestParam(value = "sortField", required = false) sortField: BookingSearchSortField?,
//    @Parameter(description = "Page number of results to return. If blank, returns all results") @RequestParam(
//      value = "page",
//      required = false
//    ) page: kotlin.Int?,
//    @Parameter(description = "Filters bookings using exact or partial match on name or exact CRN match") @RequestParam(
//      value = "crnOrName",
//      required = false
//    ) crnOrName: kotlin.String?
//  ): ResponseEntity<BookingSearchResults> {
//    val sortOrder = sortOrder ?: SortOrder.ascending
//    val sortField = sortField ?: BookingSearchSortField.bookingCreatedAt
//
//    val (results, metadata) = bookingSearchService.findBookings(status, sortOrder, sortField, page, crnOrName)
//
//    return ResponseEntity.ok()
//      .headers(metadata?.toHeaders())
//      .body(
//        bookingSearchResultTransformer.transformDomainToApi(results),
//      )
//  }
// }
