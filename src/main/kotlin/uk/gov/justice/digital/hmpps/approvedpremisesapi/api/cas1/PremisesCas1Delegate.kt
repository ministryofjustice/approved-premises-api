package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremiseCapacity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1Premises
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBasicSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesBedSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PremisesDaySummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingCharacteristic
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceBookingDaySummarySortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PremisesCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface PremisesCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PremisesCas1#getBeds
     */
    fun getBeds(premisesId: java.util.UUID): ResponseEntity<List<Cas1PremisesBedSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"roomName\" : \"roomName\",  \"bedName\" : \"bedName\"}, {  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"roomName\" : \"roomName\",  \"bedName\" : \"bedName\"} ]")
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
     * @see PremisesCas1#getCapacity
     */
    fun getCapacity(premisesId: java.util.UUID,
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        excludeSpaceBookingId: java.util.UUID?): ResponseEntity<Cas1PremiseCapacity> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"endDate\" : \"2000-01-23\",  \"startDate\" : \"2000-01-23\",  \"capacity\" : [ {    \"date\" : \"2000-01-23\",    \"totalBedCount\" : 0,    \"availableBedCount\" : 6,    \"characteristicAvailability\" : [ {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"hasEnSuite\"    }, {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"hasEnSuite\"    } ],    \"bookingCount\" : 1  }, {    \"date\" : \"2000-01-23\",    \"totalBedCount\" : 0,    \"availableBedCount\" : 6,    \"characteristicAvailability\" : [ {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"hasEnSuite\"    }, {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"hasEnSuite\"    } ],    \"bookingCount\" : 1  } ]}")
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
     * @see PremisesCas1#getDaySummary
     */
    fun getDaySummary(premisesId: java.util.UUID,
        date: java.time.LocalDate,
        bookingsCriteriaFilter: kotlin.collections.List<Cas1SpaceBookingCharacteristic>?,
        bookingsSortDirection: SortDirection?,
        bookingsSortBy: Cas1SpaceBookingDaySummarySortField?,
        excludeSpaceBookingId: java.util.UUID?): ResponseEntity<Cas1PremisesDaySummary> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"spaceBookings\" : [ {    \"essentialCharacteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],    \"tier\" : \"tier\",    \"person\" : {      \"personType\" : \"FullPersonSummary\",      \"crn\" : \"crn\"    },    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"releaseType\" : \"releaseType\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  }, {    \"essentialCharacteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],    \"tier\" : \"tier\",    \"person\" : {      \"personType\" : \"FullPersonSummary\",      \"crn\" : \"crn\"    },    \"canonicalArrivalDate\" : \"2000-01-23\",    \"canonicalDepartureDate\" : \"2000-01-23\",    \"releaseType\" : \"releaseType\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  } ],  \"previousDate\" : \"2000-01-23\",  \"forDate\" : \"2000-01-23\",  \"nextDate\" : \"2000-01-23\",  \"capacity\" : {    \"date\" : \"2000-01-23\",    \"totalBedCount\" : 0,    \"availableBedCount\" : 6,    \"characteristicAvailability\" : [ {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"hasEnSuite\"    }, {      \"bookingsCount\" : 5,      \"availableBedsCount\" : 5,      \"characteristic\" : \"hasEnSuite\"    } ],    \"bookingCount\" : 1  },  \"outOfServiceBeds\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"characteristics\" : [ null, null ],    \"endDate\" : \"2000-01-23\",    \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"roomName\" : \"roomName\",    \"startDate\" : \"2000-01-23\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"characteristics\" : [ null, null ],    \"endDate\" : \"2000-01-23\",    \"bedId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"roomName\" : \"roomName\",    \"startDate\" : \"2000-01-23\"  } ]}")
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
     * @see PremisesCas1#getPremisesById
     */
    fun getPremisesById(premisesId: java.util.UUID): ResponseEntity<Cas1Premises> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"managerDetails\" : \"managerDetails\",  \"overbookingSummary\" : [ {    \"startInclusive\" : \"2022-07-28\",    \"endInclusive\" : \"2022-07-29\"  }, {    \"startInclusive\" : \"2022-07-28\",    \"endInclusive\" : \"2022-07-29\"  } ],  \"apArea\" : {    \"identifier\" : \"LON\",    \"name\" : \"Yorkshire & The Humber\",    \"id\" : \"cd1c2d43-0b0b-4438-b0e3-d4424e61fb6a\"  },  \"availableBeds\" : 20,  \"name\" : \"Hope House\",  \"fullAddress\" : \"fullAddress\",  \"postcode\" : \"LS1 3AD\",  \"supportsSpaceBookings\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"NEHOPE1\",  \"bedCount\" : 22,  \"outOfServiceBeds\" : 2}")
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
     * @see PremisesCas1#getPremisesSummaries
     */
    fun getPremisesSummaries(gender: Cas1ApprovedPremisesGender?,
        apAreaId: java.util.UUID?): ResponseEntity<List<Cas1PremisesBasicSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"name\" : \"Hope House\",  \"fullAddress\" : \"fullAddress\",  \"postcode\" : \"postcode\",  \"supportsSpaceBookings\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"NEHOPE1\",  \"bedCount\" : 22}, {  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"name\" : \"Hope House\",  \"fullAddress\" : \"fullAddress\",  \"postcode\" : \"postcode\",  \"supportsSpaceBookings\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"apCode\" : \"NEHOPE1\",  \"bedCount\" : 22} ]")
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
