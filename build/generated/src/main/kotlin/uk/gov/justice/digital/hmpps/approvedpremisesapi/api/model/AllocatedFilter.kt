package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: allocated,unallocated
*/
enum class AllocatedFilter(val value: kotlin.String) {

    @JsonProperty("allocated") allocated("allocated"),
    @JsonProperty("unallocated") unallocated("unallocated")
}

