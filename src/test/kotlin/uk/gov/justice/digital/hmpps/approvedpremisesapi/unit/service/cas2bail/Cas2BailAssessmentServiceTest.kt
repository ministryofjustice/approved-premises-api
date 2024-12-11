package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2bail

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2bail.Cas2BailApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2bail.Cas2BailAssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2bail.Cas2BailAssessmentService
import java.time.OffsetDateTime
import java.util.UUID

class Cas2BailAssessmentServiceTest {

  private val mockCas2BailAssessmentRepository = mockk<Cas2BailAssessmentRepository>()

  private val cas2BailAssessmentService = Cas2BailAssessmentService(
    mockCas2BailAssessmentRepository,
  )

  @Nested
  inner class CreateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val cas2BailApplication = Cas2BailApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2BailAssessmentEntity(
        id = UUID.randomUUID(),
        application = cas2BailApplication,
        createdAt = OffsetDateTime.now(),
      )

      every { mockCas2BailAssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      val result = cas2BailAssessmentService.createCas2BailAssessment(
        cas2BailApplication,
      )
      Assertions.assertThat(result).isEqualTo(assessEntity)

      verify(exactly = 1) {
        mockCas2BailAssessmentRepository.save(
          match { it.application == cas2BailApplication },
        )
      }
    }
  }

  @Nested
  inner class UpdateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val assessmentId = UUID.randomUUID()
      val cas2BailApplication = Cas2BailApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2BailAssessmentEntity(
        id = assessmentId,
        application = cas2BailApplication,
        createdAt = OffsetDateTime.now(),
      )

      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockCas2BailAssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      every { mockCas2BailAssessmentRepository.findByIdOrNull(assessmentId) } answers
        {
          assessEntity
        }

      val result = cas2BailAssessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )
      Assertions.assertThat(result).isEqualTo(

        CasResult.Success(assessEntity),

      )

      verify(exactly = 1) {
        mockCas2BailAssessmentRepository.save(
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

      every { mockCas2BailAssessmentRepository.findByIdOrNull(assessmentId) } answers
        {
          null
        }

      val result = cas2BailAssessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )

      Assertions.assertThat(result is CasResult.NotFound).isTrue
    }
  }
}
