package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Cas2AssessmentMigrationJob(
  private val assessmentRepository: Cas2AssessmentRepository,
  private val applicationRepository: Cas2ApplicationRepository,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    log.info("Starting Cas2 Assessment Migration process...")

    var hasNext = true
    var slice: Slice<Cas2ApplicationEntity>

    while (hasNext) {
      slice = applicationRepository.findAllSubmittedApplicationsWithoutAssessments()
      slice.content.forEach { application ->
        transactionTemplate.executeWithoutResult {
          log.info("Saving assessment for ${application.id}")
          assessmentRepository.save(
            Cas2AssessmentEntity(
              id = UUID.randomUUID(),
              application = application,
              createdAt = OffsetDateTime.now(),
            ),
          )
        }
      }
      hasNext = slice.hasNext()
    }
    log.info("CAS2 Assessment Migration process complete!")
  }
}
