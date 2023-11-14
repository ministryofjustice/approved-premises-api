package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given a User`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.`Given an Offender`
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import java.time.OffsetDateTime

class ApplicationSummaryQueryTest : IntegrationTestBase() {
  @Autowired
  lateinit var realApplicationRepository: ApplicationRepository

  @Test
  fun `findNonWithdrawnApprovedPremisesSummariesForUser query works as described`() {
    `Given a User` { user, jwt ->
      `Given a User` { differentUser, _ ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationSchema = approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val premises = approvedPremisesEntityFactory.produceAndPersist {
            withProbationRegion(
              probationRegionEntityFactory.produceAndPersist {
                withApArea(apAreaEntityFactory.produceAndPersist())
              },
            )
            withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          val applicationCreatedByDifferentUser = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(differentUser)
            withApplicationSchema(applicationSchema)
            withIsPipeApplication(true)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(null)
          }

          val nonSubmittedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withIsPipeApplication(true)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(null)
            withIsInapplicable(false)
          }

          val withdrawnApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withIsPipeApplication(true)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(null)
            withIsInapplicable(false)
            withIsWithdrawn(true)
          }

          val inapplicableApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withIsInapplicable(true)
          }

          val submittedApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withIsPipeApplication(true)
            withIsWomensApplication(false)
            withReleaseType("rotl")
            withSubmittedAt(OffsetDateTime.parse("2023-04-19T09:34:00+01:00"))
          }

          val assessmentForSubmittedApplication = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(submittedApplication)
            withAllocatedToUser(user)
            withAssessmentSchema(assessmentSchema)
            withSubmittedAt(OffsetDateTime.parse("2023-04-19T10:15:00+01:00"))
          }

          val unansweredClarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
            withAssessment(assessmentForSubmittedApplication)
            withCreatedBy(user)
            withResponse(null)
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withApplication(submittedApplication)
          }

          val placementRequirements = placementRequirementsFactory.produceAndPersist {
            withApplication(submittedApplication)
            withAssessment(assessmentForSubmittedApplication)
            withPostcodeDistrict(postcodeDistrict)
            withEssentialCriteria(emptyList())
            withDesirableCriteria(emptyList())
          }

          val placementRequest = placementRequestFactory.produceAndPersist {
            withApplication(submittedApplication)
            withAssessment(assessmentForSubmittedApplication)
            withAllocatedToUser(user)
            withBooking(booking)
            withPlacementRequirements(placementRequirements)
          }

          val results = realApplicationRepository.findNonWithdrawnApprovedPremisesSummariesForUser(user.id)

          assertThat(results.size).isEqualTo(2)

          assertThat(results).noneMatch { it.getId() == withdrawnApplication.id }

          results.first { it.getId() == nonSubmittedApplication.id }.let {
            assertThat(it.getCrn()).isEqualTo(nonSubmittedApplication.crn)
            assertThat(it.getCreatedByUserId()).isEqualTo(nonSubmittedApplication.createdByUser.id)
            assertThat(it.getCreatedAt().toInstant()).isEqualTo(nonSubmittedApplication.createdAt.toInstant())
            assertThat(it.getSubmittedAt()?.toInstant()).isEqualTo(nonSubmittedApplication.submittedAt?.toInstant())
            assertThat(it.getLatestAssessmentSubmittedAt()).isNull()
            assertThat(it.getLatestAssessmentDecision()).isNull()
            assertThat(it.getLatestAssessmentHasClarificationNotesWithoutResponse()).isEqualTo(false)
            assertThat(it.getHasPlacementRequest()).isEqualTo(false)
            assertThat(it.getHasBooking()).isEqualTo(false)
          }

          results.first { it.getId() == submittedApplication.id }.let {
            assertThat(it.getCrn()).isEqualTo(submittedApplication.crn)
            assertThat(it.getCreatedByUserId()).isEqualTo(submittedApplication.createdByUser.id)
            assertThat(it.getCreatedAt().toInstant()).isEqualTo(submittedApplication.createdAt.toInstant())
            assertThat(it.getSubmittedAt()?.toInstant()).isEqualTo(submittedApplication.submittedAt?.toInstant())
            assertThat(it.getLatestAssessmentSubmittedAt()?.toInstant()).isEqualTo(assessmentForSubmittedApplication.submittedAt?.toInstant())
            assertThat(it.getLatestAssessmentDecision()).isEqualTo(assessmentForSubmittedApplication.decision)
            assertThat(it.getLatestAssessmentHasClarificationNotesWithoutResponse()).isEqualTo(true)
            assertThat(it.getHasPlacementRequest()).isEqualTo(true)
            assertThat(it.getHasBooking()).isEqualTo(true)
          }

          assertThat(results).noneMatch {
            it.getId() == applicationCreatedByDifferentUser.id
          }

          assertThat(results).noneMatch {
            it.getId() == inapplicableApplication.id
          }
        }
      }
    }

    @Test
    fun `findAllTemporaryAccommodationSummariesCreatedByUser query works as described`() {
      `Given a User` { user, jwt ->
        `Given an Offender` { offenderDetails, inmateDetails ->
          val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val assessmentSchema = approvedPremisesAssessmentJsonSchemaEntityFactory.produceAndPersist {
            withPermissiveSchema()
          }

          val postcodeDistrict = postCodeDistrictFactory.produceAndPersist()

          val probationRegion = probationRegionEntityFactory.produceAndPersist {
            withApArea(apAreaEntityFactory.produceAndPersist())
          }

          val premises = temporaryAccommodationPremisesEntityFactory.produceAndPersist {
            withProbationRegion(probationRegion)
            withLocalAuthorityArea(localAuthorityEntityFactory.produceAndPersist())
          }

          val room = roomEntityFactory.produceAndPersist {
            withPremises(premises)
          }

          val bed = bedEntityFactory.produceAndPersist {
            withRoom(room)
          }

          val nonSubmittedApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withSubmittedAt(null)
            withProbationRegion(probationRegion)
          }

          val submittedApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
            withCrn(offenderDetails.case.crn)
            withCreatedByUser(user)
            withApplicationSchema(applicationSchema)
            withSubmittedAt(OffsetDateTime.parse("2023-04-19T09:34:00+01:00"))
            withProbationRegion(probationRegion)
          }

          val assessmentForSubmittedApplication = approvedPremisesAssessmentEntityFactory.produceAndPersist {
            withApplication(submittedApplication)
            withAllocatedToUser(user)
            withAssessmentSchema(assessmentSchema)
            withSubmittedAt(OffsetDateTime.parse("2023-04-19T10:15:00+01:00"))
          }

          val unansweredClarificationNote = assessmentClarificationNoteEntityFactory.produceAndPersist {
            withAssessment(assessmentForSubmittedApplication)
            withCreatedBy(user)
            withResponse(null)
          }

          val booking = bookingEntityFactory.produceAndPersist {
            withPremises(premises)
            withBed(bed)
            withApplication(submittedApplication)
          }

          val results = realApplicationRepository.findAllTemporaryAccommodationSummariesCreatedByUser(user.id)

          results.first { it.getId() == nonSubmittedApplication.id }.let {
            assertThat(it.getCrn()).isEqualTo(nonSubmittedApplication.crn)
            assertThat(it.getCreatedByUserId()).isEqualTo(nonSubmittedApplication.createdByUser.id)
            assertThat(it.getCreatedAt().toInstant()).isEqualTo(nonSubmittedApplication.createdAt.toInstant())
            assertThat(it.getSubmittedAt()?.toInstant()).isEqualTo(nonSubmittedApplication.submittedAt?.toInstant())
            assertThat(it.getLatestAssessmentSubmittedAt()).isNull()
            assertThat(it.getLatestAssessmentDecision()).isNull()
            assertThat(it.getLatestAssessmentHasClarificationNotesWithoutResponse()).isEqualTo(false)
            assertThat(it.getHasBooking()).isEqualTo(false)
            assertThat(it.getRiskRatings()).isNotBlank()
          }

          results.first { it.getId() == submittedApplication.id }.let {
            assertThat(it.getCrn()).isEqualTo(submittedApplication.crn)
            assertThat(it.getCreatedByUserId()).isEqualTo(submittedApplication.createdByUser.id)
            assertThat(it.getCreatedAt().toInstant()).isEqualTo(submittedApplication.createdAt.toInstant())
            assertThat(it.getSubmittedAt()?.toInstant()).isEqualTo(submittedApplication.submittedAt?.toInstant())
            assertThat(it.getLatestAssessmentSubmittedAt()?.toInstant()).isEqualTo(assessmentForSubmittedApplication.submittedAt?.toInstant())
            assertThat(it.getLatestAssessmentDecision()).isEqualTo(assessmentForSubmittedApplication.decision)
            assertThat(it.getLatestAssessmentHasClarificationNotesWithoutResponse()).isEqualTo(true)
            assertThat(it.getHasBooking()).isEqualTo(true)
            assertThat(it.getRiskRatings()).isNotBlank()
          }
        }
      }
    }
  }
}
