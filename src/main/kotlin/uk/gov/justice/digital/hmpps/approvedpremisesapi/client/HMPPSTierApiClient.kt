package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppstier.Tier

@Component
class HMPPSTierApiClient(
  @Qualifier("hmppsTierApiWebClient") webClient: WebClient,
  objectMapper: ObjectMapper,
  redisTemplate: RedisTemplate<String, String>
) : BaseHMPPSClient(webClient, objectMapper, redisTemplate) {
  fun getTier(crn: String) = getRequest<Tier> {
    path = "/crn/$crn/tier"
  }
}
