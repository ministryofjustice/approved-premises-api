package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas2v2

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesTier2CAS2Version2.base-path:/cas2v2}")
class AssessmentsCas2v2Controller(
        delegate: AssessmentsCas2v2Delegate?
) : AssessmentsCas2v2 {
    private lateinit var delegate: AssessmentsCas2v2Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : AssessmentsCas2v2Delegate {})
    }

    override fun getDelegate(): AssessmentsCas2v2Delegate = delegate
}
