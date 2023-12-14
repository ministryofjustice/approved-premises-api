package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas2

import org.springframework.core.io.DefaultResourceLoader
import org.springframework.stereotype.Component
import org.springframework.util.FileCopyUtils
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2ApplicationStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.Cas2NewApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.reference.Cas2ApplicationStatusSeeding
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas2.JsonSchemaService
import java.io.IOException
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

const val EARLIEST_CREATION = 45
const val LATEST_CREATION = 15
const val EARLIEST_SUBMISSION = 1
const val LATEST_SUBMISSION = 5
const val FEWEST_UPDATES = 1
const val MOST_UPDATES = 6
const val MINUTES_PER_DAY = 60 * 24

@Component
class Cas2AutoScript(
  private val seedLogger: SeedLogger,
  private val nomisUserRepository: NomisUserRepository,
  private val applicationRepository: Cas2ApplicationRepository,
  private val externalUserRepository: ExternalUserRepository,
  private val statusUpdateRepository: Cas2StatusUpdateRepository,
  private val jsonSchemaService: JsonSchemaService,
) {
  fun script() {
    seedLogger.info("Auto-Scripting for CAS2")
    scriptApplications()
  }

  private fun scriptApplications() {
    seedLogger.info("Auto-Scripting CAS2 applications")
    nomisUserRepository.findAll().forEach { user ->
      listOf("IN_PROGRESS", "SUBMITTED", "IN_REVIEW").forEach { state ->
        createApplicationFor(applicant = user, state = state)
      }
    }
  }

  private fun createApplicationFor(applicant: NomisUserEntity, state: String) {
    seedLogger.info("Auto-scripting application for ${applicant.nomisUsername}, in state $state")
    val createdAt = randomDateTime()
    val submittedAt = if (state == "IN_PROGRESS") null else createdAt.plusDays(randomInt(EARLIEST_SUBMISSION, LATEST_SUBMISSION).toLong())
    val application = applicationRepository.save(
      Cas2ApplicationEntity(
        id = UUID.randomUUID(),
        crn = "X320741",
        nomsNumber = "G9542VP",
        createdAt = createdAt,
        createdByUser = applicant,
        data = dataFor(state = state, nomsNumber = "A1234AI"),
        document = documentFor(state = state, nomsNumber = "A1234AI"),
        submittedAt = submittedAt,
        schemaVersion = jsonSchemaService.getNewestSchema(Cas2ApplicationJsonSchemaEntity::class.java),
        schemaUpToDate = true,
        name = "Aadland Bertrand",
        dateOfBirth = LocalDate.parse("1978-01-06"),
        sex = "Male",
        personStatus = Cas2NewApplication.PersonStatus.inCustody.name,
        nationality = null,
        pncNumber = null,
        prisonName = null,
      ),
    )
    if (state == "IN_REVIEW") {
      val quantity = randomInt(FEWEST_UPDATES, MOST_UPDATES)
      seedLogger.info("Auto-scripting $quantity status updates for application ${application.id}")
      repeat(quantity) { idx -> createStatusUpdate(idx, application) }
    }
  }

  private fun createStatusUpdate(idx: Int, application: Cas2ApplicationEntity) {
    seedLogger.info("Auto-scripting status update $idx for application ${application.id}")
    val assessor = externalUserRepository.findAll().random()
    val status = findStatusAtPosition(idx)
    val update = statusUpdateRepository.save(
      Cas2StatusUpdateEntity(
        id = UUID.randomUUID(),
        application = application,
        assessor = assessor,
        description = status.description,
        label = status.label,
        statusId = status.id,
      ),
    )
    update.apply { this.createdAt = application.submittedAt!!.plusDays(idx + 1.toLong()) }
    statusUpdateRepository.save(update)
  }

  private fun findStatusAtPosition(idx: Int): Cas2ApplicationStatus {
    return Cas2ApplicationStatusSeeding.statusList()[idx]
  }

  private fun randomDateTime(minDays: Int = LATEST_CREATION, maxDays: Int = EARLIEST_CREATION): OffsetDateTime {
    return OffsetDateTime.now()
      .minusMinutes(randomInt(MINUTES_PER_DAY * minDays, MINUTES_PER_DAY * maxDays).toLong())
      .truncatedTo(ChronoUnit.SECONDS)
  }

  private fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)

  private fun dataFor(state: String, nomsNumber: String): String {
    if (state != "NOT_STARTED") {
      return dataFixtureFor(nomsNumber)
    }
    return "{}"
  }

  private fun documentFor(state: String, nomsNumber: String): String {
    if (listOf("SUBMITTED", "IN_REVIEW").contains(state)) {
      return documentFixtureFor(nomsNumber)
    }
    return "{}"
  }

  private fun dataFixtureFor(nomsNumber: String): String {
    return loadFixtureAsResource("data_$nomsNumber.json")
  }

  private fun documentFixtureFor(nomsNumber: String): String {
    return loadFixtureAsResource("document_$nomsNumber.json")
  }

  private fun loadFixtureAsResource(filename: String): String {
    val path = "db/seed/local+dev+test/cas2_application_data/$filename"
    val loader = DefaultResourceLoader()
    return try {
      val resource = loader.getResource(path)
      val reader = InputStreamReader(resource.inputStream, "UTF-8")
      FileCopyUtils.copyToString(reader)
    } catch (e: IOException) {
      seedLogger.warn("FAILED to load seed fixture: " + e.message!!)
      "{}"
    }
  }
}
