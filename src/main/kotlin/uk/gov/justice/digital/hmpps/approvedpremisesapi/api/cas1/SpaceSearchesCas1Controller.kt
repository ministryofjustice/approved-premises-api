package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class SpaceSearchesCas1Controller(
        delegate: SpaceSearchesCas1Delegate?
) : SpaceSearchesCas1 {
    private lateinit var delegate: SpaceSearchesCas1Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : SpaceSearchesCas1Delegate {})
    }

    override fun getDelegate(): SpaceSearchesCas1Delegate = delegate
}
