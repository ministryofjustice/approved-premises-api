package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.DocumentsApiDelegate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.NotFoundProblem
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import java.io.OutputStream
import java.util.UUID

@Controller
class DocumentsController(
  private val userService: UserService,
  private val offenderService: OffenderService,
) : DocumentsApiDelegate {

  override fun documentsCrnDocumentIdGet(crn: String, documentId: UUID): ResponseEntity<StreamingResponseBody> {
    val user = userService.getUserForRequest()

    getOffenderDetails(crn, user)

    val documentsMetaData = getDocuments(crn)

    val documentFilename = getDocumentFileName(documentsMetaData, documentId)
    return ResponseEntity(
      StreamingResponseBody { outputStream ->
        getDocument(crn, documentId, outputStream)
      },
      HttpHeaders().apply {
        put("Content-Disposition", listOf("attachment; filename=\"$documentFilename\""))
      },
      HttpStatus.OK,
    )
  }

  private fun getOffenderDetails(crn: String, user: UserEntity) {
    val offenderDetailsResult =
      offenderService.getOffenderByCrn(crn, user.deliusUsername, user.hasQualification(UserQualification.LAO))

    when (offenderDetailsResult) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Person")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> Unit
    }
  }

  private fun getDocument(crn: String, documentId: UUID, outputStream: OutputStream) {
    offenderService.getDocumentFromDelius(crn, documentId.toString(), outputStream)
  }

  private fun getDocuments(crn: String): List<APDeliusDocument> {
    return when (val result = offenderService.getDocumentsFromApDeliusApi(crn)) {
      is AuthorisableActionResult.NotFound -> throw NotFoundProblem(crn, "Documents")
      is AuthorisableActionResult.Unauthorised -> throw ForbiddenProblem()
      is AuthorisableActionResult.Success -> result.entity
    }
  }

  private fun getDocumentFileName(documentsMetaData: List<APDeliusDocument>, documentId: UUID): String {
    val document = documentsMetaData.firstOrNull { it.id == documentId.toString() } ?: throw NotFoundProblem(documentId, "Document")
    return document.filename
  }
}
