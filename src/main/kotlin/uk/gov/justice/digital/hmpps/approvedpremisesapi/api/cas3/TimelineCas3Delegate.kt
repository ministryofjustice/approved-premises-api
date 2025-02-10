package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReferralHistoryNote
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link TimelineCas3Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface TimelineCas3Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see TimelineCas3#getTimelineEntries
     */
    fun getTimelineEntries(assessmentId: java.util.UUID): ResponseEntity<List<ReferralHistoryNote>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserName\" : \"createdByUserName\",  \"messageDetails\" : {    \"isWithdrawn\" : true,    \"rejectionReason\" : \"rejectionReason\",    \"domainEvent\" : \"{}\",    \"rejectionReasonDetails\" : \"rejectionReasonDetails\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"message\" : \"message\",  \"type\" : \"type\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"createdByUserName\" : \"createdByUserName\",  \"messageDetails\" : {    \"isWithdrawn\" : true,    \"rejectionReason\" : \"rejectionReason\",    \"domainEvent\" : \"{}\",    \"rejectionReasonDetails\" : \"rejectionReasonDetails\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"message\" : \"message\",  \"type\" : \"type\"} ]")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
