package uk.gov.justice.digital.hmpps.approvedpremisesapi.controller.generated.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.11.0",
)
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class PeopleCas1Controller(
  delegate: PeopleCas1Delegate?,
) : PeopleCas1 {
  private lateinit var delegate: PeopleCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object : PeopleCas1Delegate {})
  }

  override fun getDelegate(): PeopleCas1Delegate = delegate
}
