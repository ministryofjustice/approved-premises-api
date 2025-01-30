package uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas2v2

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2v2SubmittedApplicationSummary
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationSummaryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.PersonInfoResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.PersonTransformer
import java.util.UUID

@Component
class Cas2v2SubmissionsTransformer(
  private val objectMapper: ObjectMapper,
  private val personTransformer: PersonTransformer,
  private val cas2v2UserTransformer: Cas2v2UserTransformer,
  private val cas2v2TimelineEventsTransformer: Cas2v2TimelineEventsTransformer,
  private val cas2v2AssessmentsTransformer: Cas2v2AssessmentsTransformer,
) {

  fun transformJpaToApiRepresentation(
    jpa: Cas2v2ApplicationEntity,
    personInfo: PersonInfoResult
      .Success,
  ): Cas2v2SubmittedApplication {
    return Cas2v2SubmittedApplication(
      id = jpa.id,
      person = personTransformer.transformModelToPersonApi(personInfo),
      submittedBy = cas2v2UserTransformer.transformJpaToApi(jpa.createdByUser),
      schemaVersion = jpa.schemaVersion.id,
      outdatedSchema = !jpa.schemaUpToDate,
      createdAt = jpa.createdAt.toInstant(),
      submittedAt = jpa.submittedAt?.toInstant(),
      document = if (jpa.document != null) objectMapper.readTree(jpa.document) else null,
      telephoneNumber = jpa.telephoneNumber,
      applicationOrigin = jpa.applicationOrigin,
      timelineEvents = cas2v2TimelineEventsTransformer.transformApplicationToTimelineEvents(jpa),
      assessment = cas2v2AssessmentsTransformer.transformJpaToApiRepresentation(jpa.assessment!!),
    )
  }

  fun transformJpaSummaryToApiRepresentation(
    jpaSummary: Cas2v2ApplicationSummaryEntity,
    personName: String,
  ): Cas2v2SubmittedApplicationSummary {
    return Cas2v2SubmittedApplicationSummary(
      id = jpaSummary.id,
      personName = personName,
      createdByUserId = UUID.fromString(jpaSummary.userId),
      createdAt = jpaSummary.createdAt.toInstant(),
      submittedAt = jpaSummary.submittedAt?.toInstant(),
      crn = jpaSummary.crn,
      nomsNumber = jpaSummary.nomsNumber,
    )
  }
}
