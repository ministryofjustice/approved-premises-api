package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.cAS3DomainEvents.base-path:}")
class CAS3EventsApiController(
        delegate: CAS3EventsApiDelegate?
) : CAS3EventsApi {
    private lateinit var delegate: CAS3EventsApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : CAS3EventsApiDelegate {})
    }

    override fun getDelegate(): CAS3EventsApiDelegate = delegate
}
