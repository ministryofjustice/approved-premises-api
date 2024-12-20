package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed.cas3

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3ReferralRejectionSeedCsvRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3ReferralRejectionSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.randomStringLowerCase
import java.time.OffsetDateTime
import java.util.UUID

class Cas3ReferralRejectionSeedJobTest {
  private val mockAssessmentRepository = mockk<AssessmentRepository>()
  private val mockReferralRejectionReasonRepository = mockk<ReferralRejectionReasonRepository>()
  private val mockAssertAssessmentHasSystemNoteRepository = mockk<AssessmentReferralHistoryNoteRepository>()
  private val mockUserRepository = mockk<UserRepository>()

  private val assessmentId = UUID.randomUUID()
  private val deliusUsername = randomStringLowerCase(20).uppercase()

  private val userEntity = UserEntityFactory()
    .withYieldedProbationRegion {
      ProbationRegionEntityFactory()
        .produce()
    }
    .produce()

  private val seedJob = Cas3ReferralRejectionSeedJob(
    assessmentRepository = mockAssessmentRepository,
    referralRejectionReasonRepository = mockReferralRejectionReasonRepository,
    assessmentReferralHistoryNoteRepository = mockAssertAssessmentHasSystemNoteRepository,
    userRepository = mockUserRepository,
  )

  @Test
  fun `When the user doesn't exist expect an error`() {
    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false, deliusUsername))
    }.hasMessage("User with delius username $deliusUsername not found")
  }

  @Test
  fun `When the assessment is not Temporary Accommodation assessment expect an error`() {
    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      ApprovedPremisesAssessmentEntityFactory()
        .withApplication(
          ApprovedPremisesApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .produce()

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false, deliusUsername))
    }.hasMessage("Assessment with id $assessmentId is not a temporary accommodation assessment")
  }

  @Test
  fun `When an assessment doesn't exist expect an error`() {
    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", randomStringLowerCase(20), false, deliusUsername))
    }.hasMessage("Assessment with id $assessmentId not found")
  }

  @Test
  fun `When the application has been allocated expect an error`() {
    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(
          TemporaryAccommodationApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .withReallocatedAt(OffsetDateTime.now())
        .produce()

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false, deliusUsername))
    }.hasMessage("The application has been reallocated, this assessment is read only")
  }

  @Test
  fun `When the rejection reason doesn't exist expect an error`() {
    val notExistRejectionReason = "not exist rejection reason"

    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns
      TemporaryAccommodationAssessmentEntityFactory()
        .withApplication(
          TemporaryAccommodationApplicationEntityFactory()
            .withDefaults()
            .produce(),
        )
        .produce()

    every { mockReferralRejectionReasonRepository.findByNameAndActive(notExistRejectionReason, ServiceName.temporaryAccommodation.value) } returns null

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, notExistRejectionReason, null, false, deliusUsername))
    }.hasMessage("Rejection reason $notExistRejectionReason not found")
  }

  @Test
  fun `When save an assessment and an exception happened expect logging error`() {
    val assessment = TemporaryAccommodationAssessmentEntityFactory()
      .withApplication(
        TemporaryAccommodationApplicationEntityFactory()
          .withDefaults()
          .produce(),
      )
      .produce()

    every { mockUserRepository.findByDeliusUsername(deliusUsername) } returns userEntity

    every { mockAssessmentRepository.findByIdOrNull(assessmentId) } returns assessment

    every { mockReferralRejectionReasonRepository.findByNameAndActive("rejection reason", ServiceName.temporaryAccommodation.value) } returns
      ReferralRejectionReasonEntity(UUID.randomUUID(), "rejection reason", true, ServiceName.temporaryAccommodation.value, 1)

    every { mockAssessmentRepository.save(any()) } throws RuntimeException("Failed to update assessment with id $assessmentId")

    assertThatThrownBy {
      seedJob.processRow(Cas3ReferralRejectionSeedCsvRow(assessmentId, "rejection reason", null, false, deliusUsername))
    }.hasMessage("Failed to update assessment with id $assessmentId")
  }
}
