package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesBookingCancelSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesRoomsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BackfillActiveSpaceBookingsCreatedInDelius
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingToSpaceBookingSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CreateTestApplicationsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CruManagementAreaSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DomainEventReplaySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DuplicateApplicationSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1FurtherInfoBugFixSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1ImportDeliusReferralsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1LinkedBookingToPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1PlanSpacePlanningDryRunSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemoveAssessmentDetailsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SeedPremisesFromCsvJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SoftDeleteApplicationTimelineNotes
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateEventNumberSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateSpaceBookingSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1WithdrawPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2ApplicationsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.ExternalUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.NomisUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3ReferralRejectionSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.Cas3UsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.TemporaryAccommodationBedspaceSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3.TemporaryAccommodationPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.findRootCause
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Semaphore
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass

@Service
class SeedService(
  private val seedConfig: SeedConfig,
  private val applicationContext: ApplicationContext,
  private val transactionTemplate: TransactionTemplate,
  private val seedLogger: SeedLogger,
) {
  @Async
  fun seedDataAsync(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename)

  fun seedData(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename) { "${seedConfig.filePrefix}/$filename" }

  @SuppressWarnings("CyclomaticComplexMethod", "TooGenericExceptionThrown", "TooGenericExceptionCaught")
  fun seedData(seedFileType: SeedFileType, filename: String, resolveCsvPath: SeedJob<*>.() -> String) {
    try {
      seedLogger.info("Starting seed request: $seedFileType - $filename")

      if (filename.contains("/") || filename.contains("\\")) {
        throw RuntimeException("Filename must be just the filename of a .csv file in the /seed directory, e.g. for /seed/upload.csv, just `upload` should be supplied")
      }

      val job: SeedJob<*> = when (seedFileType) {
        SeedFileType.approvedPremises -> getBean(Cas1SeedPremisesFromCsvJob::class)
        SeedFileType.approvedPremisesRooms -> getBean(ApprovedPremisesRoomsSeedJob::class)
        SeedFileType.user -> getBean(UsersSeedJob::class)
        SeedFileType.usersBasic -> getBean(UsersBasicSeedJob::class)
        SeedFileType.nomisUsers -> getBean(NomisUsersSeedJob::class)
        SeedFileType.externalUsers -> getBean(ExternalUsersSeedJob::class)
        SeedFileType.cas2Applications -> getBean(Cas2ApplicationsSeedJob::class)
        SeedFileType.approvedPremisesUsers -> getBean(Cas1UsersSeedJob::class)
        SeedFileType.temporaryAccommodationUsers -> getBean(Cas3UsersSeedJob::class)
        SeedFileType.characteristics -> getBean(CharacteristicsSeedJob::class)
        SeedFileType.updateNomsNumber -> getBean(Cas1UpdateNomsNumberSeedJob::class)
        SeedFileType.temporaryAccommodationPremises -> getBean(TemporaryAccommodationPremisesSeedJob::class)
        SeedFileType.temporaryAccommodationBedspace -> getBean(TemporaryAccommodationBedspaceSeedJob::class)
        SeedFileType.approvedPremisesCancelBookings -> getBean(ApprovedPremisesBookingCancelSeedJob::class)
        SeedFileType.approvedPremisesAssessmentMoreInfoBugFix -> getBean(Cas1FurtherInfoBugFixSeedJob::class)
        SeedFileType.approvedPremisesRedactAssessmentDetails -> getBean(Cas1RemoveAssessmentDetailsSeedJob::class)
        SeedFileType.approvedPremisesWithdrawPlacementRequest -> getBean(Cas1WithdrawPlacementRequestSeedJob::class)
        SeedFileType.approvedPremisesReplayDomainEvents -> getBean(Cas1DomainEventReplaySeedJob::class)
        SeedFileType.approvedPremisesDuplicateApplication -> getBean(Cas1DuplicateApplicationSeedJob::class)
        SeedFileType.approvedPremisesUpdateEventNumber -> getBean(Cas1UpdateEventNumberSeedJob::class)
        SeedFileType.approvedPremisesLinkBookingToPlacementRequest -> getBean(Cas1LinkedBookingToPlacementRequestSeedJob::class)
        SeedFileType.approvedPremisesOutOfServiceBeds -> getBean(Cas1OutOfServiceBedSeedJob::class)
        SeedFileType.updateUsersFromApi -> getBean(UpdateUsersFromApiSeedJob::class)
        SeedFileType.approvedPremisesCruManagementAreas -> getBean(Cas1CruManagementAreaSeedJob::class)
        SeedFileType.approvedPremisesBookingToSpaceBooking -> getBean(Cas1BookingToSpaceBookingSeedJob::class)
        SeedFileType.approvedPremisesSpacePlanningDryRun -> getBean(Cas1PlanSpacePlanningDryRunSeedJob::class)
        SeedFileType.approvedPremisesImportDeliusReferrals -> getBean(Cas1ImportDeliusReferralsSeedJob::class)
        SeedFileType.approvedPremisesUpdateSpaceBooking -> getBean(Cas1UpdateSpaceBookingSeedJob::class)
        SeedFileType.approvedPremisesCreateTestApplications -> getBean(Cas1CreateTestApplicationsSeedJob::class)
        SeedFileType.temporaryAccommodationReferralRejection -> getBean(Cas3ReferralRejectionSeedJob::class)
        SeedFileType.approvedPremisesBackfillActiveSpaceBookingsCreatedInDelius -> getBean(Cas1BackfillActiveSpaceBookingsCreatedInDelius::class)
        SeedFileType.approvedPremisesDeleteApplicationTimelineNotes -> getBean(Cas1SoftDeleteApplicationTimelineNotes::class)
      }

      val seedStarted = LocalDateTime.now()

      if (job.runInTransaction && job.processRowsConcurrently) {
        error("Can't run a job in a transaction and process rows in parallel")
      }

      val rowsProcessed = if (job.runInTransaction) {
        transactionTemplate.execute { processJob(job, resolveCsvPath) }
      } else {
        processJob(job, resolveCsvPath)
      }

      val timeTaken = ChronoUnit.MILLIS.between(seedStarted, LocalDateTime.now())
      seedLogger.info("Seed request complete. Took $timeTaken millis and processed $rowsProcessed rows")
    } catch (exception: Throwable) {
      seedLogger.error("Unable to complete Seed Job", exception)
    }
  }

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)

  private fun <T> processJob(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String): Int {
    // During processing, the CSV file is processed one row at a time to avoid OOM issues.
    // It is preferable to fail fast rather than processing half of a file before stopping,
    // so we first do a full pass but only deserializing each row
    seedLogger.info("Processing CSV file ${Path.of(job.resolveCsvPath()).absolutePathString()}")
    enforcePresenceOfRequiredHeaders(job, resolveCsvPath)
    val rowCount = ensureCsvCanBeDeserialized(job, resolveCsvPath)

    job.preSeed()
    val rowsProcessed = processCsv(
      job = job,
      resolveCsvPath = resolveCsvPath,
      rowCount = rowCount,
    )
    job.postSeed()

    return rowsProcessed
  }

  @SuppressWarnings("TooGenericExceptionThrown", "MagicNumber", "ThrowsCount")
  private fun <T> processCsv(
    job: SeedJob<T>,
    resolveCsvPath: SeedJob<T>.() -> String,
    rowCount: Int,
  ): Int {
    val context = JobExecutionContext(job, rowCount, mutableListOf())
    var rowNumber = 0

    seedLogger.info("Processing $rowCount rows")

    try {
      csvReader().open(job.resolveCsvPath()) {
        if (!job.processRowsConcurrently) {
          readAllWithHeaderAsSequence().forEach { row ->
            rowNumber += 1
            processRow(context, row, rowNumber)
          }
        } else {
          runBlocking(Dispatchers.IO) {
            /*
            We use a semaphore to ensure there are only 5 processing or pending co-routines at
            any moment in time when processing the CSV. We found that if a coroutine was created
            for every row in a large CSV file we encountered out of memory exceptions.

            If we were to use a CSV reader that allowed us to stream the CSV rows instead of loading
            them all in to memory, this restriction may not be required.
             */
            val coroutineCreationSemaphore = Semaphore(5)
            readAllWithHeaderAsSequence().forEach { row ->
              rowNumber += 1
              coroutineCreationSemaphore.acquire()
              launch {
                try {
                  processRow(context, row, rowNumber)
                } finally {
                  coroutineCreationSemaphore.release()
                }
              }
            }
          }
        }
      }
    } catch (exception: Exception) {
      throw RuntimeException("Unable to process CSV at row $rowNumber", exception)
    }

    if (context.errors.isNotEmpty()) {
      throw RuntimeException("The following row-level errors were raised: ${context.errors.joinToString("\n")}")
    }

    return rowNumber
  }

  data class JobExecutionContext<T>(
    val job: SeedJob<T>,
    val rowCount: Int,
    val errors: MutableList<String>,
  )

  @SuppressWarnings("MagicNumber")
  private fun <T> processRow(
    context: JobExecutionContext<T>,
    row: Map<String, String>,
    rowNumber: Int,
  ) {
    val deserializedRow = context.job.deserializeRow(row)
    try {
      context.job.processRow(deserializedRow)
    } catch (exception: RuntimeException) {
      val rootCauseException = findRootCause(exception)
      context.errors.add("Error on row $rowNumber: ${exception.message} ${if (rootCauseException != null) rootCauseException.message else "no exception cause"}")
      seedLogger.error("Error on row $rowNumber:", exception)
    } finally {
      if ((rowNumber % 10_000) == 0) {
        seedLogger.info("Have processed $rowNumber of ${context.rowCount} rows")
      }
    }
  }

  private fun <T> enforcePresenceOfRequiredHeaders(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    seedLogger.info("Checking that required headers are present...")

    val headerRow = try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().first().keys
      }
    } catch (exception: Exception) {
      throw RuntimeException("There was an issue opening the CSV file", exception)
    }

    try {
      job.verifyPresenceOfRequiredHeaders(headerRow)
    } catch (exception: Exception) {
      throw RuntimeException("The headers provided: $headerRow did not include ${exception.message}")
    }
  }

  private fun <T> ensureCsvCanBeDeserialized(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String): Int {
    seedLogger.info("Validating that CSV can be fully read")
    var rowNumber = 0
    val errors = mutableListOf<String>()

    try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().forEach { row ->
          rowNumber += 1
          try {
            job.deserializeRow(row)
          } catch (exception: Exception) {
            errors += "Unable to deserialize CSV at row: $rowNumber: ${exception.message} ${exception.stackTrace.joinToString("\n")}"
          }
        }
      }
    } catch (exception: Exception) {
      throw RuntimeException("There was an issue opening the CSV file", exception)
    }

    if (errors.any()) {
      throw RuntimeException("There were issues deserializing the CSV:\n${errors.joinToString(", \n")}")
    }

    return rowNumber
  }
}
