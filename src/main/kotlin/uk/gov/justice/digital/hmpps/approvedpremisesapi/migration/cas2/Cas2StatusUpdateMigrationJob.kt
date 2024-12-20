package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob

@Component
class Cas2StatusUpdateMigrationJob(
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val shouldRunInTransaction = true

  override fun process(pageSize: Int) {
    log.info("Starting Cas2 Status Update Migration process...")

    var hasNext = true
    var slice: Slice<Cas2StatusUpdateEntity>
    var page = 1

    while (hasNext) {
      log.info("Getting page $page for max page size $pageSize")
      slice = statusUpdateRepository.findAllStatusUpdatesWithoutAssessment(PageRequest.of(0, pageSize))
      slice.content.forEach { statusUpdate ->
        transactionTemplate.executeWithoutResult {
          log.info("Adding assessment to Status Update ${statusUpdate.id}")
          statusUpdate.assessment = statusUpdate.application.assessment
          statusUpdateRepository.save(
            statusUpdate,
          )
        }
      }
      hasNext = slice.hasNext()
      page += 1
    }
    log.info("CAS2 Status Update Migration process complete!")
  }
}
