package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.test.context.event.annotation.BeforeTestMethod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

private val charPoolMultiCaseNumbers = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private val charPoolUpperCase = ('A'..'Z').toList()
private val charPoolLowerCase = ('a'..'z').toList()
private val charPoolNumbers = ('0'..'9').toList()

private fun randomWithCharPool(charPool: List<Char>, length: Int) = (1..length)
  .map { Random.nextInt(0, charPool.size) }
  .map(charPool::get)
  .joinToString("")

fun randomStringMultiCaseWithNumbers(length: Int) = randomWithCharPool(charPoolMultiCaseNumbers, length)

fun randomStringUpperCase(length: Int) = randomWithCharPool(charPoolUpperCase, length)
fun randomStringLowerCase(length: Int) = randomWithCharPool(charPoolLowerCase, length)

fun randomEmailAddress() = randomWithCharPool(charPoolLowerCase, 5) + "." + randomWithCharPool(charPoolLowerCase, 8) + "@" + randomWithCharPool(charPoolLowerCase, 6) + ".com"

fun randomNumberChars(length: Int) = randomWithCharPool(charPoolNumbers, length)

fun randomPostCode() = randomStringUpperCase(2) + randomNumberChars(1) + " " +
  randomStringUpperCase(2) + randomNumberChars(1)

fun randomInt(min: Int, max: Int) = Random.nextInt(min, max)
fun randomLong() = Random.nextLong(Long.MIN_VALUE, Long.MAX_VALUE)
fun randomDouble(min: Double, max: Double) = Random.nextDouble(min, max)

fun LocalDate.nonRepeatingRandomDateAfter(groupingKey: String, maxDays: Int): LocalDate = NonRepeatingRandomValueManager.getInstance().generateUniqueValue(groupingKey) {
  this.randomDateAfter(maxDays)
}

fun LocalDate.randomDateAfter(maxDays: Int): LocalDate = this.plusDays(randomInt(1, maxDays).toLong())
fun LocalDate.randomDateBefore(maxDays: Int): LocalDate = this.minusDays(randomInt(1, maxDays).toLong())
fun LocalDate.randomDateAround(maxDays: Int): LocalDate = this.minusDays(maxDays.toLong()).randomDateAfter(maxDays * 2)

fun LocalDateTime.randomDateTimeBefore(maxDays: Int): LocalDateTime = this.minusDays(randomInt(1, maxDays).toLong())

fun OffsetDateTime.randomDateTimeAfter(maxDays: Int): OffsetDateTime = this.plusMinutes(randomInt(1, 60 * 24 * maxDays).toLong()).truncatedTo(ChronoUnit.SECONDS)
fun OffsetDateTime.randomDateTimeBefore(maxDays: Int): OffsetDateTime = this.minusMinutes(randomInt(1, 60 * 24 * maxDays).toLong()).truncatedTo(ChronoUnit.SECONDS)

fun Instant.randomDateTimeBefore(maxDays: Int): Instant = this.minus(randomInt(1, 60 * 24 * maxDays).toLong(), ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)

fun <T> randomOf(options: List<T>) = options[randomInt(0, options.size - 1)]

@Service
class NonRepeatingRandomValueManager(applicationContext: ApplicationContext) {
  private val generatedValuesByKey = mutableMapOf<String, Set<*>>()

  companion object Accessor {
    private lateinit var applicationContext: ApplicationContext
    fun getInstance(): NonRepeatingRandomValueManager = applicationContext.getBean(NonRepeatingRandomValueManager::class.java)
  }

  init {
    Accessor.applicationContext = applicationContext
  }

  @BeforeTestMethod
  fun resetBuckets() {
    generatedValuesByKey.clear()
  }

  fun <T> generateUniqueValue(key: String, generator: () -> T): T {
    val generatedValues = generatedValuesByKey.getOrPut(key) { mutableSetOf<T>() } as MutableSet<T>

    var candidate = generator.invoke()
    while (generatedValues.contains(candidate)) {
      candidate = generator.invoke()
    }

    generatedValues.add(candidate)
    return candidate
  }
}
