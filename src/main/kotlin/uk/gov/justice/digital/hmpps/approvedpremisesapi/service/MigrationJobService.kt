package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import io.sentry.Sentry
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.BookingStatusMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.NoticeTypeMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateAllUsersFromDeliusJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateSentenceTypeAndSituationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.UpdateUsersPduJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1BackfillUserApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1FixPlacementApplicationLinksJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1TaskDueMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1.Cas1UpdateApplicationLicenceExpiryDateJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2AssessmentMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2NoteMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas2.Cas2StatusUpdateMigrationJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateApplicationOffenderNameJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas3.Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob
import kotlin.reflect.KClass

@Service
class MigrationJobService(
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val migrationLogger: MigrationLogger,
) {
  @Async
  fun runMigrationJobAsync(migrationJobType: MigrationJobType) = runMigrationJob(migrationJobType, 50)

  @SuppressWarnings("CyclomaticComplexMethod")
  fun runMigrationJob(migrationJobType: MigrationJobType, pageSize: Int = 10) {
    migrationLogger.info("Starting migration job request: $migrationJobType")

    try {
      val job: MigrationJob = when (migrationJobType) {
        MigrationJobType.allUsersFromCommunityApi -> getBean(UpdateAllUsersFromDeliusJob::class)
        MigrationJobType.sentenceTypeAndSituation -> getBean(UpdateSentenceTypeAndSituationJob::class)
        MigrationJobType.bookingStatus -> getBean(BookingStatusMigrationJob::class)
        MigrationJobType.taskDueDates -> getBean(Cas1TaskDueMigrationJob::class)
        MigrationJobType.usersPduByApi -> getBean(UpdateUsersPduJob::class)
        MigrationJobType.cas2ApplicationsWithAssessments -> getBean(Cas2AssessmentMigrationJob::class)
        MigrationJobType.cas2StatusUpdatesWithAssessments -> getBean(Cas2StatusUpdateMigrationJob::class)
        MigrationJobType.cas2NotesWithAssessments -> getBean(Cas2NoteMigrationJob::class)
        MigrationJobType.cas1FixPlacementAppLinks -> getBean(Cas1FixPlacementApplicationLinksJob::class)
        MigrationJobType.cas1NoticeTypes -> getBean(NoticeTypeMigrationJob::class)
        MigrationJobType.cas1BackfillUserApArea -> getBean(Cas1BackfillUserApArea::class)
        MigrationJobType.cas3ApplicationOffenderName -> getBean(Cas3UpdateApplicationOffenderNameJob::class)
        MigrationJobType.cas3DomainEventTypeForPersonDepartedUpdated -> getBean(Cas3UpdateDomainEventTypeForPersonDepartureUpdatedJob::class)
        MigrationJobType.cas1ApplicationsLicenceExpiryDate -> getBean(Cas1UpdateApplicationLicenceExpiryDateJob::class)
      }

      if (job.shouldRunInTransaction) {
        transactionTemplate.executeWithoutResult { job.process(pageSize) }
      } else {
        job.process(pageSize)
      }

      migrationLogger.info("Finished migration job: $migrationJobType")
    } catch (exception: Exception) {
      Sentry.captureException(exception)
      migrationLogger.error("Unable to complete Migration Job", exception)
    }
  }

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)
}
