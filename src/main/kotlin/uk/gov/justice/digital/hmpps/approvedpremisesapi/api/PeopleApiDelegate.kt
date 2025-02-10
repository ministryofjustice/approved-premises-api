package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ActiveOffence
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Adjudication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskOfSeriousHarm
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysRiskToSelf
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.OASysSections
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Person
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonAcctAlert
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonRisks
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PersonalTimeline
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PrisonCaseNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link PeopleApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
interface PeopleApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see PeopleApi#peopleCrnAcctAlertsGet
     */
    fun peopleCrnAcctAlertsGet(crn: kotlin.String): ResponseEntity<List<PersonAcctAlert>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"dateCreated\" : \"2000-01-23\",  \"dateExpires\" : \"2000-01-23\",  \"description\" : \"description\",  \"comment\" : \"comment\",  \"alertId\" : 0,  \"alertTypeDescription\" : \"alertTypeDescription\"}, {  \"dateCreated\" : \"2000-01-23\",  \"dateExpires\" : \"2000-01-23\",  \"description\" : \"description\",  \"comment\" : \"comment\",  \"alertId\" : 0,  \"alertTypeDescription\" : \"alertTypeDescription\"} ]")
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
     * @see PeopleApi#peopleCrnAdjudicationsGet
     */
    fun peopleCrnAdjudicationsGet(crn: kotlin.String,
        xServiceName: ServiceName): ResponseEntity<List<Adjudication>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"reportedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"establishment\" : \"establishment\",  \"id\" : 0,  \"finding\" : \"finding\",  \"offenceDescription\" : \"Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801\",  \"hearingHeld\" : true}, {  \"reportedAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"establishment\" : \"establishment\",  \"id\" : 0,  \"finding\" : \"finding\",  \"offenceDescription\" : \"Wounding or inflicting grievous bodily harm (inflicting bodily injury with or without weapon) (S20) - 00801\",  \"hearingHeld\" : true} ]")
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
     * @see PeopleApi#peopleCrnOasysRiskToSelfGet
     */
    fun peopleCrnOasysRiskToSelfGet(crn: kotlin.String): ResponseEntity<OASysRiskToSelf> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"riskToSelf\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",  \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\",  \"assessmentId\" : 138985987,  \"assessmentState\" : \"Completed\"}")
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
     * @see PeopleApi#peopleCrnOasysRoshGet
     */
    fun peopleCrnOasysRoshGet(crn: kotlin.String): ResponseEntity<OASysRiskOfSeriousHarm> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",  \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\",  \"rosh\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"assessmentId\" : 138985987,  \"assessmentState\" : \"Completed\"}")
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
     * @see PeopleApi#peopleCrnOasysSectionsGet
     */
    fun peopleCrnOasysSectionsGet(crn: kotlin.String,
        selectedSections: kotlin.collections.List<kotlin.Int>?): ResponseEntity<OASysSections> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"roshSummary\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"riskToSelf\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"dateStarted\" : \"2000-01-23T04:56:07.000+00:00\",  \"dateCompleted\" : \"2000-01-23T04:56:07.000+00:00\",  \"offenceDetails\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ],  \"supportingInformation\" : [ {    \"answer\" : \"answer\",    \"sectionNumber\" : 0,    \"linkedToReOffending\" : true,    \"label\" : \"label\",    \"linkedToHarm\" : true,    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"sectionNumber\" : 0,    \"linkedToReOffending\" : true,    \"label\" : \"label\",    \"linkedToHarm\" : true,    \"questionNumber\" : \"questionNumber\"  } ],  \"assessmentId\" : 138985987,  \"assessmentState\" : \"Completed\",  \"riskManagementPlan\" : [ {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  }, {    \"answer\" : \"answer\",    \"label\" : \"label\",    \"questionNumber\" : \"questionNumber\"  } ]}")
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
     * @see PeopleApi#peopleCrnOasysSelectionGet
     */
    fun peopleCrnOasysSelectionGet(crn: kotlin.String): ResponseEntity<List<OASysSection>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"name\" : \"Emotional wellbeing\",  \"linkedToReOffending\" : true,  \"section\" : 10,  \"linkedToHarm\" : true}, {  \"name\" : \"Emotional wellbeing\",  \"linkedToReOffending\" : true,  \"section\" : 10,  \"linkedToHarm\" : true} ]")
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
     * @see PeopleApi#peopleCrnOffencesGet
     */
    fun peopleCrnOffencesGet(crn: kotlin.String): ResponseEntity<List<ActiveOffence>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"offenceId\" : \"M1502750438\",  \"convictionId\" : 1502724704,  \"deliusEventNumber\" : \"7\",  \"offenceDate\" : \"2000-01-23\",  \"offenceDescription\" : \"offenceDescription\"}, {  \"offenceId\" : \"M1502750438\",  \"convictionId\" : 1502724704,  \"deliusEventNumber\" : \"7\",  \"offenceDate\" : \"2000-01-23\",  \"offenceDescription\" : \"offenceDescription\"} ]")
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
     * @see PeopleApi#peopleCrnPrisonCaseNotesGet
     */
    fun peopleCrnPrisonCaseNotesGet(crn: kotlin.String,
        xServiceName: ServiceName): ResponseEntity<List<PrisonCaseNote>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"note\" : \"note\",  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"authorName\" : \"authorName\",  \"subType\" : \"subType\",  \"id\" : \"id\",  \"sensitive\" : true,  \"type\" : \"type\"}, {  \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"note\" : \"note\",  \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",  \"authorName\" : \"authorName\",  \"subType\" : \"subType\",  \"id\" : \"id\",  \"sensitive\" : true,  \"type\" : \"type\"} ]")
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
     * @see PeopleApi#peopleCrnRisksGet
     */
    fun peopleCrnRisksGet(crn: kotlin.String): ResponseEntity<PersonRisks> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"mappa\" : {    \"value\" : {      \"lastUpdated\" : \"2000-01-23\",      \"level\" : \"level\"    }  },  \"tier\" : {    \"value\" : {      \"lastUpdated\" : \"2000-01-23\",      \"level\" : \"level\"    }  },  \"roshRisks\" : {    \"value\" : {      \"lastUpdated\" : \"2000-01-23\",      \"overallRisk\" : \"overallRisk\",      \"riskToChildren\" : \"riskToChildren\",      \"riskToPublic\" : \"riskToPublic\",      \"riskToKnownAdult\" : \"riskToKnownAdult\",      \"riskToStaff\" : \"riskToStaff\"    },    \"status\" : \"retrieved\"  },  \"flags\" : {    \"value\" : [ \"value\", \"value\" ]  },  \"crn\" : \"crn\"}")
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
     * @see PeopleApi#peopleCrnTimelineGet
     */
    fun peopleCrnTimelineGet(crn: kotlin.String): ResponseEntity<PersonalTimeline> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"person\" : {    \"type\" : \"FullPerson\",    \"crn\" : \"crn\"  },  \"applications\" : [ {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"timelineEvents\" : [ {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"approved_premises_application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    }, {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"approved_premises_application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    } ],    \"isOfflineApplication\" : true,    \"createdBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"status\" : \"started\"  }, {    \"createdAt\" : \"2000-01-23T04:56:07.000+00:00\",    \"timelineEvents\" : [ {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"approved_premises_application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    }, {      \"occurredAt\" : \"2000-01-23T04:56:07.000+00:00\",      \"createdBy\" : {        \"telephoneNumber\" : \"telephoneNumber\",        \"service\" : \"service\",        \"deliusUsername\" : \"deliusUsername\",        \"name\" : \"name\",        \"probationDeliveryUnit\" : {          \"name\" : \"name\",          \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"        },        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",        \"isActive\" : true,        \"region\" : {          \"name\" : \"NPS North East Central Referrals\",          \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"        },        \"email\" : \"email\"      },      \"associatedUrls\" : [ {        \"type\" : \"application\",        \"url\" : \"url\"      }, {        \"type\" : \"application\",        \"url\" : \"url\"      } ],      \"id\" : \"id\",      \"type\" : \"approved_premises_application_submitted\",      \"triggerSource\" : \"triggerSource\",      \"content\" : \"content\"    } ],    \"isOfflineApplication\" : true,    \"createdBy\" : {      \"telephoneNumber\" : \"telephoneNumber\",      \"service\" : \"service\",      \"deliusUsername\" : \"deliusUsername\",      \"name\" : \"name\",      \"probationDeliveryUnit\" : {        \"name\" : \"name\",        \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\"      },      \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",      \"isActive\" : true,      \"region\" : {        \"name\" : \"NPS North East Central Referrals\",        \"id\" : \"952790c0-21d7-4fd6-a7e1-9018f08d8bb0\"      },      \"email\" : \"email\"    },    \"id\" : \"046b6c7f-0b8a-43b9-b35d-6489e6daee91\",    \"status\" : \"started\"  } ]}")
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
     * @see PeopleApi#peopleSearchGet
     */
    fun peopleSearchGet(crn: kotlin.String): ResponseEntity<Person> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"type\" : \"FullPerson\",  \"crn\" : \"crn\"}")
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
