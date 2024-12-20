package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

fun OffsetDateTime.toTimestamp(): Timestamp {
  return Timestamp.from(this.toInstant())
}

fun OffsetDateTime.roundNanosToMillisToAccountForLossOfPrecisionInPostgres(): OffsetDateTime =
  this.withNano(nanosToNearestMilli(this.nano.toLong()))

fun LocalDateTime.roundNanosToMillisToAccountForLossOfPrecisionInPostgres(): LocalDateTime =
  this.withNano(nanosToNearestMilli(this.nano.toLong()))

private fun nanosToNearestMilli(nanos: Long): Int {
  val millis = TimeUnit.NANOSECONDS.toMillis(nanos)
  return TimeUnit.MILLISECONDS.toNanos(millis).toInt()
}
