package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationStatusUpdatedEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas2.model.Cas2ApplicationSubmittedEvent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link CAS2EventsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface CAS2EventsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see CAS2EventsApi#eventsCas2ApplicationStatusUpdatedEventIdGet
     */
    fun eventsCas2ApplicationStatusUpdatedEventIdGet(eventId: java.util.UUID): ResponseEntity<Cas2ApplicationStatusUpdatedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"updatedBy\" : {      \"origin\" : \"NACRO\",      \"name\" : \"Roger Smith\",      \"email\" : \"roger@external.example.com\",      \"username\" : \"CAS2_ASSESSOR_USER\"    },    \"newStatus\" : {      \"name\" : \"moreInfoRequested\",      \"description\" : \"More information about the application has been requested from the POM (Prison Offender Manager).\",      \"statusDetails\" : [ {        \"name\" : \"changeOfCircumstances\",        \"label\" : \"Change of Circumstances\"      }, {        \"name\" : \"changeOfCircumstances\",        \"label\" : \"Change of Circumstances\"      } ],      \"label\" : \"More information requested\"    },    \"applicationUrl\" : \"https://community-accommodation-tier-2-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"applications.cas2.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see CAS2EventsApi#eventsCas2ApplicationSubmittedEventIdGet
     */
    fun eventsCas2ApplicationSubmittedEventIdGet(eventId: java.util.UUID): ResponseEntity<Cas2ApplicationSubmittedEvent> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"submittedBy\" : {      \"staffMember\" : {        \"name\" : \"John Smith\",        \"usertype\" : \"nomis\",        \"staffIdentifier\" : 1501234567,        \"username\" : \"SMITHJ_GEN\"      }    },    \"cas2v2ApplicationOrigin\" : \"cas2v2ApplicationOrigin\",    \"bailHearingDate\" : \"2023-03-30\",    \"preferredAreas\" : \"Leeds | Bradford\",    \"applicationUrl\" : \"https://community-accommodation-tier-2-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"conditionalReleaseDate\" : \"2023-04-30\",    \"applicationId\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"personReference\" : {      \"noms\" : \"noms\",      \"crn\" : \"crn\"    },    \"hdcEligibilityDate\" : \"2023-03-30\",    \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"referringPrisonCode\" : \"BRI\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"applications.cas2.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
