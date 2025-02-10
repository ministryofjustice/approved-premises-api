package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas3

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.transitionalAccommodationServicesCAS3.base-path:/cas3}")
class TimelineCas3Controller(
        delegate: TimelineCas3Delegate?
) : TimelineCas3 {
    private lateinit var delegate: TimelineCas3Delegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : TimelineCas3Delegate {})
    }

    override fun getDelegate(): TimelineCas3Delegate = delegate
}
