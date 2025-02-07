package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Appeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Application
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSortField
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApprovedPremisesApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewAppeal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewApplicationTimelineNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewWithdrawal
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Problem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ReleaseTypeOption
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.RequestForPlacement
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SortDirection
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SubmitApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.TimelineEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ValidationError
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawable
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Withdrawables

@RestController
interface ApplicationsApi {

  fun getDelegate(): ApplicationsApiDelegate = object : ApplicationsApiDelegate {}

  @Operation(
    tags = ["Operations on all applications"],
    summary = "Lists all applications that any user has created",
    operationId = "applicationsAllGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ApplicationSummary::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/all"],
    produces = ["application/json"],
  )
  fun applicationsAllGet(
    @Parameter(
      description = "Only approved premises anything else gets a 400",
      `in` = ParameterIn.HEADER,
      required = true,
      schema = Schema(
        allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"],
      ),
    ) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName,
    @Parameter(description = "Page number of results to return. If blank, returns all results") @RequestParam(
      value = "page",
      required = false,
    ) page: kotlin.Int?,
    @Parameter(description = "CRN of applications") @RequestParam(
      value = "crnOrName",
      required = false,
    ) crnOrName: kotlin.String?,
    @Parameter(
      description = "The direction to sort the results by. If blank, will sort in descending order",
      schema = Schema(
        allowableValues = ["asc", "desc"],
      ),
    ) @RequestParam(value = "sortDirection", required = false) sortDirection: SortDirection?,
    @Parameter(description = "Application statuses to filter on. If none provided, all will be returned") @RequestParam(
      value = "status",
      required = false,
    ) status: List<ApprovedPremisesApplicationStatus>?,
    @Parameter(
      description = "The field to sort the results by.",
      schema = Schema(
        allowableValues = ["tier", "createdAt", "arrivalDate", "releaseType"],
      ),
    ) @RequestParam(value = "sortBy", required = false) sortBy: ApplicationSortField?,
    @Parameter(description = "Approved Premises Area ID to filter results by") @RequestParam(
      value = "apAreaId",
      required = false,
    ) apAreaId: java.util.UUID?,
    @Parameter(
      description = "If provided, restricts the results to only those with the given release type.",
      schema = Schema(
        allowableValues = ["licence", "rotl", "hdc", "pss", "in_community", "not_applicable", "extendedDeterminateLicence", "paroleDirectedLicence", "reReleasedPostRecall"],
      ),
    ) @RequestParam(value = "releaseType", required = false) releaseType: ReleaseTypeOption?,
  ): ResponseEntity<List<ApplicationSummary>> {
    return getDelegate().applicationsAllGet(
      xServiceName,
      page,
      crnOrName,
      sortDirection,
      status,
      sortBy,
      apAreaId,
      releaseType,
    )
  }

  @Operation(
    tags = ["Application data"],
    summary = "Get an appeal on an application",
    operationId = "applicationsApplicationIdAppealsAppealIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Appeal::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid applicationId or appealId",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/appeals/{appealId}"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdAppealsAppealIdGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "ID of the appeal",
      required = true,
    ) @PathVariable("appealId") appealId: java.util.UUID,
  ): ResponseEntity<Appeal> {
    return getDelegate().applicationsApplicationIdAppealsAppealIdGet(applicationId, appealId)
  }

  @Operation(
    tags = ["Application data"],
    summary = "Add an appeal to an application",
    operationId = "applicationsApplicationIdAppealsPost",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Appeal::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid applicationId",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/applications/{applicationId}/appeals"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun applicationsApplicationIdAppealsPost(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "the appeal to add",
      required = true,
    ) @RequestBody body: NewAppeal,
  ): ResponseEntity<Appeal> {
    return getDelegate().applicationsApplicationIdAppealsPost(applicationId, body)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Get the assessment for an application",
    operationId = "applicationsApplicationIdAssessmentGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Assessment::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/assessment"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdAssessmentGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
  ): ResponseEntity<Assessment> {
    return getDelegate().applicationsApplicationIdAssessmentGet(applicationId)
  }

  @Operation(
    tags = ["Application data"],
    summary = "Returns meta info on documents at the person level or at the Conviction level for the index Offence of this application.",
    operationId = "applicationsApplicationIdDocumentsGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Document::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid CRN",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/documents"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdDocumentsGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
  ): ResponseEntity<List<Document>> {
    return getDelegate().applicationsApplicationIdDocumentsGet(applicationId)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Gets a single application by its ID",
    operationId = "applicationsApplicationIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Application::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
  ): ResponseEntity<Application> {
    return getDelegate().applicationsApplicationIdGet(applicationId)
  }

  @Operation(
    tags = ["Add a note on applications"],
    summary = "Add a note on applications",
    operationId = "applicationsApplicationIdNotesPost",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = ApplicationTimelineNote::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid applicationId",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/applications/{applicationId}/notes"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun applicationsApplicationIdNotesPost(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "the note to add",
      required = true,
    ) @RequestBody body: NewApplicationTimelineNote,
  ): ResponseEntity<ApplicationTimelineNote> {
    return getDelegate().applicationsApplicationIdNotesPost(applicationId, body)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Updates an application",
    operationId = "applicationsApplicationIdPut",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Application::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.PUT],
    value = ["/applications/{applicationId}"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun applicationsApplicationIdPut(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "Information to update the application with",
      required = true,
    ) @RequestBody body: UpdateApplication,
  ): ResponseEntity<Application> {
    return getDelegate().applicationsApplicationIdPut(applicationId, body)
  }

  @Operation(
    tags = ["default"],
    summary = "Returns a list of Requests for Placement for the given application.",
    operationId = "applicationsApplicationIdRequestsForPlacementGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = RequestForPlacement::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid CRN",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/requests-for-placement"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdRequestsForPlacementGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
  ): ResponseEntity<List<RequestForPlacement>> {
    return getDelegate().applicationsApplicationIdRequestsForPlacementGet(applicationId)
  }

  @Operation(
    tags = ["default"],
    summary = "Returns the specified Request for Placement.",
    operationId = "applicationsApplicationIdRequestsForPlacementRequestForPlacementIdGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = RequestForPlacement::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid CRN",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/requests-for-placement/{requestForPlacementId}"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdRequestsForPlacementRequestForPlacementIdGet(
    @Parameter(
      description = "ID of the application which the Request for Placement enacts.",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "ID of the Request for Placement.",
      required = true,
    ) @PathVariable("requestForPlacementId") requestForPlacementId: java.util.UUID,
  ): ResponseEntity<RequestForPlacement> {
    return getDelegate().applicationsApplicationIdRequestsForPlacementRequestForPlacementIdGet(
      applicationId,
      requestForPlacementId,
    )
  }

  @Operation(
    tags = ["Application data"],
    summary = "Submits an Application",
    operationId = "applicationsApplicationIdSubmissionPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successfully submitted the application"),
      ApiResponse(
        responseCode = "400",
        description = "application has already been submitted",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/applications/{applicationId}/submission"],
    produces = ["application/problem+json", "application/json"],
    consumes = ["application/json"],
  )
  fun applicationsApplicationIdSubmissionPost(
    @Parameter(
      description = "Id of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "Information needed to submit an application",
      required = true,
    ) @RequestBody submitApplication: SubmitApplication,
  ): ResponseEntity<Unit> {
    return getDelegate().applicationsApplicationIdSubmissionPost(applicationId, submitApplication)
  }

  @Operation(
    tags = ["Application data timeline"],
    summary = "Returns domain event summary",
    operationId = "applicationsApplicationIdTimelineGet",
    description = """deprecated, use /cas1/applications/{applicationId}/timeline""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = TimelineEvent::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid CRN",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/timeline"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdTimelineGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "If given, only users for this service will be returned",
      `in` = ParameterIn.HEADER,
      required = true,
      schema = Schema(
        allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"],
      ),
    ) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName,
  ): ResponseEntity<List<TimelineEvent>> {
    return getDelegate().applicationsApplicationIdTimelineGet(applicationId, xServiceName)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Returns a list of withdrawable items associated with this application, including the application itself, if withdrawable",
    operationId = "applicationsApplicationIdWithdrawablesGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = Withdrawable::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/withdrawables"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdWithdrawablesGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "If given, only users for this service will be returned",
      `in` = ParameterIn.HEADER,
      required = true,
      schema = Schema(
        allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"],
      ),
    ) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName,
  ): ResponseEntity<List<Withdrawable>> {
    return getDelegate().applicationsApplicationIdWithdrawablesGet(applicationId, xServiceName)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Returns a list of withdrawable items associated with this application, including the application itself, if withdrawable",
    operationId = "applicationsApplicationIdWithdrawablesWithNotesGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Withdrawables::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications/{applicationId}/withdrawablesWithNotes"],
    produces = ["application/json"],
  )
  fun applicationsApplicationIdWithdrawablesWithNotesGet(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "If given, only users for this service will be returned",
      `in` = ParameterIn.HEADER,
      required = true,
      schema = Schema(
        allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"],
      ),
    ) @RequestHeader(value = "X-Service-Name", required = true) xServiceName: ServiceName,
  ): ResponseEntity<Withdrawables> {
    return getDelegate().applicationsApplicationIdWithdrawablesWithNotesGet(applicationId, xServiceName)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Withdraws an application with a reason",
    operationId = "applicationsApplicationIdWithdrawalPost",
    description = """""",
    responses = [
      ApiResponse(responseCode = "200", description = "successful operation"),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid applicationId",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/applications/{applicationId}/withdrawal"],
    produces = ["application/json"],
    consumes = ["application/json"],
  )
  fun applicationsApplicationIdWithdrawalPost(
    @Parameter(
      description = "ID of the application",
      required = true,
    ) @PathVariable("applicationId") applicationId: java.util.UUID,
    @Parameter(
      description = "details of the withdrawal",
      required = true,
    ) @RequestBody body: NewWithdrawal,
  ): ResponseEntity<Unit> {
    return getDelegate().applicationsApplicationIdWithdrawalPost(applicationId, body)
  }

  @Operation(
    tags = ["Operations on all applications"],
    summary = "Lists all applications that the user has created",
    operationId = "applicationsGet",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "successful operation",
        content = [Content(array = ArraySchema(schema = Schema(implementation = ApplicationSummary::class)))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    value = ["/applications"],
    produces = ["application/json"],
  )
  fun applicationsGet(
    @Parameter(
      description = "Which service to get applications for, defaults to approved-premises",
      `in` = ParameterIn.HEADER,
      schema = Schema(
        allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"],
      ),
    ) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
  ): ResponseEntity<List<ApplicationSummary>> {
    return getDelegate().applicationsGet(xServiceName)
  }

  @Operation(
    tags = ["Operations on applications"],
    summary = "Creates an application",
    operationId = "applicationsPost",
    description = """""",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "successful operation",
        content = [Content(schema = Schema(implementation = Application::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "invalid params",
        content = [Content(schema = Schema(implementation = ValidationError::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "not authenticated",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "unauthorised",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "invalid CRN",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
      ApiResponse(
        responseCode = "500",
        description = "unexpected error",
        content = [Content(schema = Schema(implementation = Problem::class))],
      ),
    ],
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    value = ["/applications"],
    produces = ["application/json", "application/problem+json"],
    consumes = ["application/json"],
  )
  fun applicationsPost(
    @Parameter(
      description = "Information to create a blank application with",
      required = true,
    ) @RequestBody body: NewApplication,
    @Parameter(
      description = "Which service the application will belong to, defaults to approved-premises",
      `in` = ParameterIn.HEADER,
      schema = Schema(
        allowableValues = ["approved-premises", "cas2", "cas2v2", "temporary-accommodation"],
      ),
    ) @RequestHeader(value = "X-Service-Name", required = false) xServiceName: ServiceName?,
    @Parameter(description = "Instructs the API to create and return risk information from the OASys API (defaults to true)") @RequestParam(
      value = "createWithRisks",
      required = false,
    ) createWithRisks: kotlin.Boolean?,
  ): ResponseEntity<Application> {
    return getDelegate().applicationsPost(body, xServiceName, createWithRisks)
  }
}
