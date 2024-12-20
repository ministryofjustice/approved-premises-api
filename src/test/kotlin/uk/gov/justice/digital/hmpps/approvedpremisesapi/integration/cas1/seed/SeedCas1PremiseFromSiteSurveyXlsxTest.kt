package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.cas1.seed

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.SeedFromExcelFileType
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenABooking
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAUser
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.givens.givenAnApplication
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.seed.SeedTestBase
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import java.util.UUID

class SeedCas1PremiseFromSiteSurveyXlsxTest : SeedTestBase() {

  lateinit var postCodeDistrict: PostCodeDistrictEntity

  @BeforeEach
  fun setup() {
    postCodeDistrict = postCodeDistrictFactory.produceAndPersist {
      withOutcode("LE12")
    }
  }

  @Test
  fun `create new premise - all characteristics`() {
    val header = listOf("Name of AP", "The Premise Name")
    val rows = mutableListOf(
      "AP Identifier (Q No.)", "Q123",
      "AP Area", "Irrelevant",
      "Probation Delivery Unit", "Irrelevant",
      "Probation Region", "Yorks & The Humber",
      "Local Authority Area", "Bournemouth",
      "Town / City", "Narnia",
      "Address", "123 Made Up Town",
      "Postcode", "LE12 ABC",
      "Male / Female AP?", "Male",
      "Total number of beds (including any beds out of service)", "22",
      "Is this an IAP?", "Yes",
      "Is this AP a PIPE?", "Yes",
      "Is this AP an Enhanced Security Site?", "Yes",
      "Is this AP semi specialist - Mental Health?", "Yes",
      "Is this a Recovery Focussed AP?", "Yes",
      "Is this AP suitable for people at risk of criminal exploitation? N.B Enhanced Security sites answer No, other AP's answer Yes.", "Yes",
      "Does this AP accept people who have committed sexual offences against adults?", "Yes",
      "Does this AP accept people who have committed sexual offences against children?", "Yes",
      "Does this AP accept people who have committed non-sexual offences against children?", "Yes",
      "Does this AP accept people who have been convicted of hate crimes?", "Yes",
      "Is this AP Catered? Self catering AP's answer 'No'", "Yes",
      "Is there a step free entrance to the AP at least 900mm wide?", "yes",
      "Are corridors leading to communal areas at least 1.2m wide?", "yes",
      "Do corridors leading to communal areas have step free access?", "yes",
      "Does this AP have bathroom facilities that have been adapted for wheelchair users?", "Yes",
      "Is there a lift at this AP?", "Yes",
      "Does this AP have tactile & directional flooring?", "Yes",
      "Does this AP have signs in braille?", "Yes",
      "Does this AP have or has access to a hearing loop?", "Yes",
      "Are there any additional restrictions on people that this AP can accommodate?", "Some useful notes here",
    )

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet2", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.cas1ImportSiteSurveyPremise,
      UUID.randomUUID(),
      "example.xlsx",
    )

    val createdPremise = approvedPremisesRepository.findByQCode("Q123")!!
    assertThat(createdPremise.name).isEqualTo("The Premise Name")
    assertThat(createdPremise.addressLine1).isEqualTo("123 Made Up Town")
    assertThat(createdPremise.addressLine2).isNull()
    assertThat(createdPremise.town).isEqualTo("Narnia")
    assertThat(createdPremise.postcode).isEqualTo("LE12 ABC")
    assertThat(createdPremise.longitude?.toFloat()).isEqualTo(postCodeDistrict.longitude.toFloat())
    assertThat(createdPremise.latitude?.toFloat()).isEqualTo(postCodeDistrict.latitude.toFloat())
    assertThat(createdPremise.notes).isEqualTo("")
    assertThat(createdPremise.emailAddress).isNull()
    assertThat(createdPremise.probationRegion.name).isEqualTo("Yorkshire & The Humber")
    assertThat(createdPremise.localAuthorityArea?.name).isEqualTo("Bournemouth, Christchurch and Poole")
    assertThat(createdPremise.bookings).isEmpty()
    assertThat(createdPremise.lostBeds).isEmpty()
    assertThat(createdPremise.rooms).isEmpty()
    assertThat(createdPremise.status).isEqualTo(PropertyStatus.active)
    assertThat(createdPremise.gender).isEqualTo(ApprovedPremisesGender.MAN)
    assertThat(createdPremise.supportsSpaceBookings).isEqualTo(false)
    assertThat(createdPremise.managerDetails).isNull()

    assertThat(createdPremise.characteristics).hasSize(19)

    assertCharacteristics(
      createdPremise,
      listOf(
        "isIAP",
        "isPIPE",
        "isESAP",
        "isSemiSpecialistMentalHealth",
        "isRecoveryFocussed",
        "isSuitableForVulnerable",
        "acceptsSexOffenders",
        "acceptsChildSexOffenders",
        "acceptsNonSexualChildOffenders",
        "acceptsHateCrimeOffenders",
        "isCatered",
        "hasWideStepFreeAccess",
        "hasWideAccessToCommunalAreas",
        "hasStepFreeAccessToCommunalAreas",
        "hasWheelChairAccessibleBathrooms",
        "hasLift",
        "hasTactileFlooring",
        "hasBrailleSignage",
        "hasHearingLoop",
      ),
    )
  }

  @Test
  fun `create new premise - no characteristics`() {
    val header = listOf("Name of AP", "The Premise Name 2")
    val rows = mutableListOf(
      "AP Identifier (Q No.)", "Q123",
      "AP Area", "Irrelevant",
      "Probation Delivery Unit", "Irrelevant",
      "Probation Region", "North West",
      "Local Authority Area", "Bristol",
      "Town / City", "Narnia",
      "Address", "456 Nowhere",
      "Postcode", "LE12 XYZ",
      "Male / Female AP?", "Female",
      "Total number of beds (including any beds out of service)", "33",
      "Is this an IAP?", "No",
      "Is this AP a PIPE?", "No",
      "Is this AP an Enhanced Security Site?", "No",
      "Is this AP semi specialist - Mental Health?", "No",
      "Is this a Recovery Focussed AP?", "No",
      "Is this AP suitable for people at risk of criminal exploitation? N.B Enhanced Security sites answer No, other AP's answer Yes.", "No",
      "Does this AP accept people who have committed sexual offences against adults?", "No",
      "Does this AP accept people who have committed sexual offences against children?", "No",
      "Does this AP accept people who have committed non-sexual offences against children?", "No",
      "Does this AP accept people who have been convicted of hate crimes?", "No",
      "Is this AP Catered? Self catering AP's answer 'No'", "No",
      "Is there a step free entrance to the AP at least 900mm wide?", "no",
      "Are corridors leading to communal areas at least 1.2m wide?", "no",
      "Do corridors leading to communal areas have step free access?", "no",
      "Does this AP have bathroom facilities that have been adapted for wheelchair users?", "No",
      "Is there a lift at this AP?", "No",
      "Does this AP have tactile & directional flooring?", "No",
      "Does this AP have signs in braille?", "No",
      "Does this AP have or has access to a hearing loop?", "No",
      "Are there any additional restrictions on people that this AP can accommodate?", "Some useful notes here",
    )

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet2", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.cas1ImportSiteSurveyPremise,
      UUID.randomUUID(),
      "example.xlsx",
    )

    val createdPremise = approvedPremisesRepository.findByQCode("Q123")!!
    assertThat(createdPremise.name).isEqualTo("The Premise Name 2")
    assertThat(createdPremise.addressLine1).isEqualTo("456 Nowhere")
    assertThat(createdPremise.addressLine2).isNull()
    assertThat(createdPremise.town).isEqualTo("Narnia")
    assertThat(createdPremise.postcode).isEqualTo("LE12 XYZ")
    assertThat(createdPremise.longitude?.toFloat()).isEqualTo(postCodeDistrict.longitude.toFloat())
    assertThat(createdPremise.latitude?.toFloat()).isEqualTo(postCodeDistrict.latitude.toFloat())
    assertThat(createdPremise.notes).isEqualTo("")
    assertThat(createdPremise.emailAddress).isNull()
    assertThat(createdPremise.probationRegion.name).isEqualTo("North West")
    assertThat(createdPremise.localAuthorityArea?.name).isEqualTo("Bristol, City of")
    assertThat(createdPremise.bookings).isEmpty()
    assertThat(createdPremise.lostBeds).isEmpty()
    assertThat(createdPremise.rooms).isEmpty()
    assertThat(createdPremise.status).isEqualTo(PropertyStatus.active)
    assertThat(createdPremise.gender).isEqualTo(ApprovedPremisesGender.WOMAN)
    assertThat(createdPremise.supportsSpaceBookings).isEqualTo(false)
    assertThat(createdPremise.managerDetails).isNull()

    assertThat(createdPremise.characteristics).isEmpty()
  }

  @Test
  fun `update existing premise - rooms & bookings unaffected`() {
    val existingPremises = approvedPremisesEntityFactory.produceAndPersist {
      withQCode("QExisting")
      withName("Old Name")
      withAddressLine1("Old Address Line 1")
      withAddressLine2("Old Address Line 2")
      withTown("Old Town")
      withPostcode("XX1 YY2")
      withLongitude(1.0)
      withLatitude(2.0)
      withNotes("Some existing notes")
      withEmailAddress("existing@local")
      withProbationRegion(probationRegionRepository.findByName("Greater Manchester")!!)
      withLocalAuthorityArea(localAuthorityAreaRepository.findByName("Watford")!!)
      withStatus(PropertyStatus.pending)
      withGender(ApprovedPremisesGender.WOMAN)
      withSupportsSpaceBookings(true)
      withManagerDetails("some manager details")
      withCharacteristicsList(
        listOf(
          characteristicRepository.findCas1ByPropertyName("isPIPE")!!,
          characteristicRepository.findCas1ByPropertyName("isSuitableForVulnerable")!!,
          characteristicRepository.findCas1ByPropertyName("isCatered")!!,
          characteristicRepository.findCas1ByPropertyName("hasLift")!!,
        ),
      )
    }

    roomEntityFactory.produceAndPersist {
      withPremises(existingPremises)
      withCode("rc1")
    }

    givenABooking(
      crn = "CRN1",
      premises = existingPremises,
      application = givenAnApplication(
        createdByUser = givenAUser().first,
      ),
    )

    val header = listOf("Name of AP", "The Premise Name")
    val rows = mutableListOf(
      "AP Identifier (Q No.)", "QExisting",
      "AP Area", "Irrelevant",
      "Probation Delivery Unit", "Irrelevant",
      "Probation Region", "Yorks & The Humber",
      "Local Authority Area", "Windsor and Maidenhead",
      "Town / City", "Narnia",
      "Address", "123 Made Up Town",
      "Postcode", "LE12 ABC",
      "Male / Female AP?", "Male",
      "Total number of beds (including any beds out of service)", "22",
      "Is this an IAP?", "No",
      "Is this AP a PIPE?", "Yes",
      "Is this AP an Enhanced Security Site?", "No",
      "Is this AP semi specialist - Mental Health?", "No",
      "Is this a Recovery Focussed AP?", "No",
      "Is this AP suitable for people at risk of criminal exploitation? N.B Enhanced Security sites answer No, other AP's answer Yes.", "No",
      "Does this AP accept people who have committed sexual offences against adults?", "No",
      "Does this AP accept people who have committed sexual offences against children?", "No",
      "Does this AP accept people who have committed non-sexual offences against children?", "No",
      "Does this AP accept people who have been convicted of hate crimes?", "No",
      "Is this AP Catered? Self catering AP's answer 'No'", "No",
      "Is there a step free entrance to the AP at least 900mm wide?", "no",
      "Are corridors leading to communal areas at least 1.2m wide?", "No",
      "Do corridors leading to communal areas have step free access?", "No",
      "Does this AP have bathroom facilities that have been adapted for wheelchair users?", "No",
      "Is there a lift at this AP?", "Yes",
      "Does this AP have tactile & directional flooring?", "No",
      "Does this AP have signs in braille?", "No",
      "Does this AP have or has access to a hearing loop?", "No",
      "Are there any additional restrictions on people that this AP can accommodate?", "Some useful notes here",
    )

    val dataFrame = dataFrameOf(header, rows)

    withXlsx("example", "Sheet2", dataFrame)

    seedService.seedExcelData(
      SeedFromExcelFileType.cas1ImportSiteSurveyPremise,
      UUID.randomUUID(),
      "example.xlsx",
    )

    val createdPremise = approvedPremisesRepository.findByQCode("QExisting")!!
    assertThat(createdPremise.name).isEqualTo("The Premise Name")
    assertThat(createdPremise.addressLine1).isEqualTo("123 Made Up Town")
    assertThat(createdPremise.addressLine2).isNull()
    assertThat(createdPremise.town).isEqualTo("Narnia")
    assertThat(createdPremise.postcode).isEqualTo("LE12 ABC")
    assertThat(createdPremise.longitude?.toFloat()).isEqualTo(postCodeDistrict.longitude.toFloat())
    assertThat(createdPremise.latitude?.toFloat()).isEqualTo(postCodeDistrict.latitude.toFloat())
    assertThat(createdPremise.notes).isEqualTo("Some existing notes")
    assertThat(createdPremise.emailAddress).isEqualTo("existing@local")
    assertThat(createdPremise.probationRegion.name).isEqualTo("Yorkshire & The Humber")
    assertThat(createdPremise.localAuthorityArea?.name).isEqualTo("Windsor and Maidenhead")
    assertThat(createdPremise.bookings).hasSize(1)
    assertThat(createdPremise.lostBeds).isEmpty()
    assertThat(createdPremise.rooms).hasSize(1)
    assertThat(createdPremise.status).isEqualTo(PropertyStatus.pending)
    assertThat(createdPremise.gender).isEqualTo(ApprovedPremisesGender.MAN)
    assertThat(createdPremise.supportsSpaceBookings).isEqualTo(true)
    assertThat(createdPremise.managerDetails).isEqualTo("some manager details")

    assertThat(createdPremise.characteristics).hasSize(2)

    assertCharacteristics(
      createdPremise,
      listOf(
        "isPIPE",
        "hasLift",
      ),
    )
  }

  private fun assertCharacteristics(premise: ApprovedPremisesEntity, expectedPropertyNames: List<String>) {
    val characteristicPropertyNames = premise.characteristics.map { it.propertyName }
    assertThat(characteristicPropertyNames).containsExactlyInAnyOrder(*expectedPropertyNames.toTypedArray())
  }
}
