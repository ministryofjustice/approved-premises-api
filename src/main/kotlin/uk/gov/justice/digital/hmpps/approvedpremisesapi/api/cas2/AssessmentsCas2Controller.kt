package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2.base-path:/cas2}")
class AssessmentsCas2Controller(
        delegate: AssessmentsCas2Delegate?
) : AssessmentsCas2 {
    private lateinit var delegate: AssessmentsCas2Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : AssessmentsCas2Delegate {})
    }

    override fun getDelegate(): AssessmentsCas2Delegate = delegate
}
