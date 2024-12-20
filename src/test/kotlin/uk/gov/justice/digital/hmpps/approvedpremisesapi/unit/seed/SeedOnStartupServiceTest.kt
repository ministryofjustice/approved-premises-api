package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.seed

import io.mockk.Called
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.SeedConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedOnStartupService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.ApprovedPremisesRoomsSeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1SeedPremisesFromCsvJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2.Cas2AutoScript
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.SentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry

class SeedOnStartupServiceTest {
  private val seedConfig = SeedConfig()
  private val mockApplicationContext = mockk<ApplicationContext>()
  private val mockTransactionTemplate = mockk<TransactionTemplate>()
  private val mockSeedLogger = mockk<SeedLogger>()
  private val mockCas1AutoScript = mockk<Cas1AutoScript>()
  private val mockCas2AutoScript = mockk<Cas2AutoScript>()
  private val mockSeedService = mockk<SeedService>()
  private val mockEnvironmentService = mockk<EnvironmentService>()
  private val mockSentryService = mockk<SentryService>()
  private val logEntries = mutableListOf<LogEntry>()

  private val seedService = SeedOnStartupService(
    seedConfig,
    mockCas1AutoScript,
    mockCas2AutoScript,
    mockSeedService,
    mockSeedLogger,
    mockEnvironmentService,
    mockSentryService,
  )

  @BeforeEach
  fun setUp() {
    every { mockSeedLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
    }
    every { mockSeedLogger.warn(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "warn", null)
    }
    every { mockSeedLogger.error(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", null)
    }
    every { mockSeedLogger.error(any(), any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", it.invocation.args[1] as Throwable)
    }
    every { mockCas1AutoScript.script() } answers { }
    every { mockCas2AutoScript.script() } answers { }
  }

  @Test
  fun `autoSeed does nothing if automatic seeding is not enabled`() {
    seedConfig.auto.enabled = false

    every { mockEnvironmentService.isNotATestEnvironment() } returns false

    seedService.seedOnStartup()

    verify { listOf(mockApplicationContext, mockTransactionTemplate, mockSeedLogger) wasNot called }
  }

  @Test
  fun `autoSeed does nothing and raises alert if not in a test environment`() {
    seedConfig.auto.enabled = true

    every { mockEnvironmentService.isNotATestEnvironment() } returns true
    every { mockSentryService.captureErrorMessage(any()) } returns Unit

    seedService.seedOnStartup()

    verify { mockSentryService.captureErrorMessage("Auto seeding should not be enabled outside of local and dev environments") }
  }

  @Test
  fun `autoSeed logs a warning if a file prefix corresponds to a location that does not exist`() {
    seedConfig.auto.enabled = true
    seedConfig.auto.filePrefixes = listOf("classpath:does/not/exist")

    every { mockEnvironmentService.isNotATestEnvironment() } returns false

    seedService.seedOnStartup()

    assertThat(logEntries).anyMatch {
      it.level == "warn" &&
        it.message == "class path resource [does/not/exist/] cannot be resolved to URL because it does not exist"
    }
  }

  @Test
  fun `autoSeed logs a warning if it encounters a CSV file with an unknown seed job type`() {
    seedConfig.auto.enabled = true
    seedConfig.auto.filePrefixes = listOf("classpath:db/seed/unknown-job-type")

    every { mockEnvironmentService.isNotATestEnvironment() } returns false

    seedService.seedOnStartup()

    assertThat(logEntries).anyMatch {
      it.level == "warn" &&
        it.message.contains("/db/seed/unknown-job-type/unknown_seed_file.csv does not have a known job type; skipping.") ||
        it.message.contains("\\db\\seed\\unknown-job-type\\unknown_seed_file.csv does not have a known job type; skipping.")
    }
  }

  @Test
  fun `autoSeed runs the jobs for a CSV file with the name of the job type in the correct order`() {
    seedConfig.auto.enabled = true
    seedConfig.auto.filePrefixes = listOf("classpath:db/seed/known-job-type")

    every { mockEnvironmentService.isNotATestEnvironment() } returns false

    val mockPremisesRepository = mockk<PremisesRepository>()
    val mockProbationRegionRepository = mockk<ProbationRegionRepository>()
    val mockLocalAuthorityAreaRepository = mockk<LocalAuthorityAreaRepository>()
    val mockCharacteristicRepository = mockk<CharacteristicRepository>()
    val mockRoomRepository = mockk<RoomRepository>()
    val mockBedRepository = mockk<BedRepository>()

    every { mockApplicationContext.getBean(PremisesRepository::class.java) } returns mockPremisesRepository
    every { mockApplicationContext.getBean(ProbationRegionRepository::class.java) } returns mockProbationRegionRepository
    every { mockApplicationContext.getBean(LocalAuthorityAreaRepository::class.java) } returns mockLocalAuthorityAreaRepository
    every { mockApplicationContext.getBean(CharacteristicRepository::class.java) } returns mockCharacteristicRepository
    every { mockApplicationContext.getBean(RoomRepository::class.java) } returns mockRoomRepository
    every { mockApplicationContext.getBean(BedRepository::class.java) } returns mockBedRepository

    val spy = spyk(seedService, recordPrivateCalls = true)

    val approvedPremisesLambda = slot<SeedJob<*>.() -> String>()
    val approvedPremisesRoomLambda = slot<SeedJob<*>.() -> String>()

    every { mockSeedService.seedData(SeedFileType.approvedPremises, "approved_premises", capture(approvedPremisesLambda)) } returns Unit
    every { mockSeedService.seedData(SeedFileType.approvedPremisesRooms, "approved_premises_rooms", capture(approvedPremisesRoomLambda)) } returns Unit

    spy.seedOnStartup()

    verifyOrder {
      mockSeedService.seedData(SeedFileType.approvedPremises, "approved_premises", any<SeedJob<*>.() -> String>())
      mockSeedService.seedData(SeedFileType.approvedPremisesRooms, "approved_premises_rooms", any<SeedJob<*>.() -> String>())
    }

    val approvedPremisesFilename = approvedPremisesLambda.captured.invoke(
      Cas1SeedPremisesFromCsvJob(
        mockApplicationContext.getBean(PremisesRepository::class.java),
        mockApplicationContext.getBean(ProbationRegionRepository::class.java),
        mockApplicationContext.getBean(LocalAuthorityAreaRepository::class.java),
        mockApplicationContext.getBean(CharacteristicRepository::class.java),
      ),
    ).split("/").last()

    val approvedPremisesRoomsFilename = approvedPremisesRoomLambda.captured.invoke(
      ApprovedPremisesRoomsSeedJob(
        mockApplicationContext.getBean(PremisesRepository::class.java),
        mockApplicationContext.getBean(RoomRepository::class.java),
        mockApplicationContext.getBean(BedRepository::class.java),
        mockApplicationContext.getBean(CharacteristicRepository::class.java),
      ),
    ).split("/").last()

    assertThat(approvedPremisesFilename).isEqualTo("1__approved_premises.csv")
    assertThat(approvedPremisesRoomsFilename).isEqualTo("2__approved_premises_rooms.csv")
  }

  @Nested
  inner class AutoScriptCas1 {
    @Test
    fun `does nothing if autoScript is NOT enabled`() {
      seedConfig.auto.enabled = true
      seedConfig.autoScript.cas1Enabled = false

      every { mockEnvironmentService.isNotATestEnvironment() } returns false

      seedService.seedOnStartup()

      verify { mockCas1AutoScript wasNot Called }
      verify { mockCas2AutoScript wasNot Called }
    }

    @Test
    fun `runs Cas1AutoScript when autoScript IS enabled (along with auto-seeding)`() {
      seedConfig.auto.enabled = true
      seedConfig.autoScript.cas1Enabled = true

      every { mockEnvironmentService.isNotATestEnvironment() } returns false

      seedService.seedOnStartup()

      verify { mockCas1AutoScript.script() }
      verify { mockCas2AutoScript wasNot Called }
    }
  }

  @Nested
  inner class AutoScriptCas2 {
    @Test
    fun `does nothing if autoScript is NOT enabled`() {
      seedConfig.auto.enabled = true
      seedConfig.autoScript.cas2Enabled = false

      every { mockEnvironmentService.isNotATestEnvironment() } returns false

      seedService.seedOnStartup()

      verify { mockCas1AutoScript wasNot Called }
      verify { mockCas2AutoScript wasNot Called }
    }

    @Test
    fun `runs Cas2AutoScript when autoScript IS enabled (along with auto-seeding)`() {
      seedConfig.auto.enabled = true
      seedConfig.autoScript.cas2Enabled = true

      every { mockEnvironmentService.isNotATestEnvironment() } returns false

      seedService.seedOnStartup()

      verify { mockCas1AutoScript wasNot Called }
      verify { mockCas2AutoScript.script() }
    }
  }
}
