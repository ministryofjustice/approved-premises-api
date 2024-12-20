package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.cas3

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.MigrationJobType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CaseSummaryFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NameFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAProbationRegion
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOffender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.httpmocks.apDeliusContextAddListCaseSummaryToBulkResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.migration.MigrationJobTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository

class Cas3UpdateApplicationOffenderNameJobTest : MigrationJobTestBase() {

  @Autowired
  lateinit var applicationRepository: ApplicationRepository

  @Test
  fun `all applications offender name are updated from Community API`() {
    givenAnOffender(
      offenderDetailsConfigBlock = {
        withNomsNumber(null)
      },
    ) { offenderDetails, _ ->

      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val probationRegion = givenAProbationRegion()

      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val temporaryAccommodationApplications = generateSequence {
        temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withProbationRegion(probationRegion)
          withCreatedByUser(user)
        }
      }.take(10).toList()

      val offendersCrnAndName = temporaryAccommodationApplications.associateBy(
        keySelector = { it.crn },
        valueTransform = { NameFactory().produce() },
      )

      val cases = temporaryAccommodationApplications.map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(offendersCrnAndName[it.crn]!!)
          .produce()
      }

      apDeliusContextAddListCaseSummaryToBulkResponse(cases)

      migrationJobService.runMigrationJob(MigrationJobType.cas3ApplicationOffenderName, 10)

      temporaryAccommodationApplications.forEach {
        val application = applicationRepository.findTemporaryAccommodationApplicationById(it.id)!!
        val offenderName = "${offendersCrnAndName[it.crn]?.forename} ${offendersCrnAndName[it.crn]?.surname}"
        Assertions.assertThat(application.name).isEqualTo(offenderName)
      }
    }
  }

  @Test
  fun `all applications offender name are updated from Community API when delius name is empty`() {
    givenAnOffender(
      offenderDetailsConfigBlock = {
        withNomsNumber(null)
      },
    ) { offenderDetails, _ ->

      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val probationRegion = givenAProbationRegion()

      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val temporaryAccommodationApplications = generateSequence {
        temporaryAccommodationApplicationEntityFactory.produceAndPersist {
          withApplicationSchema(applicationSchema)
          withProbationRegion(probationRegion)
          withCreatedByUser(user)
        }
      }.take(10).toList()

      val offendersCrnAndName = temporaryAccommodationApplications.associateBy(
        keySelector = { it.crn },
        valueTransform = { NameFactory().produce() },
      )

      val cases = temporaryAccommodationApplications.map {
        CaseSummaryFactory()
          .withCrn(it.crn)
          .withName(offendersCrnAndName[it.crn]!!)
          .produce()
      }

      apDeliusContextAddListCaseSummaryToBulkResponse(cases)

      migrationJobService.runMigrationJob(MigrationJobType.cas3ApplicationOffenderName, 10)

      temporaryAccommodationApplications.forEach {
        val application = applicationRepository.findTemporaryAccommodationApplicationById(it.id)!!
        val offenderName = "${offendersCrnAndName[it.crn]?.forename} ${offendersCrnAndName[it.crn]?.surname}"
        Assertions.assertThat(application.name).isEqualTo(offenderName)
      }
    }
  }

  @Test
  fun `when offender is not found in Community Api throws an exception`() {
    givenAnOffender(
      offenderDetailsConfigBlock = {
        withNomsNumber(null)
      },
    ) { offenderDetails, _ ->

      val applicationSchema = temporaryAccommodationApplicationJsonSchemaEntityFactory.produceAndPersist {
        withPermissiveSchema()
      }

      val probationRegion = givenAProbationRegion()

      val user = userEntityFactory.produceAndPersist {
        withProbationRegion(probationRegion)
      }

      val temporaryAccommodationApplication = temporaryAccommodationApplicationEntityFactory.produceAndPersist {
        withApplicationSchema(applicationSchema)
        withProbationRegion(probationRegion)
        withCreatedByUser(user)
      }

      apDeliusContextAddListCaseSummaryToBulkResponse(listOf())

      migrationJobService.runMigrationJob(MigrationJobType.cas3ApplicationOffenderName, 10)

      Assertions.assertThat(logEntries)
        .withFailMessage("-> logEntries actually contains: $logEntries")
        .anyMatch {
          it.level == "error" &&
            it.message == "Unable to update offender name with crn ${temporaryAccommodationApplication.crn} for the application ${temporaryAccommodationApplication.id}" &&
            it.throwable != null &&
            it.throwable.message == "Offender not found"
        }

      val application =
        applicationRepository.findTemporaryAccommodationApplicationById(temporaryAccommodationApplication.id)!!
      Assertions.assertThat(application.name).isNull()
    }
  }
}
