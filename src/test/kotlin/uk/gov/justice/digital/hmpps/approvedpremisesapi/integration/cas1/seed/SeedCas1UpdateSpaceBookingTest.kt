package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenACas1SpaceBooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnOfflineApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.CsvBuilder
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1.Cas1UpdateSpaceBookingSeedJobCsvRow

class SeedCas1UpdateSpaceBookingTest : SeedTestBase() {

  @Test
  fun `Update Space Booking event number and criteria and add notes`() {
    val spaceBooking = givenACas1SpaceBooking(
      crn = "CRN1",
      deliusEventNumber = "101",
      offlineApplication = givenAnOfflineApplication("CRN1", eventNumberSet = false),
      criteria = listOf(
        getCharacteristic(CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM),
        getCharacteristic(CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_ENSUITE),
      ),
    )

    withCsv(
      "valid-csv",
      rowsToCsv(
        listOf(
          Cas1UpdateSpaceBookingSeedJobCsvRow(
            spaceBookingId = spaceBooking.id,
            updateEventNumber = true,
            eventNumber = "999",
            updateCriteria = true,
            criteria = listOf(
              CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM,
              CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED,
            ),
          ),
        ),
      ),
    )

    seedService.seedData(SeedFileType.approvedPremisesUpdateSpaceBooking, "valid-csv.csv")

    val updatedSpaceBooking = cas1SpaceBookingRepository.findById(spaceBooking.id).get()
    assertThat(updatedSpaceBooking.deliusEventNumber).isEqualTo("999")
    assertThat(updatedSpaceBooking.criteria).containsExactlyInAnyOrder(
      getCharacteristic(CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_SINGLE_ROOM),
      getCharacteristic(CharacteristicRepository.Constants.CAS1_PROPERTY_NAME_WHEELCHAIR_DESIGNATED),
    )
  }

  private fun getCharacteristic(propertyName: String) =
    characteristicRepository.findByPropertyName(propertyName, ServiceName.approvedPremises.value)!!

  private fun rowsToCsv(rows: List<Cas1UpdateSpaceBookingSeedJobCsvRow>): String {
    val builder = CsvBuilder()
      .withUnquotedFields(
        "space_booking_id",
        "update_event_number",
        "event_number",
        "update_criteria",
        "criteria",
      )
      .newRow()

    rows.forEach {
      builder
        .withQuotedField(it.spaceBookingId.toString())
        .withQuotedField(
          if (it.updateEventNumber) {
            "yes"
          } else {
            "no"
          },
        )
        .withQuotedField(it.eventNumber.toString())
        .withQuotedField(
          if (it.updateCriteria) {
            "yes"
          } else {
            "no"
          },
        )
        .withQuotedField(it.criteria.joinToString(","))
        .newRow()
    }

    return builder.build()
  }
}
