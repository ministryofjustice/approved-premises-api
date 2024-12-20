package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.AuthorisableActionResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.ValidatableActionResult
import java.time.OffsetDateTime
import java.util.UUID

class AssessmentServiceTest {
  private val mockAssessmentRepository = mockk<Cas2AssessmentRepository>()

  private val assessmentService = uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.AssessmentService(
    mockAssessmentRepository,
  )

  @Nested
  inner class CreateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = UUID.randomUUID(),
        application = application,
        createdAt = OffsetDateTime.now(),
      )

      every { mockAssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      val result = assessmentService.createCas2Assessment(
        application,
      )
      Assertions.assertThat(result).isEqualTo(assessEntity)

      verify(exactly = 1) {
        mockAssessmentRepository.save(
          match { it.application == application },
        )
      }
    }
  }

  @Nested
  inner class UpdateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val assessmentId = UUID.randomUUID()
      val application = Cas2ApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2AssessmentEntity(
        id = assessmentId,
        application = application,
        createdAt = OffsetDateTime.now(),
      )

      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockAssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } answers
        {
          assessEntity
        }

      val result = assessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )
      Assertions.assertThat(result).isEqualTo(
        AuthorisableActionResult.Success(
          ValidatableActionResult.Success(assessEntity),
        ),
      )

      verify(exactly = 1) {
        mockAssessmentRepository.save(
          match {
            it.id == assessEntity.id &&
              it.nacroReferralId == newAssessmentData.nacroReferralId &&
              it.assessorName == newAssessmentData.assessorName
          },
        )
      }
    }

    @Test
    fun `returns NotFound if entity is not found`() {
      val assessmentId = UUID.randomUUID()
      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockAssessmentRepository.findByIdOrNull(assessmentId) } answers
        {
          null
        }

      val result = assessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )

      Assertions.assertThat(result is AuthorisableActionResult.NotFound).isTrue
    }
  }
}
