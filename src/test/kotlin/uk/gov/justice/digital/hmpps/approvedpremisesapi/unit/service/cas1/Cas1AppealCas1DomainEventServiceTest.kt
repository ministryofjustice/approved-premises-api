package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas1

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AssessmentAppealedEnvelope
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.AppealDecision
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ApDeliusContextApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.ClientResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.StaffDetailFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.DomainEvent
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1AppealDomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.util.withinSeconds
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.UrlTemplate
import java.time.LocalDate
import java.util.UUID

class Cas1AppealCas1DomainEventServiceTest {

  private val domainEventService = mockk<Cas1DomainEventService>()
  private val apDeliusContextApiClient = mockk<ApDeliusContextApiClient>()
  private val applicationUrlTemplate = mockk<UrlTemplate>()
  private val applicationAppealUrlTemplate = mockk<UrlTemplate>()

  private val service = Cas1AppealDomainEventService(
    domainEventService,
    apDeliusContextApiClient,
    applicationUrlTemplate,
    applicationAppealUrlTemplate,
  )

  private val probationRegion = ProbationRegionEntityFactory()
    .withApArea(
      ApAreaEntityFactory()
        .produce(),
    )
    .produce()

  private val createdByUser = UserEntityFactory()
    .withProbationRegion(probationRegion)
    .produce()

  private val staffUserDetails = StaffDetailFactory.staffDetail(deliusUsername = createdByUser.deliusUsername)

  private val application = ApprovedPremisesApplicationEntityFactory()
    .withCreatedByUser(createdByUser)
    .produce()

  private val assessment = ApprovedPremisesAssessmentEntityFactory()
    .withApplication(application)
    .produce()

  private val appealId = UUID.randomUUID()

  val now = LocalDate.now()

  @BeforeEach
  fun setupMocks() {
    every { domainEventService.saveAssessmentAppealedEvent(any()) } just Runs
    every { apDeliusContextApiClient.getStaffDetail(createdByUser.deliusUsername) } returns ClientResult.Success(
      HttpStatus.OK,
      staffUserDetails,
    )

    every { applicationUrlTemplate.resolve(any(), any()) } returns "http://frontend/applications/${application.id}"
    every { applicationAppealUrlTemplate.resolve(any()) } returns "http://frontend/applications/${application.id}/appeals/$appealId"
  }

  @Test
  fun `appealRecordCreated creates domain event`() {
    mockkStatic(UUID::class) {
      every { UUID.randomUUID() } returns appealId

      service.appealRecordCreated(
        AppealEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .withAppealDate(now)
          .withDecision(AppealDecision.accepted)
          .withCreatedBy(createdByUser)
          .withAppealDetail("Some information about why the appeal is being made")
          .withDecisionDetail("Some information about the decision made")
          .produce(),
      )

      verify(exactly = 1) {
        domainEventService.saveAssessmentAppealedEvent(
          match {
            it.matches()
          },
        )
      }
    }
  }

  @Test
  fun `appealRecordCreated noms is optional`() {
    application.nomsNumber = null

    mockkStatic(UUID::class) {
      every { UUID.randomUUID() } returns appealId

      service.appealRecordCreated(
        AppealEntityFactory()
          .withApplication(application)
          .withAssessment(assessment)
          .withAppealDate(now)
          .withDecision(AppealDecision.accepted)
          .withCreatedBy(createdByUser)
          .withAppealDetail("Some information about why the appeal is being made")
          .withDecisionDetail("Some information about the decision made")
          .produce(),
      )

      verify(exactly = 1) {
        domainEventService.saveAssessmentAppealedEvent(
          match {
            it.data.eventDetails.personReference.noms == "Unknown NOMS Number"
          },
        )
      }
    }
  }

  fun DomainEvent<AssessmentAppealedEnvelope>.matches() =
    this.applicationId == application.id &&
      this.assessmentId == null &&
      this.bookingId == null &&
      this.crn == application.crn &&
      withinSeconds(10).matches(this.occurredAt.toString()) &&
      this.data.matches()

  private fun AssessmentAppealedEnvelope.matches() =
    this.eventDetails.applicationId == application.id &&
      this.eventDetails.applicationUrl == "http://frontend/applications/${application.id}" &&
      this.eventDetails.appealId == appealId &&
      this.eventDetails.appealUrl == "http://frontend/applications/${application.id}/appeals/$appealId" &&
      this.eventDetails.personReference.crn == application.crn &&
      this.eventDetails.personReference.noms == application.nomsNumber &&
      this.eventDetails.deliusEventNumber == application.eventNumber &&
      withinSeconds(10).matches(this.eventDetails.createdAt.toString()) &&
      this.eventDetails.createdBy == staffUserDetails.toStaffMember() &&
      this.eventDetails.appealDetail == "Some information about why the appeal is being made" &&
      this.eventDetails.decision == uk.gov.justice.digital.hmpps.approvedpremisesapi.api.events.cas1.model.AppealDecision.accepted &&
      this.eventDetails.decisionDetail == "Some information about the decision made"
}
