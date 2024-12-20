package uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.cas1

import jakarta.persistence.EntityManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.migration.MigrationJob
import java.util.UUID

/**
 * This job creates links between placement_applications and their corresponding placement_requests
 *
 * As of November 2023 this link was populated when a placement_applications was accepted, so this backfill
 * should only apply to placement_applications accepted before November 2023
 *
 * We establish the link by finding pairs of placement_requests and placement_applications on an application
 * that hav the same arrival date and duration
 *
 * Whilst this approach should 'fix' the link for most placement_applications, if a placement_application
 * has already been withdrawn there is no way to determine if it was ACCEPTED before the withdrawal occurred so these
 * can't be considered. This is because when withdrawing the placement_application.decision value is updated to
 * WITHDRAWN/WITHDRAWN_BY_PP overwriting 'ACCEPTED' (if set). This will result in an error being logged for the particular
 * application
 */
@Component
class Cas1FixPlacementApplicationLinksJob(
  private val placementApplicationRepository: PlacementApplicationRepository,
  private val applicationRepository: ApplicationRepository,
  private val placementRequestRepository: PlacementRequestRepository,
  private val entityManager: EntityManager,
  private val transactionTemplate: TransactionTemplate,
) : MigrationJob() {
  override val shouldRunInTransaction = false
  var log: Logger = LoggerFactory.getLogger(this::class.java)

  override fun process(pageSize: Int) {
    applyManualFixes()
    applyAutomatedFixes()

    entityManager.clear()
  }

  private fun applyManualFixes() {
    log.info("Applying manual link fixes for ${manualLinkFixes.size} fixes")

    manualLinkFixes.forEach { fix ->
      try {
        transactionTemplate.executeWithoutResult {
          applyManualFix(fix)
        }
      } catch (e: IllegalStateException) {
        log.error(e.message)
      }
    }
  }

  private fun applyAutomatedFixes() {
    val applicationIds = placementApplicationRepository.findApplicationsThatHaveAnAcceptedPlacementApplicationWithoutACorrespondingPlacementRequest()

    log.info("Automatically Fixing PlacementApplication to PlacementRequest relationships for ${applicationIds.size} applications")

    applicationIds.forEach { applicationId ->
      try {
        transactionTemplate.executeWithoutResult {
          applyAutomatedFixes(UUID.fromString(applicationId))
        }
      } catch (e: IllegalStateException) {
        log.error(e.message)
      }
    }
  }

  @SuppressWarnings("ReturnCount")
  fun applyManualFix(manualLinkFix: ManualLinkFix) {
    val application = applicationRepository.findByIdOrNull(manualLinkFix.applicationId)
    if (application == null) {
      log.error("Could not find application for id ${manualLinkFix.applicationId} when applying manual fix")
      return
    }

    log.info("Applying manual fix to application ${application.id} (application created ${application.createdAt})")

    val approvedPremisesApplicationEntity = application as ApprovedPremisesApplicationEntity
    val placementRequest = approvedPremisesApplicationEntity
      .placementRequests
      .firstOrNull { it.id == manualLinkFix.placementRequestId }

    if (placementRequest == null) {
      log.error("Could not find placement request for id ${manualLinkFix.placementRequestId} when applying manual fix")
      return
    }

    val placementApplication = placementApplicationRepository.findByIdOrNull(manualLinkFix.placementApplicationId)

    if (placementApplication == null) {
      log.error("Could not find placement application for id ${manualLinkFix.placementApplicationId} when applying manual fix")
      return
    }

    if (placementApplication.application.id != application.id) {
      log.error("Placement application ${manualLinkFix.placementApplicationId} does not apply to application ${manualLinkFix.applicationId}")
      return
    }

    linkPlacementAppToRequest(
      placementApplication = placementApplication,
      placementRequest = placementRequest,
    )
  }

  @SuppressWarnings("ReturnCount")
  fun applyAutomatedFixes(applicationId: UUID) {
    val application = applicationRepository.findByIdOrNull(applicationId) as ApprovedPremisesApplicationEntity

    log.info(
      "Fixing PlacementApplication to PlacementRequest relationships for Application $applicationId " +
        "(application created ${application.createdAt})",
    )

    val unlinkedPlacementRequests = application.placementRequests
      .filter { !it.isReallocated() }
      .filter { it.placementApplication == null }
      .toMutableList()

    val placementAppsAndDate = placementApplicationRepository.findByApplication(application)
      .filter { it.placementRequests.isEmpty() }
      .filter { it.isAccepted() }
      .flatMap { placementApp ->
        placementApp.placementDates.map {
          PlacementAppAndDate(
            placementApplication = placementApp,
            date = it,
            placementRequest = null,
          )
        }
      }

    val placementAppsWithDecisionMadeDateSet = placementAppsAndDate
      // we only started capturing decisionMadeAt in Dec 2023
      .filter { it.placementApplication.decisionMadeAt != null }

    if (placementAppsWithDecisionMadeDateSet.size == placementAppsAndDate.size) {
      log.error(
        "We should not be considering applications where all PlacementApplications have a non-null decisionMadeAt, because this was " +
          "only set for decisions made after linking PlacementApplications to PlacementRequests was automatically " +
          "managed in code. This suggests an error in the migration logic. " +
          "Placement applications are ${placementAppsWithDecisionMadeDateSet.map { describe(it) }}",
      )
      return
    }

    log.info("We have ${placementAppsAndDate.size} PlacementApplications to match for application $applicationId")

    placementAppsAndDate.forEach { placementAppAndDate ->
      val matchingPlacementRequests = unlinkedPlacementRequests.filter { placementRequest ->
        placementRequest.expectedArrival == placementAppAndDate.date.expectedArrival &&
          placementRequest.duration == placementAppAndDate.date.duration
      }

      if (matchingPlacementRequests.isEmpty()) {
        log.error("No placement request found for placement app ${describe(placementAppAndDate)} for application $applicationId")
        return
      } else if (matchingPlacementRequests.size > 1) {
        log.error("More than one potential placement request found for placement app ${describe(placementAppAndDate)} for application $applicationId")
        return
      } else {
        val placementRequest = matchingPlacementRequests.first()
        placementAppAndDate.placementRequest = placementRequest
        unlinkedPlacementRequests.remove(placementRequest)
      }
    }

    val applicationArrivalDate = application.arrivalDate?.toLocalDate()
    if (applicationArrivalDate != null) {
      if (unlinkedPlacementRequests.size != 1) {
        log.error(
          "Application ${application.id} has an arrival date set on the initial application," +
            " but after matching to placement apps, no placement requests remain that represents this date",
        )
        return
      } else {
        val remainingPlacementRequestDate = unlinkedPlacementRequests[0].expectedArrival
        if (applicationArrivalDate != remainingPlacementRequestDate) {
          log.error(
            "Application ${application.id} has an arrival date set on the initial application, but after matching to placement apps," +
              " the only remaining placement request doesn't have the expected arrival date " +
              "(expected $applicationArrivalDate was $remainingPlacementRequestDate)",
          )
          return
        }
      }
    } else if (unlinkedPlacementRequests.isNotEmpty()) {
      log.error("Application ${application.id} does not have an arrival date set on the initial application, yet there are unmatched placement requests.")
      return
    }

    placementAppsAndDate.forEach { placementAppAndDate ->
      linkPlacementAppToRequest(
        placementApplication = placementAppAndDate.placementApplication,
        placementRequest = placementAppAndDate.placementRequest!!,
      )
    }
  }

  private fun linkPlacementAppToRequest(
    placementApplication: PlacementApplicationEntity,
    placementRequest: PlacementRequestEntity,
  ) {
    placementRequest.placementApplication = placementApplication
    log.info("Linked PlacementRequest ${placementRequest.id} to PlacementApplication ${placementApplication.id}")
    placementRequestRepository.save(placementRequest)
  }

  private fun describe(placementApp: PlacementAppAndDate) =
    "${placementApp.placementApplication.id} for date ${placementApp.date.expectedArrival} and duration ${placementApp.date.duration}"

  data class PlacementAppAndDate(
    val placementApplication: PlacementApplicationEntity,
    val date: PlacementDateEntity,
    var placementRequest: PlacementRequestEntity?,
  )

  data class ManualLinkFix(
    val applicationId: UUID,
    val placementRequestId: UUID,
    val placementApplicationId: UUID,
  )

  // Some links could not be fixed automatically by this migration job. After manual (human) analysis
  // several of these relationships should be determined.
  // See https://dsdmoj.atlassian.net/wiki/spaces/AP/pages/4781965318/CAS-1+PlacementApplication+to+PlacementRequest+link+Backfill
  val manualLinkFixes = listOf(
    ManualLinkFix(
      applicationId = UUID.fromString("fcba419e-7802-4c05-947f-7b24988aa795"),
      placementRequestId = UUID.fromString("af242c35-07ff-4545-839a-b4b89e735e63"),
      placementApplicationId = UUID.fromString("09b26c6d-570d-4b4f-804c-72ac71325cc0"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("75337979-e4c9-45f9-b829-948d8516f364"),
      placementRequestId = UUID.fromString("c1131478-2b3c-4dd8-84c4-88698184d632"),
      placementApplicationId = UUID.fromString("218b7567-460b-447c-9481-7315ba53c23e"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("9c4597e3-a615-4f08-b697-39b5cb2fc152"),
      placementRequestId = UUID.fromString("44f15784-3497-4df5-9035-f65b741685c9"),
      placementApplicationId = UUID.fromString("2ef871f6-a4da-46a9-96f7-a2901f80aa40"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("fe998691-61c6-489a-b94a-f62f112acd43"),
      placementRequestId = UUID.fromString("d70a8012-bc3d-43ad-93f2-d0b5724dc3bc"),
      placementApplicationId = UUID.fromString("4af4831b-a930-4de7-b612-1999b3e0313f"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("4434ef73-abc1-451d-8c3e-eb9d43cd10ae"),
      placementRequestId = UUID.fromString("d2478632-1616-467d-8b6b-f11bfe03729b"),
      placementApplicationId = UUID.fromString("532f7117-f5f9-498e-92ee-eb961de98af0"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("4434ef73-abc1-451d-8c3e-eb9d43cd10ae"),
      placementRequestId = UUID.fromString("5bccc0d4-f541-438a-a803-152e1d3ebd54"),
      placementApplicationId = UUID.fromString("8848e951-b51d-43f3-a205-d7404a732d5f"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("95b2aaa5-9314-49de-b272-b9e0bb8ee643"),
      placementRequestId = UUID.fromString("bfdee07a-f0f8-428d-9d34-28455fc10ec7"),
      placementApplicationId = UUID.fromString("81d60dd8-df26-4820-9b8c-828b2bdbaaa4"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("9fff5bbd-c538-446e-a3c8-5c770e05bdb6"),
      placementRequestId = UUID.fromString("46765be3-b46b-4202-9cc1-118cb6d73928"),
      placementApplicationId = UUID.fromString("8cf4c9de-0e29-4af7-8621-437e6f7bc644"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ce51f404-3f8e-400c-b235-89e2ce5461d7"),
      placementRequestId = UUID.fromString("a8e380ee-10ce-48ba-98fa-9c115d722649"),
      placementApplicationId = UUID.fromString("9ef0080c-f83f-4f81-8219-4a7c61a70388"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("e3a87d85-fa54-4654-b804-4d0fb9253121"),
      placementRequestId = UUID.fromString("f681fa85-69df-46b6-bd05-cf4191c1d6d6"),
      placementApplicationId = UUID.fromString("af74d224-04d5-4598-a9a9-6e086ec2b60b"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("e3a87d85-fa54-4654-b804-4d0fb9253121"),
      placementRequestId = UUID.fromString("7a4f4108-9874-474a-8c70-528986cb3368"),
      placementApplicationId = UUID.fromString("6ebea4ff-34e7-4c64-ac20-b8b218c0f706"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ab02981b-a7f3-4075-8cff-27e5edad597d"),
      placementRequestId = UUID.fromString("55b0e01f-4006-4c5b-b61e-dfe1d47a27d3"),
      placementApplicationId = UUID.fromString("c0886c1c-11ac-4ce7-93a8-ce180e61bf36"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("cd508e65-6f2b-4bb1-8aa6-6ae3fee57c74"),
      placementRequestId = UUID.fromString("1aab2f12-d71b-47cc-af74-4e3a72d4af04"),
      placementApplicationId = UUID.fromString("c45764f2-98d6-4af5-a784-2faa16e6b2c9"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("cd508e65-6f2b-4bb1-8aa6-6ae3fee57c74"),
      placementRequestId = UUID.fromString("e7a98cc9-8a71-4c9d-b5f5-470e321bf24b"),
      placementApplicationId = UUID.fromString("2d6e7e0d-7202-47d9-847c-f50614018d9b"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("cd508e65-6f2b-4bb1-8aa6-6ae3fee57c74"),
      placementRequestId = UUID.fromString("18a532f7-7893-47a1-ab8e-1e34d7b3ff46"),
      placementApplicationId = UUID.fromString("ec618a23-2ad3-4df0-bb91-a81cdaaab677"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("07dc0d5f-0d95-4c72-aeb1-165ad3a67f44"),
      placementRequestId = UUID.fromString("2b15e7aa-323a-4626-9e1e-e4a30e82b130"),
      placementApplicationId = UUID.fromString("674d616e-5a9e-4ad6-92a4-4a1e30644146"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("07dc0d5f-0d95-4c72-aeb1-165ad3a67f44"),
      placementRequestId = UUID.fromString("7ecd4887-d0b0-4451-af38-8ee2cc07ed26"),
      placementApplicationId = UUID.fromString("270071ee-6ba9-4de7-a7e1-389bad72a377"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("07dc0d5f-0d95-4c72-aeb1-165ad3a67f44"),
      placementRequestId = UUID.fromString("64a45fbf-976d-4736-a8bd-370a058f9938"),
      placementApplicationId = UUID.fromString("3b6b444f-0254-4aea-98de-46d52834b9d3"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("1102c7bb-6824-4aa5-8e29-9196e4c8f9c3"),
      placementRequestId = UUID.fromString("ce833d80-44f7-439d-996f-f1ce41010871"),
      placementApplicationId = UUID.fromString("0b3376d4-1321-41fa-9b6b-dbf3884c34d2"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("1102c7bb-6824-4aa5-8e29-9196e4c8f9c3"),
      placementRequestId = UUID.fromString("737013c3-236d-4682-9510-e8b7928d339e"),
      placementApplicationId = UUID.fromString("bb518bec-cdfd-4721-8070-8130e4110558"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("4d4691a1-9ce0-4dfc-86c2-954dcdc0179b"),
      placementRequestId = UUID.fromString("e95d22f8-663b-427c-a00c-1e49bcfa51dd"),
      placementApplicationId = UUID.fromString("c8abc860-9222-40e8-ae76-720c3c182a07"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("911fd475-a175-4813-8d99-b5d3d13729af"),
      placementRequestId = UUID.fromString("9978e4a0-5a56-4b1b-bd2b-eb31e42e7692"),
      placementApplicationId = UUID.fromString("b1745a01-7d57-4788-80e3-996379c070fe"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("911fd475-a175-4813-8d99-b5d3d13729af"),
      placementRequestId = UUID.fromString("79a7eb05-f31d-4ab3-ab2d-63ab9901583d"),
      placementApplicationId = UUID.fromString("e77de0de-07c3-433a-9d01-5d84f9c887ca"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("911fd475-a175-4813-8d99-b5d3d13729af"),
      placementRequestId = UUID.fromString("285df48d-63da-4f69-85fe-a596f2740986"),
      placementApplicationId = UUID.fromString("7a0575f6-2de6-4c4f-b76f-d547c26f4d63"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("911fd475-a175-4813-8d99-b5d3d13729af"),
      placementRequestId = UUID.fromString("51e86a5e-9470-40d2-b08a-40d017f4eb54"),
      placementApplicationId = UUID.fromString("0da9e7a4-ede9-484d-8a12-8a235d5adc2c"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("911fd475-a175-4813-8d99-b5d3d13729af"),
      placementRequestId = UUID.fromString("4d9a7a8e-e0e6-448b-bc79-914647ce2941"),
      placementApplicationId = UUID.fromString("e8bb2d0a-7e3e-4c38-b483-21e3c97fd357"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("911fd475-a175-4813-8d99-b5d3d13729af"),
      placementRequestId = UUID.fromString("5bcccaa1-a4cf-4a32-bd4b-3a636df0c206"),
      placementApplicationId = UUID.fromString("e8bcd26b-4280-404f-8c06-ea0f5d0b47ce"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ba2c27b7-f06c-41b7-bee9-ea3b4e3c786e"),
      placementRequestId = UUID.fromString("7204a118-1efc-4178-b806-adbc430082f0"),
      placementApplicationId = UUID.fromString("1e5abf01-d59d-4092-b5d6-89dc52a72779"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ba2c27b7-f06c-41b7-bee9-ea3b4e3c786e"),
      placementRequestId = UUID.fromString("29cf583d-2f1a-40a1-b8f0-9bd400631302"),
      placementApplicationId = UUID.fromString("98fd1526-13f8-435f-b43c-5a605e7bd731"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ba2c27b7-f06c-41b7-bee9-ea3b4e3c786e"),
      placementRequestId = UUID.fromString("948e20dd-2273-4856-840d-f121722eba80"),
      placementApplicationId = UUID.fromString("1077733e-6006-48b8-8e6d-0e0af420ecf0"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ba2c27b7-f06c-41b7-bee9-ea3b4e3c786e"),
      placementRequestId = UUID.fromString("50b559c6-89d5-4920-b765-bca0e7c796f4"),
      placementApplicationId = UUID.fromString("46ef9724-f1a1-443d-922c-4e298a9e02b8"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("ba2c27b7-f06c-41b7-bee9-ea3b4e3c786e"),
      placementRequestId = UUID.fromString("07a4136a-b0ef-4f07-a53b-691c9aa796a1"),
      placementApplicationId = UUID.fromString("d41a1b42-c5bb-46d0-be15-0ba0fb6d6e4f"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("efee904d-3c3f-42a7-85f4-cb04a1bcbf6e"),
      placementRequestId = UUID.fromString("bcf62f1d-c887-44ec-9701-482dac0c7ede"),
      placementApplicationId = UUID.fromString("87e4d3a6-8561-4a0d-85ac-cfe8c3f1e750"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("efee904d-3c3f-42a7-85f4-cb04a1bcbf6e"),
      placementRequestId = UUID.fromString("573f1fb4-7d3f-4502-befc-54933302d939"),
      placementApplicationId = UUID.fromString("e9cd5e54-7099-490d-b29e-e3446b3dbb6a"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("efee904d-3c3f-42a7-85f4-cb04a1bcbf6e"),
      placementRequestId = UUID.fromString("048ac187-252d-4ac0-bfd2-058dd7609d52"),
      placementApplicationId = UUID.fromString("55965b77-3745-4a3b-b6f2-156aaade3f41"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("efee904d-3c3f-42a7-85f4-cb04a1bcbf6e"),
      placementRequestId = UUID.fromString("d5e28f80-3979-4dda-8326-b5c2109a436d"),
      placementApplicationId = UUID.fromString("9ae1f9eb-7e00-4c8e-97c3-b71b8abcde16"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("efee904d-3c3f-42a7-85f4-cb04a1bcbf6e"),
      placementRequestId = UUID.fromString("71bd6253-213a-4387-844e-6f10a50ab5d2"),
      placementApplicationId = UUID.fromString("f0c634b1-b798-48b5-a6f8-aeaec7ab9f5d"),
    ),
    ManualLinkFix(
      applicationId = UUID.fromString("efee904d-3c3f-42a7-85f4-cb04a1bcbf6e"),
      placementRequestId = UUID.fromString("558e245a-e59c-42f7-8e9f-c3cc1bd3eeac"),
      placementApplicationId = UUID.fromString("fe4d276a-3fcf-4729-8816-714d4cc56e75"),
    ),
  )
}
