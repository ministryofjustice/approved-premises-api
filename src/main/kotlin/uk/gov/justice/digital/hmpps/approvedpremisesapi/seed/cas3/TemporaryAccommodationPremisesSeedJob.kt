package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas3

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.appendCharacteristicIfSet
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.getCanonicalLocalAuthorityName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.getCanonicalRegionName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.trimToEmpty
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.trimToNull
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.CharacteristicService
import java.util.UUID

@Component
class TemporaryAccommodationPremisesSeedJob(
  private val premisesRepository: PremisesRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val probationDeliveryUnitRepository: ProbationDeliveryUnitRepository,
  private val characteristicService: CharacteristicService,
) : SeedJob<TemporaryAccommodationPremisesSeedCsvRow>() {
  private val log = LoggerFactory.getLogger(this::class.java)

  override fun deserializeRow(columns: Map<String, String>) = TemporaryAccommodationPremisesSeedCsvRow(
    name = columns["Property reference"]!!.trim(),
    addressLine1 = columns["Address Line 1"]!!.trim(),
    addressLine2 = columns["Address Line 2 (optional)"].trimToNull(),
    town = columns["City/Town"].trimToNull(),
    postcode = columns["Postcode"]!!.trim(),
    probationRegion = columns["Region"]!!,
    localAuthorityArea = columns["Local authority / Borough"]!!.trim(),
    pdu = columns["Probation delivery unit (PDU)"]!!.trim(),
    characteristics = getCharacteristics(columns),
    notes = columns["Optional notes"].trimToEmpty(),
    emailAddress = columns["Email Address"].trimToEmpty(),
  )

  override fun processRow(row: TemporaryAccommodationPremisesSeedCsvRow) {
    val existingPremises = premisesRepository.findByName(row.name, TemporaryAccommodationPremisesEntity::class.java) as TemporaryAccommodationPremisesEntity?

    val canonicalRegionName = getCanonicalRegionName(row.probationRegion)

    if (canonicalRegionName != row.probationRegion) {
      log.warn("'${row.probationRegion}' is not the canonical region name, correcting to '$canonicalRegionName'")
    }

    val probationRegion = probationRegionRepository.findByName(canonicalRegionName)
      ?: throw RuntimeException("Probation Region $canonicalRegionName does not exist")

    val canonicalLocalAuthorityName = getCanonicalLocalAuthorityName(row.localAuthorityArea)

    if (canonicalLocalAuthorityName != row.localAuthorityArea) {
      log.warn("'${row.localAuthorityArea}' is not the canonical local authority name, correcting to '$canonicalLocalAuthorityName'")
    }

    val localAuthorityArea = localAuthorityAreaRepository.findByName(canonicalLocalAuthorityName)
      ?: throw RuntimeException("Local Authority Area ${row.localAuthorityArea} does not exist")

    val characteristics = row.characteristics.map {
      characteristicService.getCharacteristics(it)
        .firstOrNull { it.serviceScope == "temporary-accommodation" }
        ?: throw RuntimeException("Characteristic $it does not exist")
    }

    val probationDeliveryUnit = probationDeliveryUnitRepository.findByNameAndProbationRegionId(row.pdu, probationRegion.id)
      ?: throw RuntimeException("Probation Delivery Unit ${row.pdu} does not exist")

    if (existingPremises != null) {
      updateExistingPremises(row, existingPremises, probationRegion, localAuthorityArea, probationDeliveryUnit, characteristics)
    } else {
      createNewPremises(row, probationRegion, localAuthorityArea, probationDeliveryUnit, characteristics)
    }
  }

  private fun createNewPremises(
    row: TemporaryAccommodationPremisesSeedCsvRow,
    probationRegion: ProbationRegionEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    probationDeliveryUnit: ProbationDeliveryUnitEntity,
    characteristics: List<CharacteristicEntity>,
  ) {
    log.info("Creating new Temporary Accommodation premises: ${row.name}")

    val premises = premisesRepository.save(
      TemporaryAccommodationPremisesEntity(
        id = UUID.randomUUID(),
        name = row.name,
        addressLine1 = row.addressLine1,
        addressLine2 = row.addressLine2,
        town = row.town,
        postcode = row.postcode,
        latitude = null,
        longitude = null,
        notes = row.notes,
        emailAddress = row.emailAddress,
        probationRegion = probationRegion,
        localAuthorityArea = localAuthorityArea,
        bookings = mutableListOf(),
        lostBeds = mutableListOf(),
        rooms = mutableListOf(),
        characteristics = mutableListOf(),
        probationDeliveryUnit = probationDeliveryUnit,
        status = PropertyStatus.active,
        turnaroundWorkingDayCount = 2,
      ),
    )

    characteristics.forEach {
      premises.characteristics.add(it)
    }

    premisesRepository.save(premises)
  }

  private fun updateExistingPremises(
    row: TemporaryAccommodationPremisesSeedCsvRow,
    existingPremises: TemporaryAccommodationPremisesEntity,
    probationRegion: ProbationRegionEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    probationDeliveryUnit: ProbationDeliveryUnitEntity,
    characteristics: List<CharacteristicEntity>,
  ) {
    log.info("Updating existing Temporary Accommodation premises: ${row.name}")

    existingPremises.apply {
      this.name = row.name
      this.addressLine1 = row.addressLine1
      this.addressLine2 = row.addressLine2
      this.town = row.town
      this.postcode = row.postcode
      this.notes = row.notes
      this.probationRegion = probationRegion
      this.localAuthorityArea = localAuthorityArea
      this.probationDeliveryUnit = probationDeliveryUnit
    }

    characteristics.forEach {
      if (!characteristicService.serviceScopeMatches(it, existingPremises)) {
        throw RuntimeException("Service scope does not match for Characteristic $it")
      }

      if (!characteristicService.modelScopeMatches(it, existingPremises)) {
        throw RuntimeException("Model scope does not match for Characteristic $it")
      }

      if (existingPremises.characteristics.none { existingCharacteristic -> existingCharacteristic.id == it.id }) {
        existingPremises.characteristics.add(it)
      }
    }

    premisesRepository.save(existingPremises)
  }

  private fun getCharacteristics(columns: Map<String, String>): List<String> {
    val characteristics = mutableListOf<String>()

    appendCharacteristicIfSet(columns, characteristics, "Ground floor level access", "Floor level access?")
    appendCharacteristicIfSet(columns, characteristics, "Wheelchair accessible")
    appendCharacteristicIfSet(columns, characteristics, "Pub nearby")
    appendCharacteristicIfSet(columns, characteristics, "Park nearby")
    appendCharacteristicIfSet(columns, characteristics, "School nearby")
    appendCharacteristicIfSet(columns, characteristics, "Women only")
    appendCharacteristicIfSet(columns, characteristics, "Men only")
    appendCharacteristicIfSet(columns, characteristics, "Not suitable for those who pose a sexual risk to adults")
    appendCharacteristicIfSet(columns, characteristics, "Not suitable for those who pose a sexual risk to children")
    appendCharacteristicIfSet(columns, characteristics, "Not suitable for those with an arson history")
    appendCharacteristicIfSet(columns, characteristics, "Single occupancy")
    appendCharacteristicIfSet(columns, characteristics, "Shared property")
    appendCharacteristicIfSet(columns, characteristics, "Shared entrance")
    appendCharacteristicIfSet(columns, characteristics, "Lift available")
    appendCharacteristicIfSet(columns, characteristics, "Sensitive let")
    appendCharacteristicIfSet(columns, characteristics, "Close proximity")
    appendCharacteristicIfSet(columns, characteristics, "Rural/out of town")
    appendCharacteristicIfSet(columns, characteristics, "Other – please state in notes")

    return characteristics.toList()
  }
}

data class TemporaryAccommodationPremisesSeedCsvRow(
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String?,
  val postcode: String,
  val probationRegion: String,
  val localAuthorityArea: String,
  val pdu: String,
  val characteristics: List<String>,
  val notes: String,
  val emailAddress: String?,
)
