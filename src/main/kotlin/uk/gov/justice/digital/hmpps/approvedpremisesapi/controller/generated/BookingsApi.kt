package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError

@RestController
interface BookingsApi {

  fun getDelegate(): BookingsApiDelegate = object : BookingsApiDelegate {}

  @Operation(
    tags = ["default"],
    summary = "Gets a booking",
    operationId = "bookingsBookingIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successfully retrieved booking",
        content = [Content(schema = Schema(implementation = Booking::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/bookings/{bookingId}"],
    produces = ["application/json"],
  )
  fun bookingsBookingIdGet(
    @Parameter(
      description = "ID of the booking",
      required = true,
    ) @PathVariable("bookingId") bookingId: java.util.UUID,
  ): ResponseEntity<Booking> {
    return getDelegate().bookingsBookingIdGet(bookingId)
  }

  @Operation(
    tags = ["default"],
    summary = "Searches for bookings with the given parameters",
    operationId = "bookingsSearchGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = BookingSearchResults::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/bookings/search"],
    produces = ["application/json", "application/problem+json"],
  )
  fun bookingsSearchGet(
    @Parameter(
      description = "If provided, only search for bookings with the given status",
      schema = Schema(
        allowableValues = ["arrived", "awaiting-arrival", "not-arrived", "departed", "cancelled", "provisional", "confirmed", "closed"],
      ),
    ) @RequestParam(value = "status", required = false) status: BookingStatus?,
    @Parameter(
      description = "If provided, return results in the given order",
      schema = Schema(
        allowableValues = ["ascending", "descending"],
      ),
    ) @RequestParam(value = "sortOrder", required = false) sortOrder: SortOrder?,
    @Parameter(
      description = "If provided, return results ordered by the given field name",
      schema = Schema(
        allowableValues = ["name", "crn", "startDate", "endDate", "createdAt"],
      ),
    ) @RequestParam(value = "sortField", required = false) sortField: BookingSearchSortField?,
    @Parameter(description = "Page number of results to return. If blank, returns all results") @RequestParam(
      value = "page",
      required = false,
    ) page: kotlin.Int?,
    @Parameter(description = "Filters bookings using exact or partial match on name or exact CRN match") @RequestParam(
      value = "crnOrName",
      required = false,
    ) crnOrName: kotlin.String?,
  ): ResponseEntity<BookingSearchResults> {
    return getDelegate().bookingsSearchGet(status, sortOrder, sortField, page, crnOrName)
  }
}
