package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingSearchSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BookingStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortOrder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link BookingsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface BookingsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see BookingsApi#bookingsBookingIdGet
     */
    fun bookingsBookingIdGet(bookingId: java.util.UUID): ResponseEntity<Booking> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"effectiveEndDate\" : \"2000-01-23\",  \"arrival\" : \"\",  \"turnaround\" : \"\",  \"turnaroundStartDate\" : \"2000-01-23\",  \"originalDepartureDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"nonArrival\" : \"\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"originalArrivalDate\" : \"2000-01-23\",  \"confirmation\" : \"\",  \"serviceName\" : \"approved-premises\",  \"departures\" : [ {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"turnarounds\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"arrivalDate\" : \"2000-01-23\",  \"cancellations\" : [ {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"extensions\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"cancellation\" : \"\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"keyWorker\" : \"\",  \"departure\" : \"\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrived\"}")
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
     * @see BookingsApi#bookingsSearchGet
     */
    fun bookingsSearchGet(status: BookingStatus?,
        sortOrder: SortOrder?,
        sortField: BookingSearchSortField?,
        page: kotlin.Int?,
        crnOrName: kotlin.String?): ResponseEntity<BookingSearchResults> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"resultsCount\" : 0,  \"results\" : [ {    \"bed\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"booking\" : {      \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"endDate\" : \"2000-01-23\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"startDate\" : \"2000-01-23\",      \"status\" : \"arrived\"    },    \"person\" : {      \"name\" : \"name\",      \"crn\" : \"crn\"    },    \"premises\" : {      \"town\" : \"town\",      \"name\" : \"name\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"room\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  }, {    \"bed\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"booking\" : {      \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"endDate\" : \"2000-01-23\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"startDate\" : \"2000-01-23\",      \"status\" : \"arrived\"    },    \"person\" : {      \"name\" : \"name\",      \"crn\" : \"crn\"    },    \"premises\" : {      \"town\" : \"town\",      \"name\" : \"name\",      \"postcode\" : \"postcode\",      \"addressLine1\" : \"addressLine1\",      \"addressLine2\" : \"addressLine2\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    },    \"room\" : {      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  } ]}")
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
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
