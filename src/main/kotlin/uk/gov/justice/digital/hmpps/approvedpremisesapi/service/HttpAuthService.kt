package uk.gov.justice.digital.hmpps.approvedpremisesapi.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.AuthAwareAuthenticationToken
import uk.gov.justice.digital.hmpps.approvedpremisesapi.problem.ForbiddenProblem

@Service
class HttpAuthService {
  fun getNomisPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "nomis") {
      throw ForbiddenProblem()
    }

    return principal
  }

  fun getCas2AuthenticatedPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken
    if (!listOf("nomis", "auth").contains(principal.token.claims["auth_source"])) {
      throw ForbiddenProblem()
    }

    return principal
  }

  fun getCas2v2AuthenticatedPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken
    if (!listOf("nomis", "auth").contains(principal.token.claims["auth_source"])) {
      throw ForbiddenProblem()
    }

    return principal
  }

  fun getDeliusPrincipalOrThrow(): AuthAwareAuthenticationToken {
    val principal = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken

    if (principal.token.claims["auth_source"] != "delius") {
      throw ForbiddenProblem()
    }

    return principal
  }

  fun getDeliusPrincipalOrNull(): AuthAwareAuthenticationToken? {
    val principal = SecurityContextHolder.getContext().authentication as? AuthAwareAuthenticationToken ?: return null

    if (principal.token.claims["auth_source"] != "delius") {
      return null
    }

    return principal
  }
}
