package uk.gov.justice.digital.hmpps.approvedpremisesapi.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.BodyContentSpec.jsonForObject(value: Any): WebTestClient.BodyContentSpec {
  val objectMapper =
    ApplicationContextProvider.get().getBean(ObjectMapper::class.java)

  return this.json(objectMapper.writeValueAsString(value))
}

inline fun <reified T> WebTestClient.ResponseSpec.bodyAsObject(): T {
  return this.returnResult(T::class.java).responseBody.blockFirst()!!
}

inline fun <reified T> WebTestClient.ResponseSpec.bodyAsListOfObjects(): List<T> {
  val objectMapper =
    ApplicationContextProvider.get().getBean(ObjectMapper::class.java)

  val rawResponseBody = this.returnResult(String::class.java)
  return objectMapper.readValue<List<T>>(rawResponseBody.responseBody.blockFirst()!!)
}
