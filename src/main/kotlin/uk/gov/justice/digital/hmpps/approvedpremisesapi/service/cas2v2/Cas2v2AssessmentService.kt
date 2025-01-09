package uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2v2

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.UpdateCas2Assessment
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.results.CasResult
import java.time.OffsetDateTime
import java.util.UUID

@Service("Cas2v2AssessmentService")
class Cas2v2AssessmentService(
  private val cas2AssessmentRepository: Cas2v2AssessmentRepository,
) {

  @Transactional
  fun createCas2v2Assessment(cas2v2ApplicationEntity: Cas2v2ApplicationEntity): Cas2v2AssessmentEntity =
    cas2AssessmentRepository.save(
      Cas2v2AssessmentEntity(
        id = UUID.randomUUID(),
        createdAt = OffsetDateTime.now(),
        application = cas2v2ApplicationEntity,
      ),
    )

  fun updateAssessment(
    assessmentId: UUID,
    newAssessment: UpdateCas2Assessment,
  ): CasResult<Cas2v2AssessmentEntity> {
    val assessmentEntity = cas2AssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound()

    assessmentEntity.apply {
      this.nacroReferralId = newAssessment.nacroReferralId
      this.assessorName = newAssessment.assessorName
    }

    val savedAssessment = cas2AssessmentRepository.save(assessmentEntity)

    return CasResult.Success(savedAssessment)
  }

  fun getAssessment(assessmentId: UUID): CasResult<Cas2v2AssessmentEntity> {
    val assessmentEntity = cas2AssessmentRepository.findByIdOrNull(assessmentId)
      ?: return CasResult.NotFound()

    return CasResult.Success(assessmentEntity)
  }
}
