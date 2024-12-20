package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Document
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.DocumentLevel
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.APDeliusDocument

@Component
class DocumentTransformer {

  fun transformToApi(documents: List<APDeliusDocument>): List<Document> =
    documents
      .filter { it.id != null }
      .map {
        Document(
          id = it.id!!,
          level = when (it.level) {
            "Conviction" -> DocumentLevel.conviction
            else -> DocumentLevel.offender
          },
          fileName = it.filename,
          createdAt = it.dateCreated.toInstant(),
          typeCode = it.typeCode,
          typeDescription = it.typeDescription,
          description = it.description,
        )
      }
}
