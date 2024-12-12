package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.7.0")
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class UsersCas1Controller(
        @org.springframework.beans.factory.annotation.Autowired(required = false) delegate: UsersCas1Delegate?
) : UsersCas1 {
    private lateinit var delegate: UsersCas1Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : UsersCas1Delegate {})
    }

    override fun getDelegate(): UsersCas1Delegate = delegate
}
