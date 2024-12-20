package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.netty.channel.ChannelOption
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

data class WebClientConfig(
  val webClient: WebClient,
  val maxRetryAttempts: Long = 1,
)

@Configuration
class WebClientConfiguration(
  @Value("\${upstream-timeout-ms}") private val upstreamTimeoutMs: Long,
  @Value("\${case-notes-service-upstream-timeout-ms}") private val caseNotesServiceUpstreamTimeoutMs: Long,
  @Value("\${web-clients.max-response-in-memory-size-bytes}") private val defaultMaxResponseInMemorySizeBytes: Int,
  @Value("\${web-clients.prison-api-max-response-in-memory-size-bytes}") private val prisonApiMaxResponseInMemorySizeBytes: Int,
  @Value("\${web-clients.probation-offender-search-api-max-response-in-memory-size-bytes}") private val probationOffenderSearchApiMaxResponseInMemorySizeBytes: Int,
) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Bean
  fun authorizedClientManager(clients: ClientRegistrationRepository): OAuth2AuthorizedClientManager {
    val service: OAuth2AuthorizedClientService = InMemoryOAuth2AuthorizedClientService(clients)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service)
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
      .clientCredentials()
      .build()
    manager.setAuthorizedClientProvider(authorizedClientProvider)
    return manager
  }

  @Bean(name = ["apDeliusContextApiWebClient"])
  fun apDeliusContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.ap-delius-context-api.base-url}") apDeliusContextApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(apDeliusContextApiBaseUrl)
        .filter(oauth2Client)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(defaultMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .build(),
    )
  }

  @Bean(name = ["hmppsTierApiWebClient"])
  fun hmppsTierApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.hmpps-tier.base-url}") hmppsTierApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("hmpps-tier")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(hmppsTierApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["prisonsApiWebClient"])
  fun prisonsApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.prisons-api.base-url}") prisonsApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("prisons-api")

    log.info("Using maxInMemorySize of $prisonApiMaxResponseInMemorySizeBytes bytes for Prison API Web Client")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(prisonsApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(prisonApiMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["caseNotesWebClient"])
  fun caseNotesWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.case-notes.base-url}") caseNotesBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("case-notes")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(caseNotesBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(caseNotesServiceUpstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["apOASysContextApiWebClient"])
  fun apOASysContextApiWebClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    authorizedClientManager: OAuth2AuthorizedClientManager,
    @Value("\${services.ap-oasys-context-api.base-url}") apOASysContextApiBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)

    oauth2Client.setDefaultClientRegistrationId("ap-oasys-context")

    return WebClientConfig(
      WebClient.builder()
        .baseUrl(apOASysContextApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .build(),
    )
  }

  @Bean(name = ["govUKBankHolidaysApiWebClient"])
  fun govUKBankHolidaysApiClient(
    @Value("\${services.gov-uk-bank-holidays-api.base-url}") govUKBankHolidaysApiBaseUrl: String,
  ): WebClientConfig {
    return WebClientConfig(
      WebClient.builder()
        .baseUrl(govUKBankHolidaysApiBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .build(),
    )
  }

  @Bean(name = ["nomisUserRolesApiWebClient"])
  fun nomisUserRolesApiClient(
    @Value("\${services.nomis-user-roles-api.base-url}") nomisUserRolesBaseUrl: String,
  ): WebClientConfig {
    return WebClientConfig(
      WebClient.builder()
        .baseUrl(nomisUserRolesBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .build(),
    )
  }

  @Bean(name = ["manageUsersApiWebClient"])
  fun manageUsersApiClient(
    @Value("\${services.manage-users-api.base-url}") manageUsersBaseUrl: String,
  ): WebClientConfig {
    return WebClientConfig(
      WebClient.builder()
        .baseUrl(manageUsersBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .build(),
    )
  }

  @Bean(name = ["probationOffenderSearchApiWebClient"])
  fun probationOffenderSearchApiClient(
    clientRegistrations: ClientRegistrationRepository,
    authorizedClients: OAuth2AuthorizedClientRepository,
    @Value("\${services.probation-offender-search-api.base-url}") probationOffenderSearchBaseUrl: String,
  ): WebClientConfig {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients)

    oauth2Client.setDefaultClientRegistrationId("delius-backed-apis")

    return WebClientConfig(
      webClient = WebClient.builder()
        .baseUrl(probationOffenderSearchBaseUrl)
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient
              .create()
              .responseTimeout(Duration.ofMillis(upstreamTimeoutMs))
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Duration.ofMillis(upstreamTimeoutMs).toMillis().toInt()),
          ),
        )
        .filter(oauth2Client)
        .exchangeStrategies(
          ExchangeStrategies.builder().codecs {
            it.defaultCodecs().maxInMemorySize(probationOffenderSearchApiMaxResponseInMemorySizeBytes)
          }.build(),
        )
        .build(),
    )
  }
}
