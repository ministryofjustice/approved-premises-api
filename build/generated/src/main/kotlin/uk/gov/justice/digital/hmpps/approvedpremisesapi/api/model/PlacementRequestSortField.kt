package uk.gov.justice.digital.hmpps.approvedpremisesapi.api.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
* 
* Values: duration,expectedArrival,createdAt,applicationSubmittedAt,requestType,personName,personRisksTier
*/
enum class PlacementRequestSortField(val value: kotlin.String) {

    @JsonProperty("duration") duration("duration"),
    @JsonProperty("expected_arrival") expectedArrival("expected_arrival"),
    @JsonProperty("created_at") createdAt("created_at"),
    @JsonProperty("application_date") applicationSubmittedAt("application_date"),
    @JsonProperty("request_type") requestType("request_type"),
    @JsonProperty("person_name") personName("person_name"),
    @JsonProperty("person_risks_tier") personRisksTier("person_risks_tier")
}

