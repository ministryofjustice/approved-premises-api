package uk.gov.justice.digital.hmpps.approvedpremisesapi.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.11.0")
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class UsersApiController(
        delegate: UsersApiDelegate?
) : UsersApi {
    private lateinit var delegate: UsersApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : UsersApiDelegate {})
    }

    override fun getDelegate(): UsersApiDelegate = delegate
}
