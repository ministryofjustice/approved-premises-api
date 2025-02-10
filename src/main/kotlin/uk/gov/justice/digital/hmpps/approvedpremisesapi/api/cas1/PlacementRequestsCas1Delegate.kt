package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PlacementRequestSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestRequestType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PlacementRequestStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RiskTierLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PlacementRequestsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface PlacementRequestsCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PlacementRequestsCas1#search
     */
    fun search(status: PlacementRequestStatus?,
        crnOrName: kotlin.String?,
        tier: RiskTierLevel?,
        arrivalDateStart: java.time.LocalDate?,
        arrivalDateEnd: java.time.LocalDate?,
        requestType: PlacementRequestRequestType?,
        cruManagementAreaId: java.util.UUID?,
        page: kotlin.Int?,
        sortBy: PlacementRequestSortField?,
        sortDirection: SortDirection?): ResponseEntity<List<Cas1PlacementRequestSummary>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"requestedPlacementDuration\" : 0,  \"personTier\" : \"personTier\",  \"firstBookingPremisesName\" : \"firstBookingPremisesName\",  \"firstBookingArrivalDate\" : \"2000-01-23\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"requestedPlacementArrivalDate\" : \"2000-01-23\",  \"applicationSubmittedDate\" : \"2000-01-23\",  \"isParole\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"placementRequestStatus\" : \"matched\"}, {  \"requestedPlacementDuration\" : 0,  \"personTier\" : \"personTier\",  \"firstBookingPremisesName\" : \"firstBookingPremisesName\",  \"firstBookingArrivalDate\" : \"2000-01-23\",  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"requestedPlacementArrivalDate\" : \"2000-01-23\",  \"applicationSubmittedDate\" : \"2000-01-23\",  \"isParole\" : true,  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"placementRequestStatus\" : \"matched\"} ]")
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
