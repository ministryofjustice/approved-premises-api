package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2PersistedApplicationStatusFinder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApStaffUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesBookingCancelSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesRoomsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1ApAreaEmailAddressSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1BookingAdhocPropertySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1CruManagementAreaSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DomainEventReplaySeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1DuplicateApplicationSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1FurtherInfoBugFixSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1LinkedBookingToPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1OutOfServiceBedSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1RemoveAssessmentDetailsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateEventNumberSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1WithdrawPlacementRequestSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2ApplicationsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.ExternalUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.NomisUsersSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.ApplicationTimelineNoteService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.BookingService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.DomainEventService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.OffenderService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PlacementRequestService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.PremisesService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.RoomService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1OutOfServiceBedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.findRootCause
import java.nio.file.Path
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

  fun seedData(seedFileType: SeedFileType, filename: String) = seedData(seedFileType, filename) { "${seedConfig.filePrefix}/${this.fileName}" }

  @SuppressWarnings("CyclomaticComplexMethod")
  fun seedData(seedFileType: SeedFileType, filename: String, resolveCsvPath: SeedJob<*>.() -> String) {
    seedLogger.info("Starting seed request: $seedFileType - $filename")

    try {
      val job: SeedJob<*> = when (seedFileType) {
        SeedFileType.approvedPremises -> ApprovedPremisesSeedJob(
          filename,
          getBean(PremisesRepository::class),
          getBean(ProbationRegionRepository::class),
          getBean(LocalAuthorityAreaRepository::class),
          getBean(CharacteristicRepository::class),
        )
        SeedFileType.approvedPremisesRooms -> ApprovedPremisesRoomsSeedJob(
          filename,
          getBean(PremisesRepository::class),
          getBean(RoomRepository::class),
          getBean(BedRepository::class),
          getBean(CharacteristicRepository::class),
        )
        SeedFileType.user -> UsersSeedJob(
          filename,
          ServiceName.values().toList(),
          getBean(UserService::class),
        )
        SeedFileType.approvedPremisesApStaffUsers -> ApStaffUsersSeedJob(
          filename,
          getBean(UserService::class),
          seedLogger,
        )
        SeedFileType.nomisUsers -> NomisUsersSeedJob(
          filename,
          getBean(NomisUserRepository::class),
        )
        SeedFileType.externalUsers -> ExternalUsersSeedJob(
          filename,
          getBean(ExternalUserRepository::class),
        )
        SeedFileType.cas2Applications -> Cas2ApplicationsSeedJob(
          filename,
          getBean(Cas2ApplicationRepository::class),
          getBean(NomisUserRepository::class),
          getBean(ExternalUserRepository::class),
          getBean(Cas2StatusUpdateRepository::class),
          getBean(Cas2AssessmentRepository::class),
          getBean(JsonSchemaService::class),
          getBean(Cas2PersistedApplicationStatusFinder::class),
        )
        SeedFileType.approvedPremisesUsers -> UsersSeedJob(
          filename,
          listOf(ServiceName.approvedPremises),
          getBean(UserService::class),
        )
        SeedFileType.temporaryAccommodationUsers -> UsersSeedJob(
          filename,
          listOf(ServiceName.temporaryAccommodation),
          getBean(UserService::class),
        )
        SeedFileType.characteristics -> CharacteristicsSeedJob(
          filename,
          getBean(CharacteristicRepository::class),
        )
        SeedFileType.updateNomsNumber -> Cas1UpdateNomsNumberSeedJob(
          filename,
          getBean(ApplicationRepository::class),
          getBean(ApplicationTimelineNoteService::class),
          getBean(BookingRepository::class),
        )
        SeedFileType.temporaryAccommodationPremises -> TemporaryAccommodationPremisesSeedJob(
          filename,
          getBean(PremisesRepository::class),
          getBean(ProbationRegionRepository::class),
          getBean(LocalAuthorityAreaRepository::class),
          getBean(ProbationDeliveryUnitRepository::class),
          getBean(CharacteristicService::class),
        )
        SeedFileType.temporaryAccommodationBedspace -> TemporaryAccommodationBedspaceSeedJob(
          filename,
          getBean(PremisesRepository::class),
          getBean(CharacteristicService::class),
          getBean(RoomService::class),
        )

        SeedFileType.approvedPremisesCancelBookings -> ApprovedPremisesBookingCancelSeedJob(
          filename,
          getBean(BookingService::class),
          getBean(BookingRepository::class),
        )

        SeedFileType.approvedPremisesBookingAdhocProperty -> Cas1BookingAdhocPropertySeedJob(
          filename,
          getBean(BookingRepository::class),
        )

        SeedFileType.approvedPremisesAssessmentMoreInfoBugFix -> Cas1FurtherInfoBugFixSeedJob(
          filename,
          getBean(AssessmentRepository::class),
        )

        SeedFileType.approvedPremisesRedactAssessmentDetails -> Cas1RemoveAssessmentDetailsSeedJob(
          filename,
          getBean(AssessmentRepository::class),
          getBean(ObjectMapper::class),
          getBean(ApplicationService::class),
        )

        SeedFileType.approvedPremisesWithdrawPlacementRequest -> Cas1WithdrawPlacementRequestSeedJob(
          filename,
          getBean(PlacementRequestService::class),
          getBean(ApplicationService::class),
        )

        SeedFileType.approvedPremisesReplayDomainEvents -> Cas1DomainEventReplaySeedJob(
          filename,
          getBean(DomainEventService::class),
        )

        SeedFileType.approvedPremisesDuplicateApplication -> Cas1DuplicateApplicationSeedJob(
          filename,
          getBean(ApplicationService::class),
          getBean(OffenderService::class),
        )

        SeedFileType.approvedPremisesUpdateEventNumber -> Cas1UpdateEventNumberSeedJob(
          filename,
          getBean(ApplicationService::class),
          getBean(ApplicationRepository::class),
          getBean(DomainEventRepository::class),
          getBean(ObjectMapper::class),
        )

        SeedFileType.approvedPremisesLinkBookingToPlacementRequest -> Cas1LinkedBookingToPlacementRequestSeedJob(
          filename,
          getBean(PlacementRequestRepository::class),
          getBean(BookingRepository::class),
          getBean(ApplicationTimelineNoteService::class),
        )

        SeedFileType.approvedPremisesOutOfServiceBeds -> Cas1OutOfServiceBedSeedJob(
          filename,
          getBean(Cas1OutOfServiceBedService::class),
          getBean(PremisesService::class),
        )

        SeedFileType.updateUsersFromApi -> UpdateUsersFromApiSeedJob(
          filename,
          getBean(UserService::class),
        )

        SeedFileType.approvedPremisesCruManagementAreas -> Cas1CruManagementAreaSeedJob(
          filename,
          getBean(Cas1CruManagementAreaRepository::class),
        )
      }

      transactionTemplate.executeWithoutResult { processJob(job, resolveCsvPath) }

      seedLogger.info("Seed request complete")
    } catch (exception: Exception) {
      seedLogger.error("Unable to complete Seed Job", exception)
    }
  }

  private fun <T : Any> getBean(clazz: KClass<T>) = applicationContext.getBean(clazz.java)

  private fun <T> processJob(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    // During processing, the CSV file is processed one row at a time to avoid OOM issues.
    // It is preferable to fail fast rather than processing half of a file before stopping,
    // so we first do a full pass but only deserializing each row
    seedLogger.info("Processing CSV file ${Path.of(job.resolveCsvPath()).absolutePathString()}")
    enforcePresenceOfRequiredHeaders(job, resolveCsvPath)
    ensureCsvCanBeDeserialized(job, resolveCsvPath)
    processCsv(job, resolveCsvPath)
  }

  private fun <T> processCsv(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    var rowNumber = 1
    val errors = mutableListOf<String>()

    try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().forEach { row ->
          val deserializedRow = job.deserializeRow(row)
          try {
            job.processRow(deserializedRow)
          } catch (exception: RuntimeException) {
            val rootCauseException = findRootCause(exception)
            errors.add("Error on row $rowNumber: ${exception.message} ${if (rootCauseException != null) rootCauseException.message else "no exception cause"}")
            seedLogger.error("Error on row $rowNumber:", exception)
          }

          rowNumber += 1
        }
      }
    } catch (exception: Exception) {
      throw RuntimeException("Unable to process CSV at row $rowNumber", exception)
    }
    if (errors.isNotEmpty()) {
      seedLogger.error("The following row-level errors were raised: ${errors.joinToString("\n")}")
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

  private fun <T> ensureCsvCanBeDeserialized(job: SeedJob<T>, resolveCsvPath: SeedJob<T>.() -> String) {
    seedLogger.info("Validating that CSV can be fully read")
    var rowNumber = 1
    val errors = mutableListOf<String>()

    try {
      csvReader().open(job.resolveCsvPath()) {
        readAllWithHeaderAsSequence().forEach { row ->
          try {
            job.deserializeRow(row)
          } catch (exception: Exception) {
            errors += "Unable to deserialize CSV at row: $rowNumber: ${exception.message} ${exception.stackTrace.joinToString("\n")}"
          }

          rowNumber += 1
        }
      }
    } catch (exception: Exception) {
      throw RuntimeException("There was an issue opening the CSV file", exception)
    }

    if (errors.any()) {
      throw RuntimeException("There were issues deserializing the CSV:\n${errors.joinToString(", \n")}")
    }
  }
}
