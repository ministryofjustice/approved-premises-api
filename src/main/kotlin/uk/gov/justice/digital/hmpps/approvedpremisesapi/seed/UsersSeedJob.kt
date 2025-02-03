package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

/**
 * Seeds users, along with their roles and qualifications.
 *
 * NB: this clears roles and qualifications for both CAS1 and CAS3
 *
 * If you want to seed users without touching the pre-existing roles/qualifications
 * of pre-existing users, then consider using [UsersBasicSeedJob].
 */
@Component
class UsersSeedJob(userService: UserService) : AbstractUsersSeedJob(ServiceName.entries, userService)
