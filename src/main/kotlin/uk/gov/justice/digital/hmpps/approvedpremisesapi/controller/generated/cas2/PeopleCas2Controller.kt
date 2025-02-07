package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}")
class PeopleCas2Controller(
        delegate: uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas2.PeopleCas2Delegate?
) : uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas2.PeopleCas2 {
    private lateinit var delegate: uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas2.PeopleCas2Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object :
            uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas2.PeopleCas2Delegate {})
    }

    override fun getDelegate(): uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas2.PeopleCas2Delegate = delegate
}
