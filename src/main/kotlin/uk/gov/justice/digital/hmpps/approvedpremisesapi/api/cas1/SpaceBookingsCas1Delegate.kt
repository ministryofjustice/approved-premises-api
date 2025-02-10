package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1AssignKeyWorker
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewSpaceBookingCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NonArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingResidency
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingSummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1UpdateSpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link SpaceBookingsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface SpaceBookingsCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see SpaceBookingsCas1#assignKeyworker
     */
    fun assignKeyworker(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        cas1AssignKeyWorker: Cas1AssignKeyWorker): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
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
     * @see SpaceBookingsCas1#cancelSpaceBooking
     */
    fun cancelSpaceBooking(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: Cas1NewSpaceBookingCancellation): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
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
     * @see SpaceBookingsCas1#createSpaceBooking
     */
    fun createSpaceBooking(placementRequestId: java.util.UUID,
        body: Cas1NewSpaceBooking): ResponseEntity<Cas1SpaceBooking> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"otherBookingsInPremisesForCrn\" : [ {    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"tier\" : \"tier\",  \"expectedArrivalDate\" : \"2000-01-23\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"actualDepartureDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"nonArrival\" : {    \"reason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"notes\" : \"notes\",    \"confirmedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"actualArrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"requirements\" : {    \"essentialCharacteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ]  },  \"actualDepartureTime\" : \"23:15\",  \"characteristics\" : [ null, null ],  \"canonicalArrivalDate\" : \"2000-01-23\",  \"canonicalDepartureDate\" : \"2000-01-23\",  \"deliusEventNumber\" : \"deliusEventNumber\",  \"requestForPlacementId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"expectedDepartureDate\" : \"2000-01-23\",  \"keyWorkerAllocation\" : {    \"keyWorker\" : {      \"code\" : \"code\",      \"keyWorker\" : true,      \"name\" : \"Brown, James (PS - PSO)\"    },    \"allocatedAt\" : \"2000-01-23\"  },  \"cancellation\" : {    \"reason_notes\" : \"reason_notes\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"occurredAt\" : \"2000-01-23\",    \"recordedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"actualArrivalDateOnly\" : \"2000-01-23\",  \"actualArrivalTime\" : \"23:15\",  \"bookedBy\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"actualDepartureDateOnly\" : \"2000-01-23\",  \"departure\" : {    \"reason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"parentReason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  },  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrivingWithin6Weeks\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
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
     * @see SpaceBookingsCas1#getSpaceBookingById
     */
    fun getSpaceBookingById(bookingId: java.util.UUID): ResponseEntity<Cas1SpaceBooking> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"otherBookingsInPremisesForCrn\" : [ {    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"tier\" : \"tier\",  \"expectedArrivalDate\" : \"2000-01-23\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"actualDepartureDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"nonArrival\" : {    \"reason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"notes\" : \"notes\",    \"confirmedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"actualArrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"requirements\" : {    \"essentialCharacteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ]  },  \"actualDepartureTime\" : \"23:15\",  \"characteristics\" : [ null, null ],  \"canonicalArrivalDate\" : \"2000-01-23\",  \"canonicalDepartureDate\" : \"2000-01-23\",  \"deliusEventNumber\" : \"deliusEventNumber\",  \"requestForPlacementId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"expectedDepartureDate\" : \"2000-01-23\",  \"keyWorkerAllocation\" : {    \"keyWorker\" : {      \"code\" : \"code\",      \"keyWorker\" : true,      \"name\" : \"Brown, James (PS - PSO)\"    },    \"allocatedAt\" : \"2000-01-23\"  },  \"cancellation\" : {    \"reason_notes\" : \"reason_notes\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"occurredAt\" : \"2000-01-23\",    \"recordedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"actualArrivalDateOnly\" : \"2000-01-23\",  \"actualArrivalTime\" : \"23:15\",  \"bookedBy\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"actualDepartureDateOnly\" : \"2000-01-23\",  \"departure\" : {    \"reason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"parentReason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  },  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrivingWithin6Weeks\"}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see SpaceBookingsCas1#getSpaceBookingByPremiseAndId
     */
    fun getSpaceBookingByPremiseAndId(premisesId: java.util.UUID,
        bookingId: java.util.UUID): ResponseEntity<Cas1SpaceBooking> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"otherBookingsInPremisesForCrn\" : [ {    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"tier\" : \"tier\",  \"expectedArrivalDate\" : \"2000-01-23\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"actualDepartureDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"nonArrival\" : {    \"reason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"notes\" : \"notes\",    \"confirmedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"actualArrivalDate\" : \"2000-01-23T04:56:07.000+00:00\",  \"requirements\" : {    \"essentialCharacteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ]  },  \"actualDepartureTime\" : \"23:15\",  \"characteristics\" : [ null, null ],  \"canonicalArrivalDate\" : \"2000-01-23\",  \"canonicalDepartureDate\" : \"2000-01-23\",  \"deliusEventNumber\" : \"deliusEventNumber\",  \"requestForPlacementId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"expectedDepartureDate\" : \"2000-01-23\",  \"keyWorkerAllocation\" : {    \"keyWorker\" : {      \"code\" : \"code\",      \"keyWorker\" : true,      \"name\" : \"Brown, James (PS - PSO)\"    },    \"allocatedAt\" : \"2000-01-23\"  },  \"cancellation\" : {    \"reason_notes\" : \"reason_notes\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"occurredAt\" : \"2000-01-23\",    \"recordedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"actualArrivalDateOnly\" : \"2000-01-23\",  \"actualArrivalTime\" : \"23:15\",  \"bookedBy\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"actualDepartureDateOnly\" : \"2000-01-23\",  \"departure\" : {    \"reason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"parentReason\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  },  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrivingWithin6Weeks\"}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see SpaceBookingsCas1#getSpaceBookingTimeline
     */
    fun getSpaceBookingTimeline(premisesId: java.util.UUID,
        bookingId: java.util.UUID): ResponseEntity<List<TimelineEvent>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdBy\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"associatedUrls\" : [ {    \"type\" : \"application\",    \"url\" : \"url\"  }, {    \"type\" : \"application\",    \"url\" : \"url\"  } ],  \"id\" : \"id\",  \"type\" : \"approved_premises_application_submitted\",  \"triggerSource\" : \"triggerSource\",  \"content\" : \"content\"}, {  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdBy\" : {    \"telephoneNumber\" : \"telephoneNumber\",    \"service\" : \"service\",    \"deliusUsername\" : \"deliusUsername\",    \"name\" : \"name\",    \"probationDeliveryUnit\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true,    \"region\" : {      \"name\" : \"NPS North East Central Referrals\",      \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"    },    \"email\" : \"email\"  },  \"associatedUrls\" : [ {    \"type\" : \"application\",    \"url\" : \"url\"  }, {    \"type\" : \"application\",    \"url\" : \"url\"  } ],  \"id\" : \"id\",  \"type\" : \"approved_premises_application_submitted\",  \"triggerSource\" : \"triggerSource\",  \"content\" : \"content\"} ]")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see SpaceBookingsCas1#getSpaceBookings
     */
    fun getSpaceBookings(premisesId: java.util.UUID,
        residency: Cas1SpaceBookingResidency?,
        crnOrName: kotlin.String?,
        keyWorkerStaffCode: kotlin.String?,
        sortDirection: SortDirection?,
        sortBy: Cas1SpaceBookingSummarySortField?,
        page: kotlin.Int?,
        perPage: kotlin.Int?): ResponseEntity<List<Cas1SpaceBookingSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"keyWorkerAllocation\" : {    \"keyWorker\" : {      \"code\" : \"code\",      \"keyWorker\" : true,      \"name\" : \"Brown, James (PS - PSO)\"    },    \"allocatedAt\" : \"2000-01-23\"  },  \"tier\" : \"tier\",  \"person\" : {    \"personType\" : \"FullPersonSummary\",    \"crn\" : \"crn\"  },  \"canonicalArrivalDate\" : \"2000-01-23\",  \"canonicalDepartureDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrivingWithin6Weeks\"}, {  \"keyWorkerAllocation\" : {    \"keyWorker\" : {      \"code\" : \"code\",      \"keyWorker\" : true,      \"name\" : \"Brown, James (PS - PSO)\"    },    \"allocatedAt\" : \"2000-01-23\"  },  \"tier\" : \"tier\",  \"person\" : {    \"personType\" : \"FullPersonSummary\",    \"crn\" : \"crn\"  },  \"canonicalArrivalDate\" : \"2000-01-23\",  \"canonicalDepartureDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrivingWithin6Weeks\"} ]")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see SpaceBookingsCas1#recordArrival
     */
    fun recordArrival(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        cas1NewArrival: Cas1NewArrival): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
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
     * @see SpaceBookingsCas1#recordDeparture
     */
    fun recordDeparture(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        cas1NewDeparture: Cas1NewDeparture): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
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
     * @see SpaceBookingsCas1#recordNonArrival
     */
    fun recordNonArrival(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        cas1NonArrival: Cas1NonArrival): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/problem+json"))) {
                    ApiUtil.setExampleResponse(request, "application/problem+json", "Custom MIME type example not yet supported: application/problem+json")
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
     * @see SpaceBookingsCas1#updateSpaceBooking
     */
    fun updateSpaceBooking(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        cas1UpdateSpaceBooking: Cas1UpdateSpaceBooking): ResponseEntity<Unit> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
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
