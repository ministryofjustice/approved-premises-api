package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ApplicationOrigin
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.NomisUserTransformer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component("Cas2SubmittedApplicationTransformer")
class SubmissionsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val nomisUserTransformer: NomisUserTransformer,
  private val timelineEventsTransformer: TimelineEventsTransformer,
  private val assessmentsTransformer: AssessmentsTransformer,
) {

  fun transformJpaToApiRepresentation(
    jpa: Cas2ApplicationEntity,
    personInfo: PersonInfoResult
      .Success,
  ): Cas2SubmittedApplication {
    return Cas2SubmittedApplication(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      submittedBy = nomisUserTransformer.transformJpaToApi(jpa.createdByUser),
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      telephoneNumber = jpa.telephoneNumber,
      timelineEvents = timelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
      assessment = assessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
      applicationOrigin = getApplicationOrigin(jpa.applicationOrigin),
    )
  }

  fun transformJpaSummaryToApiRepresentation(
    jpaSummary: Cas2ApplicationSummaryEntity,
    personName: String,
  ): Cas2SubmittedApplicationSummary {
    return Cas2SubmittedApplicationSummary(
      id = jpaSummary.id,
      personName = personName,
      createdByUserId = UUID.fromString(jpaSummary.userId),
      createdAt = jpaSummary.createdAt.toInstant(),
      submittedAt = jpaSummary.submittedAt?.toInstant(),
      crn = jpaSummary.crn,
      nomsNumber = jpaSummary.nomsNumber,
      applicationOrigin = getApplicationOrigin(jpaSummary.applicationOrigin),
    )
  }
}

// shared function between SubmissionsTransformer and ApplicationsTransformer
fun getApplicationOrigin(origin: String?): ApplicationOrigin? {
  return when (origin) {
    "courtBail" -> ApplicationOrigin.courtBail
    "prisonBail" -> ApplicationOrigin.prisonBail
    else -> ApplicationOrigin.homeDetentionCurfew
  }
}
