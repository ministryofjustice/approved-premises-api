package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedJob
import java.util.UUID

@Component
class Cas1SeedPremisesFromCsvJob(
  private val premisesRepository: PremisesRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val characteristicRepository: CharacteristicRepository,
) : SeedJob<ApprovedPremisesSeedCsvRow>(
  requiredHeaders = setOf(
    "name",
    "addressLine1",
    "addressLine2",
    "town",
    "postcode",
    "notes",
    "emailAddress",
    "probationRegion",
    "localAuthorityArea",
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
    "status",
    "apCode",
    "qCode",
    "latitude",
    "longitude",
    "gender",
    "supportsSpaceBookings",
    "managerDetails",
  ),
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  companion object Constants {
    private const val LAT_LON_SRID = 4326
  }

  private val geometryFactory = GeometryFactory(PrecisionModel(PrecisionModel.FLOATING), LAT_LON_SRID)

  override fun deserializeRow(columns: Map<String, String>) = ApprovedPremisesSeedCsvRow(
    name = columns["name"]!!,
    addressLine1 = columns["addressLine1"]!!,
    addressLine2 = columns["addressLine2"]!!,
    town = columns["town"]!!,
    postcode = columns["postcode"]!!,
    notes = columns["notes"]!!,
    emailAddress = columns["emailAddress"]!!,
    probationRegion = columns["probationRegion"]!!,
    localAuthorityArea = columns["localAuthorityArea"]!!,
    isIAP = parseBooleanStringOrThrow(columns["isIAP"]!!, "isIAP"),
    isPIPE = parseBooleanStringOrThrow(columns["isPIPE"]!!, "isPIPE"),
    isESAP = parseBooleanStringOrThrow(columns["isESAP"]!!, "isESAP"),
    isSemiSpecialistMentalHealth = parseBooleanStringOrThrow(columns["isSemiSpecialistMentalHealth"]!!, "isSemiSpecialistMentalHealth"),
    isRecoveryFocussed = parseBooleanStringOrThrow(columns["isRecoveryFocussed"]!!, "isRecoveryFocussed"),
    isSuitableForVulnerable = parseBooleanStringOrThrow(columns["isSuitableForVulnerable"]!!, "isSuitableForVulnerable"),
    acceptsSexOffenders = parseBooleanStringOrThrow(columns["acceptsSexOffenders"]!!, "acceptsSexOffenders"),
    acceptsChildSexOffenders = parseBooleanStringOrThrow(columns["acceptsChildSexOffenders"]!!, "acceptsChildSexOffenders"),
    acceptsNonSexualChildOffenders = parseBooleanStringOrThrow(columns["acceptsNonSexualChildOffenders"]!!, "acceptsNonSexualChildOffenders"),
    acceptsHateCrimeOffenders = parseBooleanStringOrThrow(columns["acceptsHateCrimeOffenders"]!!, "acceptsHateCrimeOffenders"),
    isCatered = parseBooleanStringOrThrow(columns["isCatered"]!!, "isCatered"),
    hasWideStepFreeAccess = parseBooleanStringOrThrow(columns["hasWideStepFreeAccess"]!!, "hasWideStepFreeAccess"),
    hasWideAccessToCommunalAreas = parseBooleanStringOrThrow(columns["hasWideAccessToCommunalAreas"]!!, "hasWideAccessToCommunalAreas"),
    hasStepFreeAccessToCommunalAreas = parseBooleanStringOrThrow(columns["hasStepFreeAccessToCommunalAreas"]!!, "hasStepFreeAccessToCommunalAreas"),
    hasWheelChairAccessibleBathrooms = parseBooleanStringOrThrow(columns["hasWheelChairAccessibleBathrooms"]!!, "hasWheelChairAccessibleBathrooms"),
    hasLift = parseBooleanStringOrThrow(columns["hasLift"]!!, "hasLift"),
    hasTactileFlooring = parseBooleanStringOrThrow(columns["hasTactileFlooring"]!!, "hasTactileFlooring"),
    hasBrailleSignage = parseBooleanStringOrThrow(columns["hasBrailleSignage"]!!, "hasBrailleSignage"),
    hasHearingLoop = parseBooleanStringOrThrow(columns["hasHearingLoop"]!!, "hasHearingLoop"),
    status = PropertyStatus.valueOf(columns["status"]!!),
    apCode = columns["apCode"]!!,
    qCode = columns["qCode"]!!,
    latitude = columns["latitude"]!!.toDoubleOrNull(),
    longitude = columns["longitude"]!!.toDoubleOrNull(),
    gender = ApprovedPremisesGender.valueOf(columns["gender"]!!),
    supportsSpaceBookings = parseBooleanStringOrThrow(columns["supportsSpaceBookings"]!!, "supportsSpaceBookings"),
    managerDetails = columns["managerDetails"]!!,
  )

  @SuppressWarnings("TooGenericExceptionThrown")
  override fun processRow(row: ApprovedPremisesSeedCsvRow) {
    val existingPremises = premisesRepository.findByApCode(row.apCode)

    val probationRegion = probationRegionRepository.findByName(row.probationRegion)
      ?: throw RuntimeException("Probation Region ${row.probationRegion} does not exist")

    val localAuthorityArea = localAuthorityAreaRepository.findByName(row.localAuthorityArea)
      ?: throw RuntimeException("Local Authority Area ${row.localAuthorityArea} does not exist")

    val characteristics = characteristicsFromRow(row)

    if (existingPremises != null) {
      updateExistingApprovedPremises(row, existingPremises, probationRegion, localAuthorityArea, characteristics)
    } else {
      createNewApprovedPremises(row, probationRegion, localAuthorityArea, characteristics)
    }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun characteristicsFromRow(row: ApprovedPremisesSeedCsvRow): List<CharacteristicEntity> {
    return listOf(
      CharacteristicValue("isIAP", castBooleanString(row.isIAP)),
      CharacteristicValue("isPIPE", castBooleanString(row.isPIPE)),
      CharacteristicValue("isESAP", castBooleanString(row.isESAP)),
      CharacteristicValue("isSemiSpecialistMentalHealth", castBooleanString(row.isSemiSpecialistMentalHealth)),
      CharacteristicValue("isRecoveryFocussed", castBooleanString(row.isRecoveryFocussed)),
      CharacteristicValue("isSuitableForVulnerable", castBooleanString(row.isSuitableForVulnerable)),
      CharacteristicValue("acceptsSexOffenders", castBooleanString(row.acceptsSexOffenders)),
      CharacteristicValue("acceptsChildSexOffenders", castBooleanString(row.acceptsChildSexOffenders)),
      CharacteristicValue("acceptsNonSexualChildOffenders", castBooleanString(row.acceptsNonSexualChildOffenders)),
      CharacteristicValue("acceptsHateCrimeOffenders", castBooleanString(row.acceptsHateCrimeOffenders)),
      CharacteristicValue("isCatered", castBooleanString(row.isCatered)),
      CharacteristicValue("hasWideStepFreeAccess", castBooleanString(row.hasWideStepFreeAccess)),
      CharacteristicValue("hasWideAccessToCommunalAreas", castBooleanString(row.hasWideAccessToCommunalAreas)),
      CharacteristicValue("hasStepFreeAccessToCommunalAreas", castBooleanString(row.hasStepFreeAccessToCommunalAreas)),
      CharacteristicValue("hasWheelChairAccessibleBathrooms", castBooleanString(row.hasWheelChairAccessibleBathrooms)),
      CharacteristicValue("hasLift", castBooleanString(row.hasLift)),
      CharacteristicValue("hasTactileFlooring", castBooleanString(row.hasTactileFlooring)),
      CharacteristicValue("hasBrailleSignage", castBooleanString(row.hasBrailleSignage)),
      CharacteristicValue("hasHearingLoop", castBooleanString(row.hasHearingLoop)),
    ).filter { it.value }
      .map {
        characteristicRepository.findByPropertyNameAndScopes(propertyName = it.propertyName, serviceName = "approved-premises", modelName = "premises")
          ?: throw RuntimeException("Characteristic '${it.propertyName}' does not exist for AP premises")
      }
  }

  private fun createNewApprovedPremises(
    row: ApprovedPremisesSeedCsvRow,
    probationRegion: ProbationRegionEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    characteristics: List<CharacteristicEntity>,
  ) {
    log.info("Creating new Approved Premises: ${row.apCode} ${row.name}")

    val approvedPremises = premisesRepository.save(
      ApprovedPremisesEntity(
        id = UUID.randomUUID(),
        name = row.name,
        addressLine1 = row.addressLine1,
        addressLine2 = row.addressLine2,
        town = row.town,
        postcode = row.postcode,
        notes = row.notes,
        emailAddress = row.emailAddress,
        probationRegion = probationRegion,
        localAuthorityArea = localAuthorityArea,
        bookings = mutableListOf(),
        lostBeds = mutableListOf(),
        apCode = row.apCode,
        qCode = row.qCode,
        rooms = mutableListOf(),
        characteristics = mutableListOf(),
        status = row.status,
        longitude = row.longitude,
        latitude = row.latitude,
        point = if (row.longitude != null && row.latitude != null) geometryFactory.createPoint(Coordinate(row.latitude, row.longitude)) else null,
        gender = row.gender,
        supportsSpaceBookings = castBooleanString(row.supportsSpaceBookings),
        managerDetails = row.managerDetails,
      ),
    )

    approvedPremises.characteristics.addAll(characteristics)

    premisesRepository.save(approvedPremises)
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun parseBooleanStringOrThrow(value: String, fieldName: String): String {
    val booleanString = listOf("YES", "NO").find { it == value.trim().uppercase() }
      ?: throw RuntimeException("'$value' is not a recognised boolean for '$fieldName' (use yes | no)")

    return if (booleanString == "YES") "YES" else "NO"
  }

  private fun castBooleanString(booleanString: String): Boolean {
    return booleanString == "YES"
  }

  private fun updateExistingApprovedPremises(
    row: ApprovedPremisesSeedCsvRow,
    existingApprovedPremises: ApprovedPremisesEntity,
    probationRegion: ProbationRegionEntity,
    localAuthorityArea: LocalAuthorityAreaEntity,
    characteristics: List<CharacteristicEntity>,
  ) {
    log.info("Updating existing Approved Premises: ${row.apCode} ${row.name}")

    existingApprovedPremises.apply {
      this.name = row.name
      this.apCode = row.apCode
      this.qCode = row.qCode
      this.addressLine1 = row.addressLine1
      this.addressLine2 = row.addressLine2
      this.town = row.town
      this.postcode = row.postcode
      this.longitude = row.longitude
      this.latitude = row.latitude
      this.notes = row.notes
      this.emailAddress = row.emailAddress
      this.probationRegion = probationRegion
      this.localAuthorityArea = localAuthorityArea
      this.status = row.status
      this.point = if (row.longitude != null && row.latitude != null) geometryFactory.createPoint(Coordinate(row.latitude, row.longitude)) else null
      this.gender = row.gender
      this.supportsSpaceBookings = castBooleanString(row.supportsSpaceBookings)
      this.managerDetails = row.managerDetails
    }

    characteristics.forEach {
      if (existingApprovedPremises.characteristics.none { existingCharacteristic -> existingCharacteristic.id == it.id }) {
        existingApprovedPremises.characteristics.add(it)
      }
    }

    premisesRepository.save(existingApprovedPremises)
  }
}

data class CharacteristicValue(
  val propertyName: String,
  val value: Boolean,
)

data class ApprovedPremisesSeedCsvRow(
  val name: String,
  val addressLine1: String,
  val addressLine2: String?,
  val town: String,
  val postcode: String,
  val notes: String,
  val emailAddress: String,
  val probationRegion: String,
  val localAuthorityArea: String,
  val isIAP: String,
  val isPIPE: String,
  val isESAP: String,
  val isSemiSpecialistMentalHealth: String,
  val isRecoveryFocussed: String,
  val isSuitableForVulnerable: String,
  val acceptsSexOffenders: String,
  val acceptsChildSexOffenders: String,
  val acceptsNonSexualChildOffenders: String,
  val acceptsHateCrimeOffenders: String,
  val isCatered: String,
  val hasWideStepFreeAccess: String,
  val hasWideAccessToCommunalAreas: String,
  val hasStepFreeAccessToCommunalAreas: String,
  val hasWheelChairAccessibleBathrooms: String,
  val hasLift: String,
  val hasTactileFlooring: String,
  val hasBrailleSignage: String,
  val hasHearingLoop: String,
  val status: PropertyStatus,
  val apCode: String,
  val qCode: String,
  val latitude: Double?,
  val longitude: Double?,
  val gender: ApprovedPremisesGender,
  val supportsSpaceBookings: String,
  val managerDetails: String,
)
