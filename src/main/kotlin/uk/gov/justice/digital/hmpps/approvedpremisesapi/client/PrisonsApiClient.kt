package uk.gov.justice.digital.hmpps.approvedpremisesapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.WebClientConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.AdjudicationsPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import java.time.Duration

@Component
class PrisonsApiClient(
  @Qualifier("prisonsApiWebClient") webClientConfig: WebClientConfig,
  objectMapper: ObjectMapper,
  webClientCache: WebClientCache,
) : BaseHMPPSClient(webClientConfig, objectMapper, webClientCache) {
  private val inmateDetailsCacheConfig = WebClientCache.PreemptiveCacheConfig(
    cacheName = "inmateDetails",
    successSoftTtlSeconds = Duration.ofHours(6).toSeconds().toInt(),
    successSoftTtlJitterSeconds = Duration.ofHours(1).toSeconds(),
    failureSoftTtlBackoffSeconds = listOf(
      30,
      Duration.ofMinutes(5).toSeconds().toInt(),
      Duration.ofMinutes(10).toSeconds().toInt(),
      Duration.ofMinutes(30).toSeconds().toInt(),
    ),
    hardTtlSeconds = Duration.ofHours(12).toSeconds().toInt(),
  )

  fun getInmateDetailsWithWait(nomsNumber: String) = getRequest<InmateDetail> {
    preemptiveCacheConfig = inmateDetailsCacheConfig
    preemptiveCacheKey = nomsNumber
    preemptiveCacheTimeoutMs = 0
  }

  fun getInmateDetailsWithCall(nomsNumber: String) = getRequest<InmateDetail> {
    path = "/api/offenders/$nomsNumber"
    isPreemptiveCall = true
    preemptiveCacheConfig = inmateDetailsCacheConfig
    preemptiveCacheKey = nomsNumber
  }

  fun getInmateDetailsCacheEntryStatus(nomsNumber: String) = checkPreemptiveCacheStatus(inmateDetailsCacheConfig, nomsNumber)

  fun getAdjudicationsPage(nomsNumber: String, offset: Int?, pageSize: Int) = getRequest<AdjudicationsPage> {
    withHeader("Page-Limit", pageSize.toString())

    if (offset != null) {
      withHeader("Page-Offset", offset.toString())
    }

    path = "/api/offenders/$nomsNumber/adjudications"
  }
}
