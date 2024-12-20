package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import jakarta.persistence.EntityManager
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.ListCompareAlgorithm
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.locationtech.jts.geom.Point
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.PropertyStatus
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesGender
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostcodeDistrictRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.ExcelSeedJob
import java.util.UUID

@Component
class Cas1SeedPremisesFromSiteSurveyXlsxJob(
  private val premisesRepository: ApprovedPremisesRepository,
  private val probationRegionRepository: ProbationRegionRepository,
  private val localAuthorityAreaRepository: LocalAuthorityAreaRepository,
  private val characteristicRepository: CharacteristicRepository,
  private val postcodeDistrictRepository: PostcodeDistrictRepository,
  private val entityManager: EntityManager,
) : ExcelSeedJob {

  companion object {
    val javers: Javers = JaversBuilder.javers().withListCompareAlgorithm(ListCompareAlgorithm.AS_SET).build()
  }

  private val log = LoggerFactory.getLogger(this::class.java)

  override fun processDataFrame(dataFrame: DataFrame<*>, premisesId: UUID) {
    val siteSurveyPremise = Cas1SiteSurveyPremiseFactory().load(dataFrame)
    val premiseInfo = resolvePremiseInfo(siteSurveyPremise)
    val existingPremises = premisesRepository.findByQCode(siteSurveyPremise.qCode)
    if (existingPremises == null) {
      createPremise(premiseInfo)
    } else {
      updatePremise(existingPremises, premiseInfo)
    }
  }

  private fun resolvePremiseInfo(siteSurveyPremise: Cas1SiteSurveyPremise): PremisesInfo {
    val postcodeDistrict = resolvePostcodeDistrict(siteSurveyPremise.postcode)

    return PremisesInfo(
      siteSurveyPremise = siteSurveyPremise,
      probationRegion = resolveProbationRegion(siteSurveyPremise),
      localAuthorityArea = resolveLocalAuthorityArea(siteSurveyPremise),
      characteristicEntities = resolveCharacteristics(siteSurveyPremise),
      latitude = postcodeDistrict.latitude,
      longitude = postcodeDistrict.longitude,
      point = postcodeDistrict.point,
    )
  }

  private fun resolvePostcodeDistrict(postcode: String): PostCodeDistrictEntity {
    val postcodeSplit = postcode.split(" ")
    if (postcodeSplit.size < 2) {
      error("Could not extract out code from postcode $postcode")
    }

    val outcode = postcodeSplit[0]
    val postcodeDistrict = postcodeDistrictRepository.findByOutcode(postcodeSplit[0])
      ?: error("Could not find postcode district for outcode $outcode")

    return postcodeDistrict
  }

  private fun resolveLocalAuthorityArea(siteSurveyPremise: Cas1SiteSurveyPremise): LocalAuthorityAreaEntity {
    val localAuthorityAreaName = when (siteSurveyPremise.localAuthorityArea) {
      "Bournemouth" -> "Bournemouth, Christchurch and Poole"
      "Bristol" -> "Bristol, City of"
      "Windsor & Maidenhead" -> "Windsor and Maidenhead"
      else -> siteSurveyPremise.localAuthorityArea
    }

    return localAuthorityAreaRepository.findByName(localAuthorityAreaName)
      ?: error("Local Authority Area '$localAuthorityAreaName' does not exist")
  }

  private fun resolveProbationRegion(siteSurveyPremise: Cas1SiteSurveyPremise): ProbationRegionEntity {
    val name = when (siteSurveyPremise.probationRegion) {
      SiteSurveyProbationRegion.LONDON -> "London"
      SiteSurveyProbationRegion.KENT_SURREY_SUSSEX -> "Kent, Surrey & Sussex"
      SiteSurveyProbationRegion.EAST_OF_ENGLAND -> "East of England"
      SiteSurveyProbationRegion.EAST_MIDLANDS -> "East Midlands"
      SiteSurveyProbationRegion.WEST_MIDLANDS -> "West Midlands"
      SiteSurveyProbationRegion.YORKS_AND_HUMBER -> "Yorkshire & The Humber"
      SiteSurveyProbationRegion.NORTH_EAST -> "North East"
      SiteSurveyProbationRegion.NORTH_WEST -> "North West"
      SiteSurveyProbationRegion.GREATER_MANCHESTER -> "Greater Manchester"
      SiteSurveyProbationRegion.SOUTH_WEST -> "South West"
      SiteSurveyProbationRegion.SOUTH_CENTRAL -> "South Central"
      SiteSurveyProbationRegion.WALES -> "Wales"
    }

    return probationRegionRepository.findByName(name)
      ?: error("Probation Region '$name' does not exist")
  }

  private data class PremisesInfo(
    val siteSurveyPremise: Cas1SiteSurveyPremise,
    val probationRegion: ProbationRegionEntity,
    val localAuthorityArea: LocalAuthorityAreaEntity,
    val characteristicEntities: List<CharacteristicEntity>,
    val longitude: Double,
    val latitude: Double,
    var point: Point,
  )

  private fun createPremise(
    premisesInfo: PremisesInfo,
  ) {
    val qCode = premisesInfo.siteSurveyPremise.qCode
    log.info("Creating new premise for qcode $qCode (${premisesInfo.siteSurveyPremise.name})")

    val siteSurvey = premisesInfo.siteSurveyPremise

    val approvedPremises = premisesRepository.save(
      ApprovedPremisesEntity(
        id = UUID.randomUUID(),
        name = siteSurvey.name,
        addressLine1 = siteSurvey.address,
        addressLine2 = null,
        town = siteSurvey.townCity,
        postcode = siteSurvey.postcode,
        notes = "",
        // A new row is required in site surveys to capture this
        emailAddress = null,
        probationRegion = premisesInfo.probationRegion,
        localAuthorityArea = premisesInfo.localAuthorityArea,
        bookings = mutableListOf(),
        lostBeds = mutableListOf(),
        // A new row is required in site surveys to capture this
        apCode = qCode,
        qCode = qCode,
        rooms = mutableListOf(),
        characteristics = premisesInfo.characteristicEntities.toMutableList(),
        status = PropertyStatus.active,
        longitude = premisesInfo.longitude,
        latitude = premisesInfo.latitude,
        point = premisesInfo.point,
        gender = siteSurvey.maleFemale.toApprovedPremisesGender(),
        supportsSpaceBookings = false,
        // A new row is required in site surveys to capture this
        managerDetails = null,
      ),
    )

    premisesRepository.save(approvedPremises)
  }

  private fun updatePremise(
    existingPremise: ApprovedPremisesEntity,
    premisesInfo: PremisesInfo,
  ) {
    val qCode = premisesInfo.siteSurveyPremise.qCode
    log.info("Updating premise for qcode $qCode (${premisesInfo.siteSurveyPremise.name})")

    val beforeChange = ApprovedPremisesForComparison.fromEntity(existingPremise)

    val siteSurvey = premisesInfo.siteSurveyPremise
    existingPremise.apply {
      name = siteSurvey.name
      addressLine1 = siteSurvey.address
      addressLine2 = null
      town = siteSurvey.townCity
      postcode = siteSurvey.postcode
      probationRegion = premisesInfo.probationRegion
      localAuthorityArea = premisesInfo.localAuthorityArea
      longitude = premisesInfo.longitude
      latitude = premisesInfo.latitude
      point = premisesInfo.point
      gender = siteSurvey.maleFemale.toApprovedPremisesGender()
    }

    existingPremise.characteristics.clear()
    existingPremise.characteristics.addAll(premisesInfo.characteristicEntities)

    val afterChange = ApprovedPremisesForComparison.fromEntity(existingPremise)

    val diff = javers.compare(beforeChange, afterChange)
    if (diff.hasChanges()) {
      log.info("Changes for import of ${siteSurvey.name} are ${diff.prettyPrint()}")
      premisesRepository.save(existingPremise)
    } else {
      entityManager.clear()
      log.info("No changes detected")
    }
  }

  private fun MaleFemale.toApprovedPremisesGender() = when (this) {
    MaleFemale.MALE -> ApprovedPremisesGender.MAN
    MaleFemale.FEMALE -> ApprovedPremisesGender.WOMAN
  }

  private data class CharacteristicRequired(
    val propertyName: String,
    val value: Boolean,
  )

  private data class ApprovedPremisesForComparison(
    val id: UUID,
    val name: String,
    val addressLine1: String,
    val addressLine2: String?,
    val town: String?,
    val postcode: String,
    val longitude: Double?,
    val latitude: Double?,
    val notes: String,
    val emailAddress: String?,
    val probationRegionName: String,
    val localAuthorityAreaName: String?,
    val apCode: String,
    val qCode: String,
    val characteristicNames: List<String>,
    val status: PropertyStatus,
    val point: Point?,
    val gender: ApprovedPremisesGender,
    val supportsSpaceBookings: Boolean,
    val managerDetails: String?,
  ) {
    companion object {
      fun fromEntity(entity: ApprovedPremisesEntity) = ApprovedPremisesForComparison(
        id = entity.id,
        name = entity.name,
        addressLine1 = entity.addressLine1,
        addressLine2 = entity.addressLine2,
        town = entity.town,
        postcode = entity.postcode,
        longitude = entity.longitude,
        latitude = entity.latitude,
        notes = entity.notes,
        emailAddress = entity.emailAddress,
        probationRegionName = entity.probationRegion.name,
        localAuthorityAreaName = entity.localAuthorityArea?.name,
        apCode = entity.apCode,
        qCode = entity.qCode,
        characteristicNames = entity.characteristics.map { it.name }.sorted(),
        status = entity.status,
        point = entity.point,
        gender = entity.gender,
        supportsSpaceBookings = entity.supportsSpaceBookings,
        managerDetails = entity.managerDetails,
      )
    }
  }

  @SuppressWarnings("TooGenericExceptionThrown")
  private fun resolveCharacteristics(siteSurveyPremise: Cas1SiteSurveyPremise): List<CharacteristicEntity> {
    return listOf(
      CharacteristicRequired("isIAP", siteSurveyPremise.iap),
      CharacteristicRequired("isPIPE", siteSurveyPremise.pipe),
      CharacteristicRequired("isESAP", siteSurveyPremise.enhancedSecuritySite),
      CharacteristicRequired("isSemiSpecialistMentalHealth", siteSurveyPremise.mentalHealth),
      CharacteristicRequired("isRecoveryFocussed", siteSurveyPremise.recoveryFocussed),
      CharacteristicRequired("isSuitableForVulnerable", siteSurveyPremise.suitableForPeopleAtRiskOfCriminalExploitation),
      CharacteristicRequired("acceptsSexOffenders", siteSurveyPremise.willAcceptPeopleWhoHave.committedSexualOffencesAgainstAdults),
      CharacteristicRequired("acceptsChildSexOffenders", siteSurveyPremise.willAcceptPeopleWhoHave.committedSexualOffencesAgainstChildren),
      CharacteristicRequired("acceptsNonSexualChildOffenders", siteSurveyPremise.willAcceptPeopleWhoHave.committedNonSexualOffencesAgainstChildren),
      CharacteristicRequired("acceptsHateCrimeOffenders", siteSurveyPremise.willAcceptPeopleWhoHave.beenConvictedOfHateCrimes),
      CharacteristicRequired("isCatered", siteSurveyPremise.cateredOrSelfCatered),
      CharacteristicRequired("hasWideStepFreeAccess", siteSurveyPremise.stepFreeEntrance),
      CharacteristicRequired("hasWideAccessToCommunalAreas", siteSurveyPremise.corridorsAtLeast1200CmWide),
      CharacteristicRequired("hasStepFreeAccessToCommunalAreas", siteSurveyPremise.corridorsHaveStepFreeAccess),
      CharacteristicRequired("hasWheelChairAccessibleBathrooms", siteSurveyPremise.bathroomFacilitiesAdaptedForWheelchairUsers),
      CharacteristicRequired("hasLift", siteSurveyPremise.hasALift),
      CharacteristicRequired("hasTactileFlooring", siteSurveyPremise.hasTactileAndDirectionalFlooring),
      CharacteristicRequired("hasBrailleSignage", siteSurveyPremise.hasSignsInBraille),
      CharacteristicRequired("hasHearingLoop", siteSurveyPremise.hasAHearingLoop),
    ).filter { it.value }
      .map {
        characteristicRepository.findByPropertyNameAndScopes(propertyName = it.propertyName, serviceName = "approved-premises", modelName = "premises")
          ?: throw RuntimeException("Characteristic '${it.propertyName}' does not exist for AP premises")
      }
  }
}
