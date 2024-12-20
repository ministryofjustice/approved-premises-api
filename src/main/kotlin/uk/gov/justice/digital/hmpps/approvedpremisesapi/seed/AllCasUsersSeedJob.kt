package uk.gov.justice.digital.hmpps.approvedpremisesapi.seed

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model.ServiceName
import uk.gov.justice.digital.hmpps.approvedpremisesapi.service.UserService

@Component
class AllCasUsersSeedJob(userService: UserService) : AbstractUsersSeedJob(ServiceName.entries, userService)
