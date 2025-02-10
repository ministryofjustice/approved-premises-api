package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchParameters
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1SpaceSearchResults
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link SpaceSearchesCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface SpaceSearchesCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see SpaceSearchesCas1#spaceSearch
     */
    fun spaceSearch(cas1SpaceSearchParameters: Cas1SpaceSearchParameters): ResponseEntity<Cas1SpaceSearchResults> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"searchCriteria\" : {    \"targetPostcodeDistrict\" : \"SE5\",    \"durationInDays\" : 84,    \"requirements\" : {      \"apTypes\" : [ \"normal\", \"normal\" ],      \"genders\" : [ \"male\", \"male\" ],      \"spaceCharacteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ]    },    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\"  },  \"resultsCount\" : 4,  \"results\" : [ {    \"spacesAvailable\" : [ {      \"durationInDays\" : 77,      \"spaceCharacteristics\" : [ null, null ]    }, {      \"durationInDays\" : 77,      \"spaceCharacteristics\" : [ null, null ]    } ],    \"distanceInMiles\" : 2.1,    \"premises\" : {      \"premisesCharacteristics\" : [ {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      }, {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      } ],      \"characteristics\" : [ null, null ],      \"apArea\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"town\" : \"Braintree\",      \"name\" : \"Hope House\",      \"fullAddress\" : \"fullAddress\",      \"postcode\" : \"LS1 3AD\",      \"addressLine1\" : \"1 The Street\",      \"addressLine2\" : \"Blackmore End\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  }, {    \"spacesAvailable\" : [ {      \"durationInDays\" : 77,      \"spaceCharacteristics\" : [ null, null ]    }, {      \"durationInDays\" : 77,      \"spaceCharacteristics\" : [ null, null ]    } ],    \"distanceInMiles\" : 2.1,    \"premises\" : {      \"premisesCharacteristics\" : [ {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      }, {        \"propertyName\" : \"propertyName\",        \"name\" : \"name\"      } ],      \"characteristics\" : [ null, null ],      \"apArea\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"town\" : \"Braintree\",      \"name\" : \"Hope House\",      \"fullAddress\" : \"fullAddress\",      \"postcode\" : \"LS1 3AD\",      \"addressLine1\" : \"1 The Street\",      \"addressLine2\" : \"Blackmore End\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"    }  } ]}")
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
