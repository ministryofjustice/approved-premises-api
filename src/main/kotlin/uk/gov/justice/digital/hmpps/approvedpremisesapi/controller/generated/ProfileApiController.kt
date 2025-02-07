package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.11.0",
)
@Controller
@RequestMapping("\${openapi.approvedPremises.base-path:}")
class ProfileApiController(
  delegate: ProfileApiDelegate?,
) : ProfileApi {
  private lateinit var delegate: ProfileApiDelegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : ProfileApiDelegate {})
  }

  override fun getDelegate(): ProfileApiDelegate = delegate
}
