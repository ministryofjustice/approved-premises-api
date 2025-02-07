/**
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech) (7.11.0).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated

import io.swagger.v3.oas.annotations.*
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingExtendedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope

@RestController
interface EventsApi {

  fun getDelegate(): EventsApiDelegate = object : EventsApiDelegate {}

  @Operation(
    tags = ["'Apply' events"],
    summary = "An 'application-assessed' event",
    operationId = "eventsApplicationAssessedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'application-assessed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = ApplicationAssessedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No application-assessed event found for the provided `eventId`",
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
    value = ["/events/application-assessed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationAssessedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<ApplicationAssessedEnvelope> {
    return getDelegate().eventsApplicationAssessedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "An 'application-expired' event",
    operationId = "eventsApplicationExpiredEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'application-expired' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = ApplicationExpiredEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'application-expired' event found for the provided `eventId`",
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
    value = ["/events/application-expired/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationExpiredEventIdGet(
    @Parameter(description = "UUID of the event", required = true) @PathVariable(
      "eventId",
    ) eventId: java.util.UUID,
  ): ResponseEntity<ApplicationExpiredEnvelope> {
    return getDelegate().eventsApplicationExpiredEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Apply' events"],
    summary = "An application-submitted event",
    operationId = "eventsApplicationSubmittedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The application-submitted corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = ApplicationSubmittedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No application-submitted event found for the provided `eventId`",
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
    value = ["/events/application-submitted/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationSubmittedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<ApplicationSubmittedEnvelope> {
    return getDelegate().eventsApplicationSubmittedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Apply' events"],
    summary = "An application-withdrawn event",
    operationId = "eventsApplicationWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The application-withdrawn event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = ApplicationWithdrawnEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No application-withdrawn event found for the provided `eventId`",
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
    value = ["/events/application-withdrawn/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsApplicationWithdrawnEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<ApplicationWithdrawnEnvelope> {
    return getDelegate().eventsApplicationWithdrawnEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Apply' events"],
    summary = "An assessment-allocated event",
    operationId = "eventsAssessmentAllocatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The assessment-allocated event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = AssessmentAllocatedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No assessment-allocated event found for the provided `eventId`",
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
    value = ["/events/assessment-allocated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsAssessmentAllocatedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<AssessmentAllocatedEnvelope> {
    return getDelegate().eventsAssessmentAllocatedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Apply' events"],
    summary = "An 'assessment-appealed' event",
    operationId = "eventsAssessmentAppealedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'assessment-appealed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = AssessmentAppealedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No assessment-appealed event found for the provided `eventId`",
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
    value = ["/events/assessment-appealed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsAssessmentAppealedEventIdGet(
    @Parameter(description = "UUID of the event", required = true) @PathVariable(
      "eventId",
    ) eventId: java.util.UUID,
  ): ResponseEntity<AssessmentAppealedEnvelope> {
    return getDelegate().eventsAssessmentAppealedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'booking-cancelled' event",
    operationId = "eventsBookingCancelledEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-cancelled' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingCancelledEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-made' event found for the provided `eventId`",
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
    value = ["/events/booking-cancelled/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingCancelledEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingCancelledEnvelope> {
    return getDelegate().eventsBookingCancelledEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'booking-changed' event",
    operationId = "eventsBookingChangedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-changed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingChangedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-changed' event found for the provided `eventId`",
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
    value = ["/events/booking-changed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingChangedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingChangedEnvelope> {
    return getDelegate().eventsBookingChangedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'booking-extended' event",
    operationId = "eventsBookingExtendedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-extended' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingExtendedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-extended' event found for the provided `eventId`",
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
    value = ["/events/booking-extended/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingExtendedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingExtendedEnvelope> {
    return getDelegate().eventsBookingExtendedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'booking-keyworker-assigned' event",
    operationId = "eventsBookingKeyworkerAssignedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-keyworker-assigned' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingKeyWorkerAssignedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-keyworker-changed' event found for the provided `eventId`",
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
    value = ["/events/booking-keyworker-assigned/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingKeyworkerAssignedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingKeyWorkerAssignedEnvelope> {
    return getDelegate().eventsBookingKeyworkerAssignedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Match' events"],
    summary = "A 'booking-made' event",
    operationId = "eventsBookingMadeEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-made' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingMadeEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-made' event found for the provided `eventId`",
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
    value = ["/events/booking-made/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingMadeEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingMadeEnvelope> {
    return getDelegate().eventsBookingMadeEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Match' events"],
    summary = "A 'booking-not-made' event",
    operationId = "eventsBookingNotMadeEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'booking-not-made' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = BookingNotMadeEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'booking-not-made' event found for the provided `eventId`",
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
    value = ["/events/booking-not-made/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsBookingNotMadeEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<BookingNotMadeEnvelope> {
    return getDelegate().eventsBookingNotMadeEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Apply' events"],
    summary = "A 'further-information-requested' event",
    operationId = "eventsFurtherInformationRequestedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'further-information-requested' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = FurtherInformationRequestedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-not-arrived' event found for the provided `eventId`",
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
    value = ["/events/further-information-requested/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsFurtherInformationRequestedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<FurtherInformationRequestedEnvelope> {
    return getDelegate().eventsFurtherInformationRequestedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'match-request-withdrawn' event",
    operationId = "eventsMatchRequestWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'match-request-withdrawn' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = MatchRequestWithdrawnEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'match-request-withdrawn' event found for the provided `eventId`",
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
    value = ["/events/match-request-withdrawn/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsMatchRequestWithdrawnEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<MatchRequestWithdrawnEnvelope> {
    return getDelegate().eventsMatchRequestWithdrawnEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'person-arrived' event",
    operationId = "eventsPersonArrivedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-arrived' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PersonArrivedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-arrived' event found for the provided `eventId`",
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
    value = ["/events/person-arrived/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPersonArrivedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PersonArrivedEnvelope> {
    return getDelegate().eventsPersonArrivedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'person-departed' event",
    operationId = "eventsPersonDepartedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-departed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PersonDepartedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-not-arrived' event found for the provided `eventId`",
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
    value = ["/events/person-departed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPersonDepartedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PersonDepartedEnvelope> {
    return getDelegate().eventsPersonDepartedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'person-not-arrived' event",
    operationId = "eventsPersonNotArrivedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'person-not-arrived' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PersonNotArrivedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'person-not-arrived' event found for the provided `eventId`",
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
    value = ["/events/person-not-arrived/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPersonNotArrivedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PersonNotArrivedEnvelope> {
    return getDelegate().eventsPersonNotArrivedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'placement-application-allocated' event",
    operationId = "eventsPlacementApplicationAllocatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'placement-application-allocated' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PlacementApplicationAllocatedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'placement-application-allocated' event found for the provided `eventId`",
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
    value = ["/events/placement-application-allocated/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPlacementApplicationAllocatedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PlacementApplicationAllocatedEnvelope> {
    return getDelegate().eventsPlacementApplicationAllocatedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'placement-application-withdrawn' event",
    operationId = "eventsPlacementApplicationWithdrawnEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'placement-application-withdrawn' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = PlacementApplicationWithdrawnEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'placement-application-withdrawn' event found for the provided `eventId`",
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
    value = ["/events/placement-application-withdrawn/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsPlacementApplicationWithdrawnEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<PlacementApplicationWithdrawnEnvelope> {
    return getDelegate().eventsPlacementApplicationWithdrawnEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'request-for-placement-assessed' event",
    operationId = "eventsRequestForPlacementAssessedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'request-for-placement-assessed' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = RequestForPlacementAssessedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'request-for-placement-assessed' event found for the provided `eventId`",
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
    value = ["/events/request-for-placement-assessed/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsRequestForPlacementAssessedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<RequestForPlacementAssessedEnvelope> {
    return getDelegate().eventsRequestForPlacementAssessedEventIdGet(eventId)
  }

  @Operation(
    tags = ["'Manage' events"],
    summary = "A 'request-for-placement-created' event",
    operationId = "eventsRequestForPlacementCreatedEventIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "The 'request-for-placement-created' event corresponding to the provided `eventId`",
        content = [Content(schema = Schema(implementation = RequestForPlacementCreatedEnvelope::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "No 'request-for-placement-created' event found for the provided `eventId`",
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
    value = ["/events/request-for-placement-created/{eventId}"],
    produces = ["application/json"],
  )
  fun eventsRequestForPlacementCreatedEventIdGet(
    @Parameter(
      description = "UUID of the event",
      required = true,
    ) @PathVariable("eventId") eventId: java.util.UUID,
  ): ResponseEntity<RequestForPlacementCreatedEnvelope> {
    return getDelegate().eventsRequestForPlacementCreatedEventIdGet(eventId)
  }
}
