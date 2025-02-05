package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2v2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas2v2.Cas2v2UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.IntegrationTestDbManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.TestPropertiesInitializer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2v2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas2v2.Cas2v2UserRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2v2ApplicationJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2v2ApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2v2StatusUpdateTestRepository
import java.util.UUID

@ExtendWith(IntegrationTestDbManager.IntegrationTestListener::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [TestPropertiesInitializer::class])
@ActiveProfiles("test")
@Tag("integration")
abstract class Cas2v2IntegrationTestBase : IntegrationTestBase() {

  @Autowired
  lateinit var cas2v2ApplicationRepository: Cas2v2ApplicationTestRepository

  @Autowired
  lateinit var cas2v2AssessmentRepository: Cas2v2AssessmentRepository

  @Autowired
  lateinit var cas2v2StatusUpdateRepository: Cas2v2StatusUpdateTestRepository

  @Autowired
  lateinit var cas2v2UserRepository: Cas2v2UserRepository

  @Autowired
  lateinit var cas2v2ApplicationJsonSchemaRepository: Cas2v2ApplicationJsonSchemaTestRepository

  lateinit var cas2v2ApplicationEntityFactory: PersistedFactory<Cas2v2ApplicationEntity, UUID, Cas2v2ApplicationEntityFactory>
  lateinit var cas2v2AssessmentEntityFactory: PersistedFactory<Cas2v2AssessmentEntity, UUID, Cas2v2AssessmentEntityFactory>
  lateinit var cas2v2StatusUpdateEntityFactory: PersistedFactory<Cas2v2StatusUpdateEntity, UUID, Cas2v2StatusUpdateEntityFactory>
  lateinit var cas2v2UserEntityFactory: PersistedFactory<Cas2v2UserEntity, UUID, Cas2v2UserEntityFactory>
  lateinit var cas2v2ApplicationJsonSchemaEntityFactory: PersistedFactory<Cas2v2ApplicationJsonSchemaEntity, UUID, Cas2v2ApplicationJsonSchemaEntityFactory>

  override fun setupFactories() {
    super.setupFactories()
    cas2v2ApplicationEntityFactory = PersistedFactory({ Cas2v2ApplicationEntityFactory() }, cas2v2ApplicationRepository)
    cas2v2AssessmentEntityFactory = PersistedFactory({ Cas2v2AssessmentEntityFactory() }, cas2v2AssessmentRepository)
    cas2v2StatusUpdateEntityFactory =
      PersistedFactory({ Cas2v2StatusUpdateEntityFactory() }, cas2v2StatusUpdateRepository)
    cas2v2UserEntityFactory = PersistedFactory({ Cas2v2UserEntityFactory() }, cas2v2UserRepository)
    cas2v2ApplicationJsonSchemaEntityFactory =
      PersistedFactory({ Cas2v2ApplicationJsonSchemaEntityFactory() }, cas2v2ApplicationJsonSchemaRepository)
  }

  fun mockSomeOtherCall(staffMember: StaffMember, qCode: String) = wiremockServer.stubFor(
    WireMock.get(urlEqualTo("/approved-premises/$qCode/staff"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(
              StaffMembersPage(
                content = listOf(staffMember),
              ),
            ),
          ),
      ),
  )
}
