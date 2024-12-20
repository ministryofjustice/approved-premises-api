package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApArea
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.UpdateNomsNumberSeedRow
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.cas1.Cas1DomainEventService

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SeedUpdateNomsNumberSeedJobTest : SeedTestBase() {

  @Autowired
  lateinit var domainEventService: Cas1DomainEventService

  companion object CONSTANTS {
    const val CRN = "CRN123"
    const val OLD_NOMS = "NOMS123"
    const val NEW_NOMS = "NOMS456"
    const val OTHER_CRN = "OTHERCRN"
    const val OTHER_NOMS = "OTHERNOMS"
  }

  @Test
  fun `Update Application and Booking NOMS Numbers`() {
    val (applicant, _) = givenAUser()

    val application = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withDefaults()
      withCreatedByUser(applicant)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withApArea(givenAnApArea())
      withCrn(CRN)
      withNomsNumber(OLD_NOMS)
    }

    val alreadyCorrectApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withDefaults()
      withCreatedByUser(applicant)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withApArea(givenAnApArea())
      withCrn(CRN)
      withNomsNumber(NEW_NOMS)
    }

    val otherCrnApplication = approvedPremisesApplicationEntityFactory.produceAndPersist {
      withDefaults()
      withCreatedByUser(applicant)
      withApplicationSchema(
        approvedPremisesApplicationJsonSchemaEntityFactory.produceAndPersist {
          withPermissiveSchema()
        },
      )
      withApArea(givenAnApArea())
      withCrn(OTHER_CRN)
      withNomsNumber(OTHER_NOMS)
    }

    val booking = bookingEntityFactory.produceAndPersist {
      withPremises(
        approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { givenAnApArea() } }
          }
        },
      )
      withCrn(CRN)
      withNomsNumber(OLD_NOMS)
    }

    val otherCrnBooking = bookingEntityFactory.produceAndPersist {
      withPremises(
        approvedPremisesEntityFactory.produceAndPersist {
          withYieldedLocalAuthorityArea { localAuthorityEntityFactory.produceAndPersist() }
          withYieldedProbationRegion {
            probationRegionEntityFactory.produceAndPersist { withYieldedApArea { givenAnApArea() } }
          }
        },
      )
      withCrn(OTHER_CRN)
      withNomsNumber(OTHER_NOMS)
    }

    withCsv(
      "valid-csv",
      rowsToCsv(
        listOf(
          UpdateNomsNumberSeedRow(
            CRN,
            OLD_NOMS,
            NEW_NOMS,
          ),
        ),
      ),
    )

    seedService.seedData(SeedFileType.updateNomsNumber, "valid-csv.csv")

    val updatedApplication = approvedPremisesApplicationRepository.findByIdOrNull(application.id)
    assertThat(updatedApplication!!.nomsNumber).isEqualTo(NEW_NOMS)
    assertNoteAdded(updatedApplication, "NOMS Number for application updated from 'NOMS123' to 'NOMS456' by Application Support")

    val updatedAlreadyCorrectApplication = approvedPremisesApplicationRepository.findByIdOrNull(alreadyCorrectApplication.id)
    assertThat(updatedAlreadyCorrectApplication!!.nomsNumber).isEqualTo(NEW_NOMS)
    assertNoteNotAdded(updatedAlreadyCorrectApplication)

    val updatedOtherCrnApplication = approvedPremisesApplicationRepository.findByIdOrNull(otherCrnApplication.id)
    assertThat(updatedOtherCrnApplication!!.nomsNumber).isEqualTo(OTHER_NOMS)
    assertNoteNotAdded(updatedOtherCrnApplication)

    val updatedBooking = bookingRepository.findByIdOrNull(booking.id)
    assertThat(updatedBooking!!.nomsNumber).isEqualTo(NEW_NOMS)

    val updatedOtherCrnBooking = bookingRepository.findByIdOrNull(otherCrnBooking.id)
    assertThat(updatedOtherCrnBooking!!.nomsNumber).isEqualTo(OTHER_NOMS)
  }

  private fun assertNoteAdded(application: ApplicationEntity, message: String) {
    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationId(application.id)
    assertThat(notes).hasSize(1)
    assertThat(notes)
      .extracting("body")
      .contains(message)
  }

  private fun assertNoteNotAdded(application: ApplicationEntity) {
    val notes = applicationTimelineNoteRepository.findApplicationTimelineNoteEntitiesByApplicationId(application.id)
    assertThat(notes).isEmpty()
  }

  private fun rowsToCsv(rows: List<UpdateNomsNumberSeedRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "crn",
        "oldNomsNumber",
        "newNomsNumber",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.crn)
        .withQuotedField(it.oldNomsNumber)
        .withQuotedField(it.newNomsNumber)
        .newRow()
    }

    return builder.build()
  }
}
