package uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext

data class UserOffenderAccess(
  val userRestricted: Boolean,
  val userExcluded: Boolean,
  val restrictionMessage: String?,
)
