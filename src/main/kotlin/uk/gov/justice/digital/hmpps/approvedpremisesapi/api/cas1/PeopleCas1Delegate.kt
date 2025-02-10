package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PeopleCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface PeopleCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PeopleCas1#getPeopleApplicationsTimeline
     */
    fun getPeopleApplicationsTimeline(crn: kotlin.String): ResponseEntity<Cas1PersonalTimeline> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"applications\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"timelineEvents\" : [ {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"payload\" : {        \"schemaVersion\" : 0,        \"premises\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        }      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    }, {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"payload\" : {        \"schemaVersion\" : 0,        \"premises\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        }      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    } ],    \"isOfflineApplication\" : true,    \"createdBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"status\" : \"started\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"timelineEvents\" : [ {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"payload\" : {        \"schemaVersion\" : 0,        \"premises\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        }      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    }, {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"payload\" : {        \"schemaVersion\" : 0,        \"premises\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        }      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    } ],    \"isOfflineApplication\" : true,    \"createdBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"status\" : \"started\"  } ]}")
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

}
