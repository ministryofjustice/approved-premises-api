package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.cas1

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(
  value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"],
  comments = "Generator version: 7.11.0",
)
@Controller
@RequestMapping("\${openapi.communityAccommodationServicesApprovedPremisesCAS1.base-path:/cas1}")
class OutOfServiceBedsCas1Controller(
    delegate: uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.cas1.OutOfServiceBedsCas1Delegate?,
) : uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.cas1.OutOfServiceBedsCas1 {
  private lateinit var delegate: uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.cas1.OutOfServiceBedsCas1Delegate

  init {
    this.delegate = Optional.ofNullable(delegate).orElse(object :
        uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.cas1.OutOfServiceBedsCas1Delegate {})
  }

  override fun getDelegate(): uk.gov.justice.digital.hmpps.approvedpremisesapi.api.cas1.cas1.OutOfServiceBedsCas1Delegate = delegate
}
