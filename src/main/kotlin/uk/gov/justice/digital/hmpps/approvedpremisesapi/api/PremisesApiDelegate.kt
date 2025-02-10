package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Arrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.BedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Booking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Confirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Departure
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ExtendedPremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Extension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.LostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewArrival
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewConfirmation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDateChange
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewDeparture
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewExtension
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewLostBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewPremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewRoom
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewTurnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PremisesSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Room
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Turnaround
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateLostBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdatePremises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateRoom
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PremisesApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface PremisesApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PremisesApi#premisesGet
     */
    fun premisesGet(xServiceName: ServiceName?,
        xUserRegion: java.util.UUID?): ResponseEntity<List<Premises>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}, {  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"} ]")
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
     * @see PremisesApi#premisesPost
     */
    fun premisesPost(body: NewPremises,
        xServiceName: ServiceName?): ResponseEntity<Premises> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}")
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


    /**
     * @see PremisesApi#premisesPremisesIdBedsBedIdGet
     */
    fun premisesPremisesIdBedsBedIdGet(premisesId: java.util.UUID,
        bedId: java.util.UUID): ResponseEntity<BedDetail> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"propertyName\",    \"name\" : \"name\"  }, {    \"propertyName\" : \"propertyName\",    \"name\" : \"name\"  } ],  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"roomName\" : \"roomName\",  \"status\" : \"occupied\"}")
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
     * @see PremisesApi#premisesPremisesIdBedsGet
     */
    fun premisesPremisesIdBedsGet(premisesId: java.util.UUID): ResponseEntity<List<BedSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"roomName\" : \"roomName\",  \"status\" : \"occupied\"}, {  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"roomName\" : \"roomName\",  \"status\" : \"occupied\"} ]")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdArrivalsPost
     */
    fun premisesPremisesIdBookingsBookingIdArrivalsPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewArrival): ResponseEntity<Arrival> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"notes\" : \"notes\",  \"keyWorkerStaffCode\" : \"keyWorkerStaffCode\",  \"arrivalTime\" : \"arrivalTime\",  \"expectedDepartureDate\" : \"2000-01-23\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"arrivalDate\" : \"2000-01-23\"}")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdCancellationsPost
     */
    fun premisesPremisesIdBookingsBookingIdCancellationsPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewCancellation): ResponseEntity<Cancellation> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"date\" : \"2000-01-23\",  \"reason\" : {    \"name\" : \"Recall\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"otherReason\" : \"otherReason\",  \"notes\" : \"notes\",  \"premisesName\" : \"premisesName\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdConfirmationsPost
     */
    fun premisesPremisesIdBookingsBookingIdConfirmationsPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewConfirmation): ResponseEntity<Confirmation> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"notes\" : \"notes\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdDateChangesPost
     */
    fun premisesPremisesIdBookingsBookingIdDateChangesPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewDateChange): ResponseEntity<DateChange> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"newArrivalDate\" : \"2000-01-23\",  \"previousDepartureDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"previousArrivalDate\" : \"2000-01-23\",  \"newDepartureDate\" : \"2000-01-23\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdDeparturesPost
     */
    fun premisesPremisesIdBookingsBookingIdDeparturesPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewDeparture): ResponseEntity<Departure> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",  \"reason\" : {    \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"name\" : \"Admitted to Hospital\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"destinationProvider\" : {    \"name\" : \"Ext - North East Region\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"notes\" : \"notes\",  \"moveOnCategory\" : {    \"name\" : \"Housing Association - Rented\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdExtensionsPost
     */
    fun premisesPremisesIdBookingsBookingIdExtensionsPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewExtension): ResponseEntity<Extension> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"notes\" : \"notes\",  \"previousDepartureDate\" : \"2000-01-23\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"newDepartureDate\" : \"2000-01-23\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdGet
     */
    fun premisesPremisesIdBookingsBookingIdGet(premisesId: java.util.UUID,
        bookingId: java.util.UUID): ResponseEntity<Booking> {
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see PremisesApi#premisesPremisesIdBookingsBookingIdTurnaroundsPost
     */
    fun premisesPremisesIdBookingsBookingIdTurnaroundsPost(premisesId: java.util.UUID,
        bookingId: java.util.UUID,
        body: NewTurnaround): ResponseEntity<Turnaround> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"workingDays\" : 0,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see PremisesApi#premisesPremisesIdBookingsGet
     */
    fun premisesPremisesIdBookingsGet(premisesId: java.util.UUID): ResponseEntity<List<Booking>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"effectiveEndDate\" : \"2000-01-23\",  \"arrival\" : \"\",  \"turnaround\" : \"\",  \"turnaroundStartDate\" : \"2000-01-23\",  \"originalDepartureDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"nonArrival\" : \"\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"originalArrivalDate\" : \"2000-01-23\",  \"confirmation\" : \"\",  \"serviceName\" : \"approved-premises\",  \"departures\" : [ {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"turnarounds\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"arrivalDate\" : \"2000-01-23\",  \"cancellations\" : [ {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"extensions\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"cancellation\" : \"\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"keyWorker\" : \"\",  \"departure\" : \"\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrived\"}, {  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"effectiveEndDate\" : \"2000-01-23\",  \"arrival\" : \"\",  \"turnaround\" : \"\",  \"turnaroundStartDate\" : \"2000-01-23\",  \"originalDepartureDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"nonArrival\" : \"\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"originalArrivalDate\" : \"2000-01-23\",  \"confirmation\" : \"\",  \"serviceName\" : \"approved-premises\",  \"departures\" : [ {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"turnarounds\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"arrivalDate\" : \"2000-01-23\",  \"cancellations\" : [ {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"extensions\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"cancellation\" : \"\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"keyWorker\" : \"\",  \"departure\" : \"\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrived\"} ]")
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
     * @see PremisesApi#premisesPremisesIdBookingsPost
     */
    fun premisesPremisesIdBookingsPost(premisesId: java.util.UUID,
        body: NewBooking): ResponseEntity<Booking> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"bed\" : {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  },  \"effectiveEndDate\" : \"2000-01-23\",  \"arrival\" : \"\",  \"turnaround\" : \"\",  \"turnaroundStartDate\" : \"2000-01-23\",  \"originalDepartureDate\" : \"2000-01-23\",  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"departureDate\" : \"2000-01-23\",  \"nonArrival\" : \"\",  \"assessmentId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"originalArrivalDate\" : \"2000-01-23\",  \"confirmation\" : \"\",  \"serviceName\" : \"approved-premises\",  \"departures\" : [ {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"dateTime\" : \"2000-01-23T04:56:07.000+00:00\",    \"reason\" : {      \"parentReasonId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"name\" : \"Admitted to Hospital\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"destinationProvider\" : {      \"name\" : \"Ext - North East Region\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"moveOnCategory\" : {      \"name\" : \"Housing Association - Rented\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"turnarounds\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"workingDays\" : 0,    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"arrivalDate\" : \"2000-01-23\",  \"cancellations\" : [ {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"date\" : \"2000-01-23\",    \"reason\" : {      \"name\" : \"Recall\",      \"serviceScope\" : \"serviceScope\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"otherReason\" : \"otherReason\",    \"notes\" : \"notes\",    \"premisesName\" : \"premisesName\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"extensions\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"notes\" : \"notes\",    \"previousDepartureDate\" : \"2000-01-23\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"newDepartureDate\" : \"2000-01-23\",    \"bookingId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"cancellation\" : \"\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"keyWorker\" : \"\",  \"departure\" : \"\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"status\" : \"arrived\"}")
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
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"instance\" : \"f7493e12-546d-42c3-b838-06c12671ab5b\",  \"detail\" : \"You provided invalid request parameters\",  \"type\" : \"https://example.net/validation-error\",  \"title\" : \"Invalid request parameters\",  \"status\" : 400}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see PremisesApi#premisesPremisesIdGet
     */
    fun premisesPremisesIdGet(premisesId: java.util.UUID): ResponseEntity<Premises> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}")
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
     * @see PremisesApi#premisesPremisesIdLostBedsGet
     */
    fun premisesPremisesIdLostBedsGet(premisesId: java.util.UUID): ResponseEntity<List<LostBed>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"cancellation\" : \"\",  \"endDate\" : \"2000-01-23\",  \"referenceNumber\" : \"referenceNumber\",  \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"bedName\" : \"bedName\",  \"roomName\" : \"roomName\",  \"status\" : \"active\"}, {  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"cancellation\" : \"\",  \"endDate\" : \"2000-01-23\",  \"referenceNumber\" : \"referenceNumber\",  \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"bedName\" : \"bedName\",  \"roomName\" : \"roomName\",  \"status\" : \"active\"} ]")
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
     * @see PremisesApi#premisesPremisesIdLostBedsLostBedIdCancellationsPost
     */
    fun premisesPremisesIdLostBedsLostBedIdCancellationsPost(premisesId: java.util.UUID,
        lostBedId: java.util.UUID,
        body: NewLostBedCancellation): ResponseEntity<LostBedCancellation> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"notes\" : \"notes\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"}")
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
     * @see PremisesApi#premisesPremisesIdLostBedsLostBedIdGet
     */
    fun premisesPremisesIdLostBedsLostBedIdGet(premisesId: java.util.UUID,
        lostBedId: java.util.UUID): ResponseEntity<LostBed> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"cancellation\" : \"\",  \"endDate\" : \"2000-01-23\",  \"referenceNumber\" : \"referenceNumber\",  \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"bedName\" : \"bedName\",  \"roomName\" : \"roomName\",  \"status\" : \"active\"}")
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
     * @see PremisesApi#premisesPremisesIdLostBedsLostBedIdPut
     */
    fun premisesPremisesIdLostBedsLostBedIdPut(premisesId: java.util.UUID,
        lostBedId: java.util.UUID,
        body: UpdateLostBed): ResponseEntity<LostBed> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"cancellation\" : \"\",  \"endDate\" : \"2000-01-23\",  \"referenceNumber\" : \"referenceNumber\",  \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"bedName\" : \"bedName\",  \"roomName\" : \"roomName\",  \"status\" : \"active\"}")
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
     * @see PremisesApi#premisesPremisesIdLostBedsPost
     */
    fun premisesPremisesIdLostBedsPost(premisesId: java.util.UUID,
        body: NewLostBed): ResponseEntity<LostBed> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"serviceScope\" : \"serviceScope\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"cancellation\" : \"\",  \"endDate\" : \"2000-01-23\",  \"referenceNumber\" : \"referenceNumber\",  \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"bedName\" : \"bedName\",  \"roomName\" : \"roomName\",  \"status\" : \"active\"}")
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
     * @see PremisesApi#premisesPremisesIdPut
     */
    fun premisesPremisesIdPut(premisesId: java.util.UUID,
        body: UpdatePremises): ResponseEntity<Premises> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"notes\" : \"some notes about this property\",  \"town\" : \"Braintree\",  \"probationRegion\" : {    \"name\" : \"NPS North East Central Referrals\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"  },  \"postcode\" : \"LS1 3AD\",  \"availableBedsForToday\" : 20,  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"localAuthorityArea\" : {    \"identifier\" : \"LEEDS\",    \"name\" : \"Leeds City Council\",    \"id\" : \"6abb5fa3-e93f-4445-887b-30d081688f44\"  },  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}")
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


    /**
     * @see PremisesApi#premisesPremisesIdRoomsGet
     */
    fun premisesPremisesIdRoomsGet(premisesId: java.util.UUID): ResponseEntity<List<Room>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}, {  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]} ]")
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
     * @see PremisesApi#premisesPremisesIdRoomsPost
     */
    fun premisesPremisesIdRoomsPost(premisesId: java.util.UUID,
        newRoom: NewRoom): ResponseEntity<Room> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}")
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
     * @see PremisesApi#premisesPremisesIdRoomsRoomIdGet
     */
    fun premisesPremisesIdRoomsRoomIdGet(premisesId: java.util.UUID,
        roomId: java.util.UUID): ResponseEntity<Room> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}")
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
     * @see PremisesApi#premisesPremisesIdRoomsRoomIdPut
     */
    fun premisesPremisesIdRoomsRoomIdPut(premisesId: java.util.UUID,
        roomId: java.util.UUID,
        updateRoom: UpdateRoom): ResponseEntity<Room> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"characteristics\" : [ {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  }, {    \"propertyName\" : \"isCatered\",    \"name\" : \"Is this premises catered (rather than self-catered)?\",    \"serviceScope\" : \"approved-premises\",    \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\",    \"modelScope\" : \"premises\"  } ],  \"code\" : \"NEABC-4\",  \"notes\" : \"notes\",  \"name\" : \"name\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"beds\" : [ {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  }, {    \"code\" : \"NEABC04\",    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"bedEndDate\" : \"2024-03-30\"  } ]}")
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
     * @see PremisesApi#premisesPremisesIdStaffGet
     */
    fun premisesPremisesIdStaffGet(premisesId: java.util.UUID): ResponseEntity<List<StaffMember>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"code\" : \"code\",  \"keyWorker\" : true,  \"name\" : \"Brown, James (PS - PSO)\"}, {  \"code\" : \"code\",  \"keyWorker\" : true,  \"name\" : \"Brown, James (PS - PSO)\"} ]")
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
     * @see PremisesApi#premisesPremisesIdSummaryGet
     */
    fun premisesPremisesIdSummaryGet(premisesId: java.util.UUID): ResponseEntity<ExtendedPremisesSummary> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"dateCapacities\" : [ {    \"date\" : \"2000-01-23\",    \"availableBeds\" : 10  }, {    \"date\" : \"2000-01-23\",    \"availableBeds\" : 10  } ],  \"name\" : \"name\",  \"postcode\" : \"postcode\",  \"availableBedsForToday\" : 6,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"apCode\",  \"bookings\" : [ {    \"bed\" : {      \"code\" : \"NEABC04\",      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"bedEndDate\" : \"2024-03-30\"    },    \"person\" : {      \"type\" : \"FullPerson\",      \"crn\" : \"crn\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"departureDate\" : \"2000-01-23\",    \"arrivalDate\" : \"2000-01-23\",    \"status\" : \"arrived\"  }, {    \"bed\" : {      \"code\" : \"NEABC04\",      \"name\" : \"name\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"bedEndDate\" : \"2024-03-30\"    },    \"person\" : {      \"type\" : \"FullPerson\",      \"crn\" : \"crn\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"departureDate\" : \"2000-01-23\",    \"arrivalDate\" : \"2000-01-23\",    \"status\" : \"arrived\"  } ],  \"bedCount\" : 0}")
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
     * @see PremisesApi#premisesSummaryGet
     */
    fun premisesSummaryGet(xServiceName: ServiceName,
        probationRegionId: java.util.UUID?,
        apAreaId: java.util.UUID?): ResponseEntity<List<PremisesSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"postcode\" : \"LS1 3AD\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"}, {  \"service\" : \"service\",  \"name\" : \"Hope House\",  \"postcode\" : \"LS1 3AD\",  \"addressLine1\" : \"one something street\",  \"addressLine2\" : \"Blackmore End\",  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"bedCount\" : 22,  \"status\" : \"pending\"} ]")
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
