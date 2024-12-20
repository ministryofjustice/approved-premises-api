package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.writeExcel
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.LogEntry
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class SeedTestBase : IntegrationTestBase() {
  @Autowired
  lateinit var seedService: SeedService

  @Value("\${seed.file-prefix}")
  lateinit var seedFilePrefix: String

  @MockkBean
  lateinit var mockSeedLogger: SeedLogger
  protected val logEntries = mutableListOf<LogEntry>()

  private val log = LoggerFactory.getLogger(this::class.java)

  @BeforeEach
  fun setUp() {
    every { mockSeedLogger.info(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "info", null)
      log.info(it.invocation.args[0] as String)
    }
    every { mockSeedLogger.error(any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", null)
      log.error(it.invocation.args[0] as String)
    }
    every { mockSeedLogger.error(any(), any()) } answers {
      logEntries += LogEntry(it.invocation.args[0] as String, "error", it.invocation.args[1] as Throwable)
      log.info(it.invocation.args[0] as String, it.invocation.args[1] as Throwable)
    }
  }

  protected fun generateCsvFile(fileName: String, contents: String) {
    withCsv(fileName, contents)
  }

  protected fun withCsv(csvName: String, contents: String) {
    if (!Files.isDirectory(Path(seedFilePrefix))) {
      Files.createDirectory(Path(seedFilePrefix))
    }
    Files.writeString(
      Path("$seedFilePrefix/$csvName.csv"),
      contents,
      StandardOpenOption.CREATE,
      StandardOpenOption.TRUNCATE_EXISTING,
    )
  }

  protected fun withXlsx(xlsxName: String, sheetName: String, dataFrame: DataFrame<*>) {
    if (!Files.isDirectory(Path(seedFilePrefix))) {
      Files.createDirectory(Path(seedFilePrefix))
    }
    dataFrame.writeExcel(
      "$seedFilePrefix/$xlsxName.xlsx",
      sheetName = sheetName,
    )
  }
}
