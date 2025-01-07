package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.service.cas2v2

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2.Cas2v2AssessmentService
import java.time.OffsetDateTime
import java.util.UUID

class Cas2v2AssessmentServiceTest {

  private val mockCas2v2AssessmentRepository = mockk<Cas2v2AssessmentRepository>()

  private val cas2v2AssessmentService = Cas2v2AssessmentService(
    mockCas2v2AssessmentRepository,
  )

  @Nested
  inner class CreateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2v2AssessmentEntity(
        id = UUID.randomUUID(),
        application = cas2v2Application,
        createdAt = OffsetDateTime.now(),
      )

      every { mockCas2v2AssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      val result = cas2v2AssessmentService.createCas2v2Assessment(
        cas2v2Application,
      )
      Assertions.assertThat(result).isEqualTo(assessEntity)

      verify(exactly = 1) {
        mockCas2v2AssessmentRepository.save(
          match { it.application == cas2v2Application },
        )
      }
    }
  }

  @Nested
  inner class UpdateAssessment {

    @Test
    fun `saves and returns entity from db`() {
      val assessmentId = UUID.randomUUID()
      val cas2v2Application = Cas2v2ApplicationEntityFactory()
        .withCreatedByUser(
          NomisUserEntityFactory()
            .produce(),
        ).produce()
      val assessEntity = Cas2v2AssessmentEntity(
        id = assessmentId,
        application = cas2v2Application,
        createdAt = OffsetDateTime.now(),
      )

      val newAssessmentData = UpdateCas2Assessment(
        nacroReferralId = "1234OH",
        assessorName = "Anne Assessor",
      )

      every { mockCas2v2AssessmentRepository.save(any()) } answers
        {
          assessEntity
        }

      every { mockCas2v2AssessmentRepository.findByIdOrNull(assessmentId) } answers
        {
          assessEntity
        }

      val result = cas2v2AssessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )
      Assertions.assertThat(result).isEqualTo(

        CasResult.Success(assessEntity),

      )

      verify(exactly = 1) {
        mockCas2v2AssessmentRepository.save(
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

      every { mockCas2v2AssessmentRepository.findByIdOrNull(assessmentId) } answers
        {
          null
        }

      val result = cas2v2AssessmentService.updateAssessment(
        assessmentId = assessmentId,
        newAssessment = newAssessmentData,
      )

      Assertions.assertThat(result is CasResult.NotFound).isTrue
    }
  }
}
