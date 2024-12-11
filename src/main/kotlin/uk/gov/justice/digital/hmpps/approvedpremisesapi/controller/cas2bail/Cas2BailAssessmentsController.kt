package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.cas2bail

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2bail.AssessmentsCas2bailDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2AssessmentStatusUpdate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.NewCas2ApplicationNote
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ConflictProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ExternalUserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailApplicationNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailAssessmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailStatusUpdateService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2.ApplicationNotesTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2bail.Cas2BailAssessmentsTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.net.URI
import java.util.UUID

@Service("Cas2BailAssessmentsController")
class Cas2BailAssessmentsController(
  private val cas2BailAssessmentService: Cas2BailAssessmentService,
  private val cas2BailApplicationNoteService: Cas2BailApplicationNoteService,
  private val cas2BailAssessmentsTransformer: Cas2BailAssessmentsTransformer,
  private val applicationNotesTransformer: ApplicationNotesTransformer,
  private val cas2BailStatusUpdateService: Cas2BailStatusUpdateService,
  private val externalUserService: ExternalUserService,
) : AssessmentsCas2bailDelegate {

  override fun assessmentsAssessmentIdGet(assessmentId: UUID): ResponseEntity<Cas2Assessment> {
    val assessment = when (
      val assessmentResult = cas2BailAssessmentService.getAssessment(assessmentId)
    ) {
      is CasResult.NotFound -> throw NotFoundProblem(assessmentId, "Cas2BailAssessment")
      is CasResult.Unauthorised -> throw ForbiddenProblem()
      is CasResult.Success -> assessmentResult
      is CasResult.ConflictError<*> -> throw ConflictProblem(assessmentId, "Cas2BailAssessment conflict by assessmentId")
      is CasResult.FieldValidationError<*> -> CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))
      is CasResult.GeneralValidationError<*> -> CasResult.GeneralValidationError("General Validation Error")
    }

    val cas2BailAssessmentEntity = extractEntityFromCasResult(assessment)
    return ResponseEntity.ok(cas2BailAssessmentsTransformer.transformJpaToApiRepresentation(cas2BailAssessmentEntity))
  }

  override fun assessmentsAssessmentIdPut(
    assessmentId: UUID,
    updateCas2Assessment: UpdateCas2Assessment,
  ): ResponseEntity<Cas2Assessment> {
    val assessmentResult = cas2BailAssessmentService.updateAssessment(assessmentId, updateCas2Assessment)
    when (assessmentResult) {
      is CasResult.NotFound -> throw NotFoundProblem(assessmentId, "Assessment")
      is CasResult.Unauthorised -> throw ForbiddenProblem()
      is CasResult.Success -> assessmentResult
      is CasResult.ConflictError<*> -> throw ConflictProblem(assessmentId, "Cas2BailAssessment conflict by assessmentId")
      is CasResult.FieldValidationError<*> -> CasResult.FieldValidationError(mapOf("$.reason" to "doesNotExist"))
      is CasResult.GeneralValidationError<*> -> CasResult.GeneralValidationError("General Validation Error")
    }

    val cas2BailAssessmentEntity = extractEntityFromCasResult(assessmentResult)
    return ResponseEntity.ok(
      cas2BailAssessmentsTransformer.transformJpaToApiRepresentation(cas2BailAssessmentEntity),
    )
  }

  override fun assessmentsAssessmentIdStatusUpdatesPost(
    assessmentId: UUID,
    cas2AssessmentStatusUpdate: Cas2AssessmentStatusUpdate,
  ): ResponseEntity<Unit> {
    val result = cas2BailStatusUpdateService.createForAssessment(
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
    val noteResult = cas2BailApplicationNoteService.createAssessmentNote(assessmentId, body)

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
