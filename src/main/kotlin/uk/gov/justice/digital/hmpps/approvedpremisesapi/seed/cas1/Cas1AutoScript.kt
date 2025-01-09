package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.cas1

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualification
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRole
import uk.gov.justice.digital.hmpps.approvedpremisesapi.seed.SeedLogger
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.EnvironmentService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService.GetUserResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.extractEntityFromCasResult
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@SuppressWarnings("MagicNumber", "MaxLineLength", "TooGenericExceptionCaught")
@Component
class Cas1AutoScript(
  private val seedLogger: SeedLogger,
  private val userService: UserService,
  private val cruManagementAreaRepository: Cas1CruManagementAreaRepository,
  private val environmentService: EnvironmentService,
  private val cas1ApplicationSeedService: Cas1ApplicationSeedService,
) {

  @Transactional
  fun script() {
    seedLogger.info("Auto-Scripting for CAS1")

    if (environmentService.isLocal()) {
      scriptLocal()
    } else if (environmentService.isDev()) {
      scriptDev()
    }
  }

  fun scriptLocal() {
    seedLogger.info("Auto-Scripting for CAS1 local")
    seedUsers(usersToSeedLocal())

    cas1ApplicationSeedService.createApplication(deliusUserName = "JIMSNOWLDAP", crn = "X320741")
    cas1ApplicationSeedService.createApplication(deliusUserName = "LAOFULLACCESS", crn = "X400000")
    cas1ApplicationSeedService.createApplication(deliusUserName = "LAOFULLACCESS", crn = "X400001")
    cas1ApplicationSeedService.createOfflineApplicationWithBooking(deliusUserName = "JIMSNOWLDAP", crn = "X320741")
  }

  fun scriptDev() {
    seedLogger.info("Auto-Scripting for CAS1 dev")
    seedUsers(usersToSeedDev())
  }

  private fun seedUsers(usersToSeed: List<SeedUser>) = usersToSeed.forEach { seedUser(it) }

  @SuppressWarnings("TooGenericExceptionCaught")
  private fun seedUser(seedUser: SeedUser) {
    seedLogger.info("Auto-scripting user ${seedUser.username}")
    try {
      val getUserResponse = userService.getExistingUserOrCreate(username = seedUser.username)

      when (getUserResponse) {
        GetUserResponse.StaffRecordNotFound -> seedLogger.error("Seeding user with ${seedUser.username} failed as staff record not found")
        is GetUserResponse.Success -> {
          val user = getUserResponse.user
          seedUser.roles.forEach { role ->
            userService.addRoleToUser(user = user, role = role)
          }
          seedUser.qualifications.forEach { qualification ->
            userService.addQualificationToUser(user, qualification)
          }
          val roles = user.roles.map { it.role }.joinToString(", ")
          seedLogger.info("  -> User '${user.name}' (${user.deliusUsername}) seeded with roles $roles")

          if (seedUser.cruManagementAreaOverrideId != null) {
            user.cruManagementAreaOverride = cruManagementAreaRepository.findByIdOrNull(seedUser.cruManagementAreaOverrideId)
          }
        }
      }
    } catch (e: Exception) {
      seedLogger.error("Seeding user with ${seedUser.username} failed", e)
    }
  }

  private fun usersToSeedLocal(): List<SeedUser> {
    return listOf(
      SeedUser(
        username = "JIMSNOWLDAP",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_CRU_MEMBER,
        ),
        qualifications = UserQualification.entries.toList(),
        documentation = "For local use in development and testing",
      ),
      SeedUser(
        username = "LAOFULLACCESS",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_FUTURE_MANAGER,
          UserRole.CAS1_CRU_MEMBER,
        ),
        qualifications = emptyList(),
        documentation = "For local use in development and testing. This user has an exclusion (whitelisted) for LAO CRN X400000",
      ),
      SeedUser(
        username = "LAORESTRICTED",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_FUTURE_MANAGER,
          UserRole.CAS3_ASSESSOR,
        ),
        qualifications = emptyList(),
        documentation = "For local use in development and testing. This user has a restriction (blacklisted) for LAO CRN X400001",
      ),
      SeedUser(
        username = "CRUWOMENSESTATE",
        roles = listOf(
          UserRole.CAS1_CRU_MEMBER,
          UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
          UserRole.CAS1_ASSESSOR,
          UserRole.CAS1_MATCHER,
          UserRole.CAS1_WORKFLOW_MANAGER,
          UserRole.CAS1_REPORT_VIEWER,
          UserRole.CAS1_APPEALS_MANAGER,
          UserRole.CAS1_FUTURE_MANAGER,
        ),
        qualifications = UserQualification.entries.toList(),
        cruManagementAreaOverrideId = Cas1CruManagementAreaEntity.WOMENS_ESTATE_ID,
        documentation = "For local use in development and testing. This user's CRU Management Area is overridden to women's estate",
      ),
    )
  }

  private fun usersToSeedDev(): List<SeedUser> =
    listOf("AP_USER_TEST_1", "AP_USER_TEST_3", "AP_USER_TEST_4", "AP_USER_TEST_5")
      .map {
        SeedUser(
          username = it,
          roles = listOf(
            UserRole.CAS1_CRU_MEMBER,
            UserRole.CAS1_CRU_MEMBER_FIND_AND_BOOK_BETA,
            UserRole.CAS1_ASSESSOR,
            UserRole.CAS1_MATCHER,
            UserRole.CAS1_WORKFLOW_MANAGER,
            UserRole.CAS1_REPORT_VIEWER,
            UserRole.CAS1_APPEALS_MANAGER,
            UserRole.CAS1_CRU_MEMBER,
            UserRole.CAS1_FUTURE_MANAGER,
          ),
          qualifications = UserQualification.entries.toList(),
          documentation = "Generic E2E test user",
        )
      } +
      listOf(
        SeedUser(
          username = "AP_USER_TEST_2",
          roles = listOf(
            UserRole.CAS1_USER_MANAGER,
            UserRole.CAS1_REPORT_VIEWER,
          ),
          qualifications = emptyList(),
          documentation = "Admin and Reports Test User",
        ),
      )
}

data class SeedUser(
  val username: String,
  val roles: List<UserRole>,
  val qualifications: List<UserQualification>,
  val documentation: String,
  val cruManagementAreaOverrideId: UUID? = null,
)
