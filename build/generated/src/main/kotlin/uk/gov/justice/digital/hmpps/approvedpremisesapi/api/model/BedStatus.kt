package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: occupied,available,outOfService
*/
enum class BedStatus(val value: kotlin.String) {

    @JsonProperty("occupied") occupied("occupied"),
    @JsonProperty("available") available("available"),
    @JsonProperty("out_of_service") outOfService("out_of_service")
}

