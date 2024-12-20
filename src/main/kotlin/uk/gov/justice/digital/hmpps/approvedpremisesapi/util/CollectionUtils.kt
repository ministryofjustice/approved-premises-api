package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

fun <T> Collection<T>.containsAny(vararg values: T): Boolean = values.toSet().intersect(this.toSet()).isNotEmpty()
