package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealed
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.EventType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.PersonReference
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.util.UUID

@Service
class Cas1AppealDomainEventService(
  private val domainEventService: Cas1DomainEventService,
  private val apDeliusContextApiClient: ApDeliusContextApiClient,
  @Value("\${url-templates.frontend.application}") private val applicationUrlTemplate: UrlTemplate,
  @Value("\${url-templates.frontend.application-appeal}") private val applicationAppealUrlTemplate: UrlTemplate,
) {

  fun appealRecordCreated(appeal: AppealEntity) {
    val id = UUID.randomUUID()
    val timestamp = appeal.createdAt.toInstant()

    val staffDetails = when (val result = apDeliusContextApiClient.getStaffDetail(appeal.createdBy.deliusUsername)) {
      is ClientResult.Success -> result.body
      is ClientResult.Failure -> result.throwException()
    }

    domainEventService.saveAssessmentAppealedEvent(
      DomainEvent(
        id = id,
        applicationId = appeal.application.id,
        assessmentId = null,
        bookingId = null,
        crn = appeal.application.crn,
        nomsNumber = appeal.application.nomsNumber,
        occurredAt = timestamp,
        data = AssessmentAppealedEnvelope(
          id = id,
          timestamp = timestamp,
          eventType = EventType.assessmentAppealed,
          eventDetails = AssessmentAppealed(
            applicationId = appeal.application.id,
            applicationUrl = applicationUrlTemplate.resolve("id", appeal.application.id.toString()),
            appealId = appeal.id,
            appealUrl = applicationAppealUrlTemplate.resolve(
              mapOf("applicationId" to appeal.application.id.toString(), "appealId" to appeal.id.toString()),
            ),
            personReference = PersonReference(
              crn = appeal.application.crn,
              noms = appeal.application.nomsNumber ?: "Unknown NOMS Number",
            ),
            deliusEventNumber = (appeal.application as ApprovedPremisesApplicationEntity).eventNumber,
            createdAt = timestamp,
            createdBy = staffDetails.toStaffMember(),
            appealDetail = appeal.appealDetail,
            decision = parseDecision(appeal.decision),
            decisionDetail = appeal.decisionDetail,
          ),
        ),
      ),
    )
  }

  private fun parseDecision(value: String): AppealDecision =
    AppealDecision.entries.firstOrNull { it.value == value }
      ?: throw IllegalArgumentException("Unknown appeal decision type '$value'")
}
