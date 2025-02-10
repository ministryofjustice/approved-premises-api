package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingCancelledUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingConfirmedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3BookingProvisionallyMadeEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonArrivedUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3PersonDepartureUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas3.model.CAS3ReferralSubmittedEvent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link CAS3EventsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface CAS3EventsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see CAS3EventsApi#eventsCas3BookingCancelledEventIdGet
     */
    fun eventsCas3BookingCancelledEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3BookingCancelledEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"notes\" : \"notes\",    \"cancellationReason\" : \"cancellationReason\",    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"cancelledAt\" : \"2000-01-23\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"cancelledBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3BookingCancelledUpdatedEventIdGet
     */
    fun eventsCas3BookingCancelledUpdatedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3BookingCancelledUpdatedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"notes\" : \"notes\",    \"cancellationReason\" : \"cancellationReason\",    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"cancelledAt\" : \"2000-01-23\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"cancelledBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3BookingConfirmedEventIdGet
     */
    fun eventsCas3BookingConfirmedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3BookingConfirmedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"notes\" : \"notes\",    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"expectedArrivedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"confirmedBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3BookingProvisionallyMadeEventIdGet
     */
    fun eventsCas3BookingProvisionallyMadeEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3BookingProvisionallyMadeEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"notes\" : \"notes\",    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"expectedArrivedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"bookedBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3PersonArrivedEventIdGet
     */
    fun eventsCas3PersonArrivedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3PersonArrivedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"recordedBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"notes\" : \"notes\",    \"arrivedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"premises\" : {      \"town\" : \"town\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"region\" : \"region\"    },    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"deliusEventNumber\" : \"deliusEventNumber\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"expectedDepartureOn\" : \"2000-01-23\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3PersonArrivedUpdatedEventIdGet
     */
    fun eventsCas3PersonArrivedUpdatedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3PersonArrivedUpdatedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"recordedBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"notes\" : \"notes\",    \"arrivedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"premises\" : {      \"town\" : \"town\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"region\" : \"region\"    },    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"deliusEventNumber\" : \"deliusEventNumber\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"expectedDepartureOn\" : \"2000-01-23\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3PersonDepartedEventIdGet
     */
    fun eventsCas3PersonDepartedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3PersonDepartedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"reason\" : \"reason\",    \"recordedBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"reasonDetail\" : \"reasonDetail\",    \"notes\" : \"notes\",    \"premises\" : {      \"town\" : \"town\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"region\" : \"region\"    },    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"deliusEventNumber\" : \"deliusEventNumber\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"departedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3PersonDepartureUpdatedEventIdGet
     */
    fun eventsCas3PersonDepartureUpdatedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3PersonDepartureUpdatedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"reason\" : \"reason\",    \"recordedBy\" : {      \"probationRegionCode\" : \"N53\",      \"staffCode\" : \"N54A999\",      \"username\" : \"JohnSmithNPS\"    },    \"reasonDetail\" : \"reasonDetail\",    \"notes\" : \"notes\",    \"premises\" : {      \"town\" : \"town\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"region\" : \"region\"    },    \"bookingUrl\" : \"https://openapi-generator.tech\",    \"deliusEventNumber\" : \"deliusEventNumber\",    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"departedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see CAS3EventsApi#eventsCas3ReferralSubmittedEventIdGet
     */
    fun eventsCas3ReferralSubmittedEventIdGet(eventId: java.util.UUID): ResponseEntity<CAS3ReferralSubmittedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"applicationUrl\" : \"https://openapi-generator.tech\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"accommodation.cas3.booking.cancelled\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
