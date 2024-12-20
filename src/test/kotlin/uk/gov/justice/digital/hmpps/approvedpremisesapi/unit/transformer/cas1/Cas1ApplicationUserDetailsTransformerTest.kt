package uk.gov.justice.digital.hmpps.approvedpremisesapi.unit.transformer.cas1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.transformer.cas1.Cas1ApplicationUserDetailsTransformer
import java.util.UUID

class Cas1ApplicationUserDetailsTransformerTest {

  @Test
  fun transformJpaToApi() {
    val result = Cas1ApplicationUserDetailsTransformer().transformJpaToApi(
      Cas1ApplicationUserDetailsEntity(
        id = UUID.randomUUID(),
        name = "theName",
        email = "theEmail",
        telephoneNumber = "theTelephoneNumber",
      ),
    )

    assertThat(result.name).isEqualTo("theName")
    assertThat(result.email).isEqualTo("theEmail")
    assertThat(result.telephoneNumber).isEqualTo("theTelephoneNumber")
  }
}
