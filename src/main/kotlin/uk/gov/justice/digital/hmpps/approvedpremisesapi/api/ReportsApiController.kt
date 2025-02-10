package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class ReportsApiController(
        delegate: ReportsApiDelegate?
) : ReportsApi {
    private lateinit var delegate: ReportsApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : ReportsApiDelegate {})
    }

    override fun getDelegate(): ReportsApiDelegate = delegate
}
