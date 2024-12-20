package uk.gov.justice.digital.hmpps.approvedpremisesapi.config

import io.sentry.SamplingContext
import io.sentry.SentryOptions
import org.springframework.http.server.PathContainer
import org.springframework.stereotype.Component
import org.springframework.web.util.pattern.PathPatternParser

@Component
class TracesSamplerCallback : SentryOptions.TracesSamplerCallback {
  private val ignoredPathPatterns = listOf("/health/**").let {
    val parser = PathPatternParser()

    it.map { pattern -> parser.parse(pattern) }
  }

  @SuppressWarnings("MagicNumber")
  override fun sample(context: SamplingContext): Double? {
    val parentSampled = context.transactionContext.parentSampled
    if (parentSampled != null) {
      return if (parentSampled) 0.01 else 0.0
    }

    val path = PathContainer.parsePath(removeHttpMethodPrefix(context.transactionContext.name))

    if (ignoredPathPatterns.any { it.matches(path) }) {
      return 0.0
    }

    return null
  }

  private fun removeHttpMethodPrefix(transactionName: String) = transactionName
    .replace("GET ", "")
    .replace("PUT ", "")
    .replace("POST ", "")
    .replace("PATCH ", "")
    .replace("DELETE ", "")
}
