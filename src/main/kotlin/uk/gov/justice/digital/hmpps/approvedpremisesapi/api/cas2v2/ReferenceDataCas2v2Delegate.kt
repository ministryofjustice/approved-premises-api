package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link ReferenceDataCas2v2Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface ReferenceDataCas2v2Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see ReferenceDataCas2v2#referenceDataApplicationStatusGet
     */
    fun referenceDataApplicationStatusGet(): ResponseEntity<List<Cas2ApplicationStatus>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"moreInfoRequested\",  \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",  \"statusDetails\" : [ {    \"name\" : \"changeOfCircumstances\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"Change of Circumstances\"  }, {    \"name\" : \"changeOfCircumstances\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"Change of Circumstances\"  } ],  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"label\" : \"More information requested\"}, {  \"name\" : \"moreInfoRequested\",  \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",  \"statusDetails\" : [ {    \"name\" : \"changeOfCircumstances\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"Change of Circumstances\"  }, {    \"name\" : \"changeOfCircumstances\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"label\" : \"Change of Circumstances\"  } ],  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"label\" : \"More information requested\"} ]")
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
