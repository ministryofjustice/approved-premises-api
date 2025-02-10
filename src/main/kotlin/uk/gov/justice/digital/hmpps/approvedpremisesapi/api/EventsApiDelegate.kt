package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationExpiredEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationSubmittedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.ApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingCancelledEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingChangedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingExtendedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingKeyWorkerAssignedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.BookingNotMadeEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.FurtherInformationRequestedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.MatchRequestWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonDepartedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonNotArrivedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationAllocatedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PlacementApplicationWithdrawnEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementAssessedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.RequestForPlacementCreatedEnvelope
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link EventsApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface EventsApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see EventsApi#eventsApplicationAssessedEventIdGet
     */
    fun eventsApplicationAssessedEventIdGet(eventId: java.util.UUID): ResponseEntity<ApplicationAssessedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"decision\" : \"Rejected\",    \"assessedBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"cru\" : {        \"name\" : \"NPS North East\"      },      \"probationArea\" : {        \"code\" : \"N02\",        \"name\" : \"NPS North East\"      }    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"assessedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"decisionRationale\" : \"Risk too low\",    \"arrivalDate\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsApplicationExpiredEventIdGet
     */
    fun eventsApplicationExpiredEventIdGet(eventId: java.util.UUID): ResponseEntity<ApplicationExpiredEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"updatedStatus\" : \"updatedStatus\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"previousStatus\" : \"previousStatus\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsApplicationSubmittedEventIdGet
     */
    fun eventsApplicationSubmittedEventIdGet(eventId: java.util.UUID): ResponseEntity<ApplicationSubmittedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"mappa\" : \"CAT C3/LEVEL L2\",    \"offenceId\" : \"AB43782\",    \"submittedBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"team\" : {        \"code\" : \"N54NGH\",        \"name\" : \"Gateshead 1\"      },      \"probationArea\" : {        \"code\" : \"N02\",        \"name\" : \"NPS North East\"      },      \"ldu\" : {        \"code\" : \"N54PPU\",        \"name\" : \"Public Protection NE\"      },      \"region\" : {        \"code\" : \"NE\",        \"name\" : \"North East\"      }    },    \"gender\" : \"Male\",    \"deliusEventNumber\" : \"7\",    \"targetLocation\" : \"LS2\",    \"releaseType\" : \"rotl\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"submittedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"age\" : 43,    \"sentenceLengthInMonths\" : 57  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsApplicationWithdrawnEventIdGet
     */
    fun eventsApplicationWithdrawnEventIdGet(eventId: java.util.UUID): ResponseEntity<ApplicationWithdrawnEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"otherWithdrawalReason\" : \"otherWithdrawalReason\",    \"withdrawnAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"withdrawnBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"probationArea\" : {        \"code\" : \"N02\",        \"name\" : \"NPS North East\"      }    },    \"withdrawalReason\" : \"withdrawalReason\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    }  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsAssessmentAllocatedEventIdGet
     */
    fun eventsAssessmentAllocatedEventIdGet(eventId: java.util.UUID): ResponseEntity<AssessmentAllocatedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"allocatedTo\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"allocatedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"assessmentId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"assessmentUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/assessments/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"allocatedAt\" : \"2000-01-23T04:56:07.000+00:00\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsAssessmentAppealedEventIdGet
     */
    fun eventsAssessmentAppealedEventIdGet(eventId: java.util.UUID): ResponseEntity<AssessmentAppealedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"appealDetail\" : \"appealDetail\",    \"decision\" : \"accepted\",    \"createdBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"appealId\" : \"dd450bbc-162d-4380-a103-9f261943b98f\",    \"appealUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713/appeals/dd450bbc-162d-4380-a103-9f261943b98f\",    \"decisionDetail\" : \"decisionDetail\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsBookingCancelledEventIdGet
     */
    fun eventsBookingCancelledEventIdGet(eventId: java.util.UUID): ResponseEntity<BookingCancelledEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"cancellationReason\" : \"cancellationReason\",    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"cancelledAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"deliusEventNumber\" : \"7\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"cancellationRecordedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"cancelledBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"cancelledAtDate\" : \"2000-01-23\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsBookingChangedEventIdGet
     */
    fun eventsBookingChangedEventIdGet(eventId: java.util.UUID): ResponseEntity<BookingChangedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"characteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],    \"departureOn\" : \"2023-04-30\",    \"deliusEventNumber\" : \"7\",    \"previousDepartureOn\" : \"2023-01-30\",    \"previousCharacteristics\" : [ null, null ],    \"arrivalOn\" : \"2023-01-30\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"changedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"changedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"previousArrivalOn\" : \"2023-01-30\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsBookingExtendedEventIdGet
     */
    fun eventsBookingExtendedEventIdGet(eventId: java.util.UUID): ResponseEntity<BookingExtendedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"extendedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"deliusEventNumber\" : \"7\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"extensionReason\" : \"There has been a flood at the housing association to which Mr Smith is moving\",    \"previousDepartureOn\" : \"2023-01-30\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"extendedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"newDepartureOn\" : \"2023-02-12\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsBookingKeyworkerAssignedEventIdGet
     */
    fun eventsBookingKeyworkerAssignedEventIdGet(eventId: java.util.UUID): ResponseEntity<BookingKeyWorkerAssignedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"assignedKeyWorkerName\" : \"assignedKeyWorkerName\",    \"previousKeyWorkerName\" : \"previousKeyWorkerName\",    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"keyWorker\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"departureDate\" : \"2023-02-12\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"arrivalDate\" : \"2023-02-12\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsBookingMadeEventIdGet
     */
    fun eventsBookingMadeEventIdGet(eventId: java.util.UUID): ResponseEntity<BookingMadeEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"characteristics\" : [ \"acceptsChildSexOffenders\", \"acceptsChildSexOffenders\" ],    \"departureOn\" : \"2023-04-30\",    \"deliusEventNumber\" : \"7\",    \"arrivalOn\" : \"2023-01-30\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"releaseType\" : \"releaseType\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"bookedBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"cru\" : {        \"name\" : \"NPS North East\"      }    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationSubmittedOn\" : \"2000-01-23T04:56:07.000+00:00\",    \"sentenceType\" : \"sentenceType\",    \"situation\" : \"situation\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsBookingNotMadeEventIdGet
     */
    fun eventsBookingNotMadeEventIdGet(eventId: java.util.UUID): ResponseEntity<BookingNotMadeEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"attemptedBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"cru\" : {        \"name\" : \"NPS North East\"      }    },    \"deliusEventNumber\" : \"7\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"attemptedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"failureDescription\" : \"No availability\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsFurtherInformationRequestedEventIdGet
     */
    fun eventsFurtherInformationRequestedEventIdGet(eventId: java.util.UUID): ResponseEntity<FurtherInformationRequestedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"requester\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"requestedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"requestId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"recipient\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"assessmentId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"assessmentUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/assessments/484b8b5e-6c3b-4400-b200-425bbe410713\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsMatchRequestWithdrawnEventIdGet
     */
    fun eventsMatchRequestWithdrawnEventIdGet(eventId: java.util.UUID): ResponseEntity<MatchRequestWithdrawnEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"withdrawnAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"withdrawnBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"probationArea\" : {        \"code\" : \"N02\",        \"name\" : \"NPS North East\"      }    },    \"withdrawalReason\" : \"RELATED_APPLICATION_WITHDRAWN\",    \"datePeriod\" : {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"matchRequestId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"requestIsForApplicationsArrivalDate\" : true  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsPersonArrivedEventIdGet
     */
    fun eventsPersonArrivedEventIdGet(eventId: java.util.UUID): ResponseEntity<PersonArrivedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"recordedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"notes\" : \"Arrived a day late due to rail strike. Informed in advance by COM.\",    \"arrivedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"deliusEventNumber\" : \"7\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"expectedDepartureOn\" : \"2023-02-28\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"applicationSubmittedOn\" : \"2022-08-21\",    \"previousExpectedDepartureOn\" : \"2023-02-28\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsPersonDepartedEventIdGet
     */
    fun eventsPersonDepartedEventIdGet(eventId: java.util.UUID): ResponseEntity<PersonDepartedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"reason\" : \"Arrested, remanded in custody, or sentenced\",    \"recordedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"deliusEventNumber\" : \"7\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"destination\" : {      \"destinationProvider\" : {        \"description\" : \"Ext - North East Region\",        \"id\" : \"f0703382-3e8f-49ff-82bc-b970c9fe1b35\"      },      \"moveOnCategory\" : {        \"description\" : \"B&B / Temp / Short-Term Housing\",        \"id\" : \"a3c3d3df-1e27-4ee5-aef6-8a0f0471075f\",        \"legacyMoveOnCategoryCode\" : \"MC05\"      },      \"premises\" : {        \"legacyApCode\" : \"Q061\",        \"name\" : \"New Place\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"apCode\" : \"NENEW1\",        \"probationArea\" : {          \"code\" : \"N02\",          \"name\" : \"NPS North East\"        }      }    },    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"departedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"legacyReasonCode\" : \"Q\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsPersonNotArrivedEventIdGet
     */
    fun eventsPersonNotArrivedEventIdGet(eventId: java.util.UUID): ResponseEntity<PersonNotArrivedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"reason\" : \"Arrested, remanded in custody, or sentenced\",    \"recordedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"notes\" : \"We learnt that Mr Smith is in hospital.\",    \"premises\" : {      \"legacyApCode\" : \"Q057\",      \"name\" : \"Hope House\",      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"apCode\" : \"NEHOPE1\",      \"localAuthorityAreaName\" : \"localAuthorityAreaName\"    },    \"deliusEventNumber\" : \"7\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"bookingId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"expectedArrivalOn\" : \"2022-11-29\",    \"legacyReasonCode\" : \"Q\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsPlacementApplicationAllocatedEventIdGet
     */
    fun eventsPlacementApplicationAllocatedEventIdGet(eventId: java.util.UUID): ResponseEntity<PlacementApplicationAllocatedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"allocatedTo\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"allocatedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"placementApplicationId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"allocatedAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"placementDates\" : [ {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    }, {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    } ]  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsPlacementApplicationWithdrawnEventIdGet
     */
    fun eventsPlacementApplicationWithdrawnEventIdGet(eventId: java.util.UUID): ResponseEntity<PlacementApplicationWithdrawnEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"withdrawnAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"withdrawnBy\" : {      \"staffMember\" : {        \"staffCode\" : \"N54A999\",        \"surname\" : \"Smith\",        \"forenames\" : \"John\",        \"username\" : \"JohnSmithNPS\"      },      \"probationArea\" : {        \"code\" : \"N02\",        \"name\" : \"NPS North East\"      }    },    \"withdrawalReason\" : \"RELATED_APPLICATION_WITHDRAWN\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    },    \"placementApplicationId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\",    \"placementDates\" : [ {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    }, {      \"endDate\" : \"2000-01-23\",      \"startDate\" : \"2000-01-23\"    } ]  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsRequestForPlacementAssessedEventIdGet
     */
    fun eventsRequestForPlacementAssessedEventIdGet(eventId: java.util.UUID): ResponseEntity<RequestForPlacementAssessedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"duration\" : 7,    \"expectedArrival\" : \"2023-01-30\",    \"decision\" : \"accepted\",    \"assessedBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"decisionSummary\" : \"the decision was to accept\",    \"placementApplicationId\" : \"14c80733-4b6d-4f35-b724-66955aac320c\"  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
     * @see EventsApi#eventsRequestForPlacementCreatedEventIdGet
     */
    fun eventsRequestForPlacementCreatedEventIdGet(eventId: java.util.UUID): ResponseEntity<RequestForPlacementCreatedEnvelope> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"eventDetails\" : {    \"duration\" : 7,    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"expectedArrival\" : \"2023-01-30\",    \"createdBy\" : {      \"staffCode\" : \"N54A999\",      \"surname\" : \"Smith\",      \"forenames\" : \"John\",      \"username\" : \"JohnSmithNPS\"    },    \"applicationUrl\" : \"https://approved-premises-dev.hmpps.service.justice.gov.uk/applications/484b8b5e-6c3b-4400-b200-425bbe410713\",    \"deliusEventNumber\" : \"7\",    \"requestForPlacementId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"requestForPlacementType\" : \"initial\",    \"applicationId\" : \"484b8b5e-6c3b-4400-b200-425bbe410713\",    \"personReference\" : {      \"noms\" : \"A1234ZX\",      \"crn\" : \"C123456\"    }  },  \"id\" : \"364145f9-0af8-488e-9901-b4c46cd9ba37\",  \"eventType\" : \"approved-premises.application.submitted\",  \"timestamp\" : \"2000-01-23T04:56:07.000+00:00\"}")
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
