package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2v2

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2.AssessmentsCas2v2Delegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.*
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2ApplicationNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2AssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2StatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2.Cas2v2AssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Service("Cas2v2AssessmentsController")
class Cas2v2AssessmentsController(
  private val cas2v2AssessmentService: Cas2v2AssessmentService,
  private val cas2v2ApplicationNoteService: Cas2v2ApplicationNoteService,
  private val cas2v2AssessmentsTransformer: Cas2v2AssessmentsTransformer,
  private val applicationNotesTransformer: ApplicationNotesTransformer,
  private val cas2v2StatusUpdateService: Cas2v2StatusUpdateService,
  private val externalUserService: ExternalUserService,
) : AssessmentsCas2v2Delegate {

  override fun assessmentsAssessmentIdGet(assessmentId: UUID): ResponseEntity<Cas2v2Assessment> {
    val assessment = when (
      val assessmentResult = cas2v2AssessmentService.getAssessment(assessmentId)
    ) {
      is CasResult.NotFound -> throw NotFoundProblem(assessmentId, "Cas2v2Assessment")
      is CasResult.Unauthorised -> throw ForbiddenProblem()
      is CasResult.Success -> assessmentResult
      is CasResult.ConflictError<*> -> throw ConflictProblem(assessmentId, "Cas2v2Assessment conflict by assessmentId")
      is CasResult.FieldValidationError<*> -> CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))
      is CasResult.GeneralValidationError<*> -> CasResult.GeneralValidationError("General Validation Error")
    }

    val cas2v2AssessmentEntity = extractEntityFromCasResult(assessment)
    return ResponseEntity.ok(cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(cas2v2AssessmentEntity))
  }

  override fun assessmentsAssessmentIdPut(
    assessmentId: UUID,
    updateCas2Assessment: UpdateCas2Assessment,
  ): ResponseEntity<Cas2v2Assessment> {
    val assessmentResult = cas2v2AssessmentService.updateAssessment(assessmentId, updateCas2Assessment)
    when (assessmentResult) {
      is CasResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is CasResult.Unauthorised -> throw ForbiddenProblem()
      is CasResult.Success -> assessmentResult
      is CasResult.ConflictError<*> -> throw ConflictProblem(assessmentId, "Cas2v2Assessment conflict by assessmentId")
      is CasResult.FieldValidationError<*> -> CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))
      is CasResult.GeneralValidationError<*> -> CasResult.GeneralValidationError("General Validation Error")
    }

    val cas2v2AssessmentEntity = extractEntityFromCasResult(assessmentResult)
    return ResponseEntity.ok(
      cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(cas2v2AssessmentEntity),
    )
  }

  override fun assessmentsAssessmentIdStatusUpdatesPost(
    assessmentId: UUID,
    cas2AssessmentStatusUpdate: Cas2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = cas2v2StatusUpdateService.createForAssessment(
      assessmentId = assessmentId,
      statusUpdate = cas2AssessmentStatusUpdate,
      assessor = externalUserService.getUserForRequest(),
    )

    processAuthorisationFor(result)
      .run { processValidation(result) }

    return ResponseEntity(HttpStatus.CREATED)
  }

  override fun assessmentsAssessmentIdNotesPost(
    assessmentId: UUID,
    body: NewCas2ApplicationNote,
  ): ResponseEntity<Cas2ApplicationNote> {
    val noteResult = cas2v2ApplicationNoteService.createAssessmentNote(assessmentId, body)

    val validationResult = processAuthorisationFor(noteResult) as CasResult<Cas2ApplicationNote>

    val note = processValidation(validationResult) as Cas2ApplicationNoteEntity

    return ResponseEntity
      .created(URI.create("/cas2/assessments/$assessmentId/notes/${note.id}"))
      .body(
        applicationNotesTransformer.transformJpaToApi(note),
      )
  }

  private fun <EntityType> processAuthorisationFor(
    result: CasResult<EntityType>,
  ): Any? {
    return extractEntityFromCasResult(result)
  }

  private fun <EntityType : Any> processValidation(casResult: CasResult<EntityType>): Any {
    return extractEntityFromCasResult(casResult)
  }
}
