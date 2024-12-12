package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 'Initial' means that the request for placement was created for the arrival date included on the original application.  'Additional' means the request for placement was created after the application had been assessed as suitable. A given application should only have, at most, one request for placement of type 'Initial'. 
* Values: initial,additional
*/
enum class PlacementApplicationType(val value: kotlin.String) {

    @JsonProperty("Initial") initial("Initial"),
    @JsonProperty("Additional") additional("Additional")
}

