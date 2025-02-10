package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewOutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1NewOutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedCancellation
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas1OutOfServiceBedSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Temporality
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas1OutOfServiceBed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link OutOfServiceBedsCas1Controller}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface OutOfServiceBedsCas1Delegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see OutOfServiceBedsCas1#cancelOutOfServiceBed
     */
    fun cancelOutOfServiceBed(premisesId: java.util.UUID,
        outOfServiceBedId: java.util.UUID,
        body: Cas1NewOutOfServiceBedCancellation): ResponseEntity<Cas1OutOfServiceBedCancellation> {
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
     * @see OutOfServiceBedsCas1#createOutOfServiceBed
     */
    fun createOutOfServiceBed(premisesId: java.util.UUID,
        body: Cas1NewOutOfServiceBed): ResponseEntity<Cas1OutOfServiceBed> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"}")
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
     * @see OutOfServiceBedsCas1#getOutOfServiceBed
     */
    fun getOutOfServiceBed(premisesId: java.util.UUID,
        outOfServiceBedId: java.util.UUID): ResponseEntity<Cas1OutOfServiceBed> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"}")
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
     * @see OutOfServiceBedsCas1#getOutOfServiceBeds
     */
    fun getOutOfServiceBeds(temporality: kotlin.collections.List<Temporality>?,
        premisesId: java.util.UUID?,
        apAreaId: java.util.UUID?,
        sortDirection: SortDirection?,
        sortBy: Cas1OutOfServiceBedSortField?,
        page: kotlin.Int?,
        perPage: kotlin.Int?): ResponseEntity<List<Cas1OutOfServiceBed>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"}, {  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"} ]")
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
     * @see OutOfServiceBedsCas1#getOutOfServiceBedsForPremises
     */
    fun getOutOfServiceBedsForPremises(premisesId: java.util.UUID): ResponseEntity<List<Cas1OutOfServiceBed>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"}, {  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"} ]")
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
     * @see OutOfServiceBedsCas1#updateOutOfServiceBed
     */
    fun updateOutOfServiceBed(premisesId: java.util.UUID,
        outOfServiceBedId: java.util.UUID,
        body: UpdateCas1OutOfServiceBed): ResponseEntity<Cas1OutOfServiceBed> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"bed\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"revisionHistory\" : [ {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  }, {    \"reason\" : {      \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true    },    \"revisionType\" : [ \"created\", \"created\" ],    \"updatedBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"notes\" : \"notes\",    \"endDate\" : \"2000-01-23\",    \"referenceNumber\" : \"referenceNumber\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"startDate\" : \"2000-01-23\",    \"updatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  } ],  \"reason\" : {    \"name\" : \"Double Room with Single Occupancy - Other (Non-FM)\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"isActive\" : true  },  \"notes\" : \"notes\",  \"endDate\" : \"2000-01-23\",  \"temporality\" : \"past\",  \"room\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"apArea\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"cancellation\" : \"\",  \"referenceNumber\" : \"referenceNumber\",  \"premises\" : {    \"name\" : \"name\",    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"  },  \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",  \"startDate\" : \"2000-01-23\",  \"daysLostCount\" : 0,  \"status\" : \"active\"}")
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

}
