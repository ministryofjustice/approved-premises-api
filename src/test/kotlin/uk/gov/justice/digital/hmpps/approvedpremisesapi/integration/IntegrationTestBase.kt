package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.cache.CacheManager
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.client.PrisonsApiClient
import uk.gov.justice.digital.hmpps.approvedpremisesapi.config.NotifyConfig
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AppealEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTeamCodeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApplicationTimelineNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesAssessmentJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ApprovedPremisesPlacementApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentClarificationNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistorySystemNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.AssessmentReferralHistoryUserNoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BedMoveEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.BookingNotMadeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CancellationReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1ApplicationUserDetailsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1OutOfServiceBedRevisionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas1SpaceBookingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2ApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2AssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2NoteEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateDetailEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.Cas2StatusUpdateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.CharacteristicEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ConfirmationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DateChangeEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DepartureReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DestinationProviderEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.DomainEventEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExtensionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ExternalUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.LocalAuthorityEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.MoveOnCategoryEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NomisUserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.NonArrivalReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.OfflineApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PersistedFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementDateEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequestEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PlacementRequirementsEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.PostCodeDistrictEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationAreaProbationRegionMappingEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationDeliveryUnitEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ProbationRegionEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.ReferralRejectionReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.RoomEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationApplicationJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationAssessmentJsonSchemaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TemporaryAccommodationPremisesEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.TurnaroundEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserQualificationAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.UserRoleAssignmentEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas1.Cas1CruManagementAreaEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceCancellationEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.factory.cas3.Cas3VoidBedspaceReasonEntityFactory
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.asserter.DomainEventAsserter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.asserter.EmailNotificationAsserter
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.IntegrationTestDbManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.OpenEntityInTestManager
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.config.TestPropertiesInitializer
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.MockFeatureFlagService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.MutableClockConfiguration
import uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.mocks.NoOpSentryService
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AppealEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTeamCodeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApplicationTimelineNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ApprovedPremisesPlacementApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentClarificationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistorySystemNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.AssessmentReferralHistoryUserNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedMoveRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BedRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.BookingNotMadeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CancellationReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1ApplicationUserDetailsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1CruManagementAreaRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1OutOfServiceBedRevisionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas1SpaceBookingRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2ApplicationNoteRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2AssessmentRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateDetailRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.Cas2StatusUpdateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.CharacteristicRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ConfirmationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DateChangeRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DepartureReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DestinationProviderEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.DomainEventEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExtensionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ExternalUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.LocalAuthorityAreaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.MoveOnCategoryEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NomisUserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.NonArrivalReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.OfflineApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementApplicationRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementDateRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PlacementRequirementsRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.PostCodeDistrictEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationAreaProbationRegionMappingEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationDeliveryUnitEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ProbationRegionEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.ReferralRejectionReasonRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.RoomRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationApplicationJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationAssessmentJsonSchemaEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TemporaryAccommodationPremisesEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.TurnaroundEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserQualificationAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.UserRoleAssignmentEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceCancellationEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.jpa.entity.cas3.Cas3VoidBedspaceReasonEntity
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMember
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.deliuscontext.StaffMembersPage
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.hmppsauth.GetTokenResponse
import uk.gov.justice.digital.hmpps.approvedpremisesapi.model.prisonsapi.InmateDetail
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.AppealTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApplicationTeamCodeTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesApplicationJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesAssessmentJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesAssessmentTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesPlacementApplicationJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ApprovedPremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.AssessmentClarificationNoteTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.AssessmentReferralHistorySystemNoteTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.AssessmentReferralHistoryUserNoteTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.AssessmentTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingNotMadeTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.BookingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CancellationReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.CancellationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1OutOfServiceBedCancellationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1OutOfServiceBedDetailsTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1OutOfServiceBedReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas1OutOfServiceBedTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2ApplicationJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2ApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas2StatusUpdateTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3VoidBedspaceCancellationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3VoidBedspaceReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.Cas3VoidBedspacesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ConfirmationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DepartureTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DestinationProviderTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.DomainEventTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ExtensionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ExternalUserTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.LocalAuthorityAreaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.MoveOnCategoryTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NomisUserTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NonArrivalReasonTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.NonArrivalTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.OfflineApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PlacementApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PlacementRequestTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.PostCodeDistrictTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationAreaProbationRegionMappingTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationDeliveryUnitTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.ProbationRegionTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.RoomTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationApplicationJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationApplicationTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationAssessmentJsonSchemaTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationAssessmentTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TemporaryAccommodationPremisesTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.TurnaroundTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.UserQualificationAssignmentTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.UserRoleAssignmentTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.repository.UserTestRepository
import uk.gov.justice.digital.hmpps.approvedpremisesapi.util.JwtAuthHelper
import java.time.Duration
import java.util.TimeZone
import java.util.UUID

@ExtendWith(value =
  [
    IntegrationTestDbManager.IntegrationTestListener::class,
    OpenEntityInTestManager.IntegrationTestListener::class,
  ]
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(initializers = [TestPropertiesInitializer::class])
@ActiveProfiles("test")
@Tag("integration")
abstract class IntegrationTestBase {
  @Autowired
  lateinit var cas1SpaceBookingRepository: Cas1SpaceBookingRepository

  @Autowired
  private lateinit var cas2StatusUpdateDetailRepository: Cas2StatusUpdateDetailRepository
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Value("\${preemptive-cache-key-prefix}")
  lateinit var preemptiveCacheKeyPrefix: String

  @Autowired
  lateinit var wiremockManager: WiremockManager

  val wiremockServer: WireMockServer by lazy {
    wiremockManager.wiremockServer
  }

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var cacheManager: CacheManager

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var redisTemplate: RedisTemplate<String, String>

  @Autowired
  private lateinit var prisonsApiClient: PrisonsApiClient

  @Autowired
  lateinit var probationRegionRepository: ProbationRegionTestRepository

  @Autowired
  lateinit var apAreaRepository: ApAreaTestRepository

  @Autowired
  lateinit var localAuthorityAreaRepository: LocalAuthorityAreaTestRepository

  @Autowired
  lateinit var approvedPremisesRepository: ApprovedPremisesTestRepository

  @Autowired
  lateinit var temporaryAccommodationPremisesRepository: TemporaryAccommodationPremisesTestRepository

  @Autowired
  lateinit var bookingRepository: BookingTestRepository

  @Autowired
  lateinit var bedMoveRepository: BedMoveRepository

  @Autowired
  lateinit var arrivalRepository: ArrivalTestRepository

  @Autowired
  lateinit var confirmationRepository: ConfirmationTestRepository

  @Autowired
  lateinit var departureRepository: DepartureTestRepository

  @Autowired
  lateinit var destinationProviderRepository: DestinationProviderTestRepository

  @Autowired
  lateinit var nonArrivalRepository: NonArrivalTestRepository

  @Autowired
  lateinit var cancellationRepository: CancellationTestRepository

  @Autowired
  lateinit var departureReasonRepository: DepartureReasonTestRepository

  @Autowired
  lateinit var moveOnCategoryRepository: MoveOnCategoryTestRepository

  @Autowired
  lateinit var cancellationReasonRepository: CancellationReasonTestRepository

  @Autowired
  lateinit var cas3VoidBedspaceReasonTestRepository: Cas3VoidBedspaceReasonTestRepository

  @Autowired
  lateinit var cas3VoidBedspacesTestRepository: Cas3VoidBedspacesTestRepository

  @Autowired
  lateinit var cas3VoidBedspaceCancellationTestRepository: Cas3VoidBedspaceCancellationTestRepository

  @Autowired
  lateinit var extensionRepository: ExtensionTestRepository

  @Autowired
  lateinit var dateChangeRepository: DateChangeRepository

  @Autowired
  lateinit var nonArrivalReasonRepository: NonArrivalReasonTestRepository

  @Autowired
  lateinit var approvedPremisesApplicationRepository: ApprovedPremisesApplicationTestRepository

  @Autowired
  lateinit var cas2ApplicationRepository: Cas2ApplicationTestRepository

  @Autowired
  lateinit var cas2AssessmentRepository: Cas2AssessmentRepository

  @Autowired
  lateinit var cas2StatusUpdateRepository: Cas2StatusUpdateTestRepository

  @Autowired
  lateinit var cas2NoteRepository: Cas2ApplicationNoteRepository

  @Autowired
  lateinit var temporaryAccommodationApplicationRepository: TemporaryAccommodationApplicationTestRepository

  @Autowired
  lateinit var offlineApplicationRepository: OfflineApplicationTestRepository

  @Autowired
  lateinit var approvedPremisesApplicationJsonSchemaRepository: ApprovedPremisesApplicationJsonSchemaTestRepository

  @Autowired
  lateinit var cas2ApplicationJsonSchemaRepository: Cas2ApplicationJsonSchemaTestRepository

  @Autowired
  lateinit var temporaryAccommodationApplicationJsonSchemaRepository: TemporaryAccommodationApplicationJsonSchemaTestRepository

  @Autowired
  lateinit var approvedPremisesAssessmentJsonSchemaRepository: ApprovedPremisesAssessmentJsonSchemaTestRepository

  @Autowired
  lateinit var temporaryAccommodationAssessmentJsonSchemaRepository: TemporaryAccommodationAssessmentJsonSchemaTestRepository

  @Autowired
  lateinit var approvedPremisesPlacementApplicationJsonSchemaRepository: ApprovedPremisesPlacementApplicationJsonSchemaTestRepository

  @Autowired
  lateinit var userRepository: UserTestRepository

  @Autowired
  lateinit var nomisUserRepository: NomisUserTestRepository

  @Autowired
  lateinit var externalUserRepository: ExternalUserTestRepository

  @Autowired
  lateinit var userRoleAssignmentRepository: UserRoleAssignmentTestRepository

  @Autowired
  lateinit var userQualificationAssignmentRepository: UserQualificationAssignmentTestRepository

  @Autowired
  lateinit var approvedPremisesAssessmentRepository: ApprovedPremisesAssessmentTestRepository

  @Autowired
  lateinit var applicationTimelineNoteRepository: ApplicationTimelineNoteRepository

  @Autowired
  lateinit var temporaryAccommodationAssessmentRepository: TemporaryAccommodationAssessmentTestRepository

  @Autowired
  lateinit var assessmentClarificationNoteRepository: AssessmentClarificationNoteTestRepository

  @Autowired
  lateinit var assessmentReferralUserNoteRepository: AssessmentReferralHistoryUserNoteTestRepository

  @Autowired
  lateinit var assessmentReferralSystemNoteRepository: AssessmentReferralHistorySystemNoteTestRepository

  @Autowired
  lateinit var characteristicRepository: CharacteristicRepository

  @Autowired
  lateinit var roomRepository: RoomRepository

  @Autowired
  lateinit var bedRepository: BedRepository

  @Autowired
  lateinit var domainEventRepository: DomainEventTestRepository

  @Autowired
  lateinit var applicationTeamCodeRepository: ApplicationTeamCodeTestRepository

  @Autowired
  lateinit var postCodeDistrictRepository: PostCodeDistrictTestRepository

  @Autowired
  lateinit var placementRequestRepository: PlacementRequestRepository

  @Autowired
  lateinit var placementRequirementsRepository: PlacementRequirementsRepository

  @Autowired
  lateinit var placementApplicationRepository: PlacementApplicationRepository

  @Autowired
  lateinit var placementDateRepository: PlacementDateRepository

  @Autowired
  lateinit var bookingNotMadeRepository: BookingNotMadeTestRepository

  @Autowired
  lateinit var probationDeliveryUnitRepository: ProbationDeliveryUnitTestRepository

  @Autowired
  lateinit var turnaroundRepository: TurnaroundTestRepository

  @Autowired
  lateinit var probationAreaProbationRegionMappingRepository: ProbationAreaProbationRegionMappingTestRepository

  @Autowired
  lateinit var roomTestRepository: RoomTestRepository

  @Autowired
  lateinit var placementApplicationTestRepository: PlacementApplicationTestRepository

  @Autowired
  lateinit var assessmentTestRepository: AssessmentTestRepository

  @Autowired
  lateinit var placementRequestTestRepository: PlacementRequestTestRepository

  @Autowired
  lateinit var appealTestRepository: AppealTestRepository

  @Autowired
  lateinit var cas1ApplicationUserDetailsRepository: Cas1ApplicationUserDetailsRepository

  @Autowired
  lateinit var referralRejectionReasonRepository: ReferralRejectionReasonRepository

  @Autowired
  lateinit var cas1OutOfServiceBedTestRepository: Cas1OutOfServiceBedTestRepository

  @Autowired
  lateinit var cas1OutOfServiceBedReasonTestRepository: Cas1OutOfServiceBedReasonTestRepository

  @Autowired
  lateinit var cas1OutOfServiceBedCancellationTestRepository: Cas1OutOfServiceBedCancellationTestRepository

  @Autowired
  lateinit var cas1OutOfServiceBedDetailsTestRepository: Cas1OutOfServiceBedDetailsTestRepository

  @Autowired
  lateinit var cas1CruManagementAreaRepository: Cas1CruManagementAreaRepository

  @Autowired
  lateinit var emailAsserter: EmailNotificationAsserter

  @Autowired
  lateinit var notifyConfig: NotifyConfig

  @Autowired
  lateinit var snsDomainEventListener: SnsDomainEventListener

  @Autowired
  lateinit var domainEventAsserter: DomainEventAsserter

  @Autowired
  lateinit var clock: MutableClockConfiguration.MutableClock

  @Autowired
  lateinit var mockSentryService: NoOpSentryService

  @Autowired
  lateinit var mockFeatureFlagService: MockFeatureFlagService

  lateinit var probationRegionEntityFactory: PersistedFactory<ProbationRegionEntity, UUID, ProbationRegionEntityFactory>
  lateinit var apAreaEntityFactory: PersistedFactory<ApAreaEntity, UUID, ApAreaEntityFactory>
  lateinit var localAuthorityEntityFactory: PersistedFactory<LocalAuthorityAreaEntity, UUID, LocalAuthorityEntityFactory>
  lateinit var approvedPremisesEntityFactory: PersistedFactory<ApprovedPremisesEntity, UUID, ApprovedPremisesEntityFactory>
  lateinit var temporaryAccommodationPremisesEntityFactory: PersistedFactory<TemporaryAccommodationPremisesEntity, UUID, TemporaryAccommodationPremisesEntityFactory>
  lateinit var bookingEntityFactory: PersistedFactory<BookingEntity, UUID, BookingEntityFactory>
  lateinit var arrivalEntityFactory: PersistedFactory<ArrivalEntity, UUID, ArrivalEntityFactory>
  lateinit var confirmationEntityFactory: PersistedFactory<ConfirmationEntity, UUID, ConfirmationEntityFactory>
  lateinit var departureEntityFactory: PersistedFactory<DepartureEntity, UUID, DepartureEntityFactory>
  lateinit var destinationProviderEntityFactory: PersistedFactory<DestinationProviderEntity, UUID, DestinationProviderEntityFactory>
  lateinit var departureReasonEntityFactory: PersistedFactory<DepartureReasonEntity, UUID, DepartureReasonEntityFactory>
  lateinit var moveOnCategoryEntityFactory: PersistedFactory<MoveOnCategoryEntity, UUID, MoveOnCategoryEntityFactory>
  lateinit var nonArrivalEntityFactory: PersistedFactory<NonArrivalEntity, UUID, NonArrivalEntityFactory>
  lateinit var cancellationEntityFactory: PersistedFactory<CancellationEntity, UUID, CancellationEntityFactory>
  lateinit var cancellationReasonEntityFactory: PersistedFactory<CancellationReasonEntity, UUID, CancellationReasonEntityFactory>
  lateinit var cas3VoidBedspaceEntityFactory: PersistedFactory<Cas3VoidBedspaceEntity, UUID, Cas3VoidBedspaceEntityFactory>
  lateinit var cas3VoidBedspaceReasonEntityFactory: PersistedFactory<Cas3VoidBedspaceReasonEntity, UUID, Cas3VoidBedspaceReasonEntityFactory>
  lateinit var cas3VoidBedspaceCancellationEntityFactory: PersistedFactory<Cas3VoidBedspaceCancellationEntity, UUID, Cas3VoidBedspaceCancellationEntityFactory>
  lateinit var extensionEntityFactory: PersistedFactory<ExtensionEntity, UUID, ExtensionEntityFactory>
  lateinit var dateChangeEntityFactory: PersistedFactory<DateChangeEntity, UUID, DateChangeEntityFactory>
  lateinit var nonArrivalReasonEntityFactory: PersistedFactory<NonArrivalReasonEntity, UUID, NonArrivalReasonEntityFactory>
  lateinit var approvedPremisesApplicationEntityFactory: PersistedFactory<ApprovedPremisesApplicationEntity, UUID, ApprovedPremisesApplicationEntityFactory>
  lateinit var cas2ApplicationEntityFactory: PersistedFactory<Cas2ApplicationEntity, UUID, Cas2ApplicationEntityFactory>
  lateinit var cas2AssessmentEntityFactory: PersistedFactory<Cas2AssessmentEntity, UUID, Cas2AssessmentEntityFactory>
  lateinit var cas2StatusUpdateEntityFactory: PersistedFactory<Cas2StatusUpdateEntity, UUID, Cas2StatusUpdateEntityFactory>
  lateinit var cas2StatusUpdateDetailEntityFactory: PersistedFactory<Cas2StatusUpdateDetailEntity, UUID, Cas2StatusUpdateDetailEntityFactory>
  lateinit var cas2NoteEntityFactory: PersistedFactory<Cas2ApplicationNoteEntity, UUID, Cas2NoteEntityFactory>
  lateinit var temporaryAccommodationApplicationEntityFactory: PersistedFactory<TemporaryAccommodationApplicationEntity, UUID, TemporaryAccommodationApplicationEntityFactory>
  lateinit var offlineApplicationEntityFactory: PersistedFactory<OfflineApplicationEntity, UUID, OfflineApplicationEntityFactory>
  lateinit var approvedPremisesApplicationJsonSchemaEntityFactory: PersistedFactory<ApprovedPremisesApplicationJsonSchemaEntity, UUID, ApprovedPremisesApplicationJsonSchemaEntityFactory>
  lateinit var cas2ApplicationJsonSchemaEntityFactory: PersistedFactory<Cas2ApplicationJsonSchemaEntity, UUID, Cas2ApplicationJsonSchemaEntityFactory>
  lateinit var temporaryAccommodationApplicationJsonSchemaEntityFactory: PersistedFactory<TemporaryAccommodationApplicationJsonSchemaEntity, UUID, TemporaryAccommodationApplicationJsonSchemaEntityFactory>
  lateinit var approvedPremisesPlacementApplicationJsonSchemaEntityFactory: PersistedFactory<ApprovedPremisesPlacementApplicationJsonSchemaEntity, UUID, ApprovedPremisesPlacementApplicationJsonSchemaEntityFactory>
  lateinit var approvedPremisesAssessmentJsonSchemaEntityFactory: PersistedFactory<ApprovedPremisesAssessmentJsonSchemaEntity, UUID, ApprovedPremisesAssessmentJsonSchemaEntityFactory>
  lateinit var temporaryAccommodationAssessmentJsonSchemaEntityFactory: PersistedFactory<TemporaryAccommodationAssessmentJsonSchemaEntity, UUID, TemporaryAccommodationAssessmentJsonSchemaEntityFactory>
  lateinit var userEntityFactory: PersistedFactory<UserEntity, UUID, UserEntityFactory>
  lateinit var nomisUserEntityFactory: PersistedFactory<NomisUserEntity, UUID, NomisUserEntityFactory>
  lateinit var externalUserEntityFactory: PersistedFactory<ExternalUserEntity, UUID, ExternalUserEntityFactory>
  lateinit var userRoleAssignmentEntityFactory: PersistedFactory<UserRoleAssignmentEntity, UUID, UserRoleAssignmentEntityFactory>
  lateinit var userQualificationAssignmentEntityFactory: PersistedFactory<UserQualificationAssignmentEntity, UUID, UserQualificationAssignmentEntityFactory>
  lateinit var approvedPremisesAssessmentEntityFactory: PersistedFactory<ApprovedPremisesAssessmentEntity, UUID, ApprovedPremisesAssessmentEntityFactory>
  lateinit var temporaryAccommodationAssessmentEntityFactory: PersistedFactory<TemporaryAccommodationAssessmentEntity, UUID, TemporaryAccommodationAssessmentEntityFactory>
  lateinit var assessmentClarificationNoteEntityFactory: PersistedFactory<AssessmentClarificationNoteEntity, UUID, AssessmentClarificationNoteEntityFactory>
  lateinit var assessmentReferralHistoryUserNoteEntityFactory: PersistedFactory<AssessmentReferralHistoryUserNoteEntity, UUID, AssessmentReferralHistoryUserNoteEntityFactory>
  lateinit var assessmentReferralHistorySystemNoteEntityFactory: PersistedFactory<AssessmentReferralHistorySystemNoteEntity, UUID, AssessmentReferralHistorySystemNoteEntityFactory>
  lateinit var characteristicEntityFactory: PersistedFactory<CharacteristicEntity, UUID, CharacteristicEntityFactory>
  lateinit var roomEntityFactory: PersistedFactory<RoomEntity, UUID, RoomEntityFactory>
  lateinit var bedEntityFactory: PersistedFactory<BedEntity, UUID, BedEntityFactory>
  lateinit var domainEventFactory: PersistedFactory<DomainEventEntity, UUID, DomainEventEntityFactory>
  lateinit var postCodeDistrictFactory: PersistedFactory<PostCodeDistrictEntity, UUID, PostCodeDistrictEntityFactory>
  lateinit var placementRequestFactory: PersistedFactory<PlacementRequestEntity, UUID, PlacementRequestEntityFactory>
  lateinit var placementRequirementsFactory: PersistedFactory<PlacementRequirementsEntity, UUID, PlacementRequirementsEntityFactory>
  lateinit var bookingNotMadeFactory: PersistedFactory<BookingNotMadeEntity, UUID, BookingNotMadeEntityFactory>
  lateinit var probationDeliveryUnitFactory: PersistedFactory<ProbationDeliveryUnitEntity, UUID, ProbationDeliveryUnitEntityFactory>
  lateinit var applicationTeamCodeFactory: PersistedFactory<ApplicationTeamCodeEntity, UUID, ApplicationTeamCodeEntityFactory>
  lateinit var turnaroundFactory: PersistedFactory<TurnaroundEntity, UUID, TurnaroundEntityFactory>
  lateinit var placementApplicationFactory: PersistedFactory<PlacementApplicationEntity, UUID, PlacementApplicationEntityFactory>
  lateinit var placementDateFactory: PersistedFactory<PlacementDateEntity, UUID, PlacementDateEntityFactory>
  lateinit var probationAreaProbationRegionMappingFactory: PersistedFactory<ProbationAreaProbationRegionMappingEntity, UUID, ProbationAreaProbationRegionMappingEntityFactory>
  lateinit var applicationTimelineNoteEntityFactory: PersistedFactory<ApplicationTimelineNoteEntity, UUID, ApplicationTimelineNoteEntityFactory>
  lateinit var appealEntityFactory: PersistedFactory<AppealEntity, UUID, AppealEntityFactory>
  lateinit var cas1ApplicationUserDetailsEntityFactory: PersistedFactory<Cas1ApplicationUserDetailsEntity, UUID, Cas1ApplicationUserDetailsEntityFactory>
  lateinit var referralRejectionReasonEntityFactory: PersistedFactory<ReferralRejectionReasonEntity, UUID, ReferralRejectionReasonEntityFactory>
  lateinit var cas1OutOfServiceBedEntityFactory: PersistedFactory<Cas1OutOfServiceBedEntity, UUID, Cas1OutOfServiceBedEntityFactory>
  lateinit var cas1OutOfServiceBedReasonEntityFactory: PersistedFactory<Cas1OutOfServiceBedReasonEntity, UUID, Cas1OutOfServiceBedReasonEntityFactory>
  lateinit var cas1OutOfServiceBedCancellationEntityFactory: PersistedFactory<Cas1OutOfServiceBedCancellationEntity, UUID, Cas1OutOfServiceBedCancellationEntityFactory>
  lateinit var cas1OutOfServiceBedRevisionEntityFactory: PersistedFactory<Cas1OutOfServiceBedRevisionEntity, UUID, Cas1OutOfServiceBedRevisionEntityFactory>
  lateinit var cas1SpaceBookingEntityFactory: PersistedFactory<Cas1SpaceBookingEntity, UUID, Cas1SpaceBookingEntityFactory>
  lateinit var cas1CruManagementAreaEntityFactory: PersistedFactory<Cas1CruManagementAreaEntity, UUID, Cas1CruManagementAreaEntityFactory>

  lateinit var bedMoveEntityFactory: PersistedFactory<BedMoveEntity, UUID, BedMoveEntityFactory>
  private var clientCredentialsCallMocked = false

  @BeforeEach
  fun beforeEach(info: TestInfo) {
    log.info("Running test '${info.displayName}'")

    if (!info.tags.contains("isPerClass")) {
      this.setupTests()
    }
  }

  @AfterEach
  fun afterEach(info: TestInfo) {
    if (!info.tags.contains("isPerClass")) {
      this.teardownTests()
    }
  }

  fun setupTests() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    webTestClient = webTestClient.mutate()
      .responseTimeout(Duration.ofMinutes(20))
      .build()

    wiremockManager.beforeTest()

    cacheManager.cacheNames.forEach {
      cacheManager.getCache(it)!!.clear()
    }

    redisTemplate.keys("$preemptiveCacheKeyPrefix-**").forEach(redisTemplate::delete)
    this.setupFactories()
  }

  fun teardownTests() {
    wiremockManager.afterTest()
  }

  fun setupFactories() {
    probationRegionEntityFactory = PersistedFactory({ ProbationRegionEntityFactory() }, probationRegionRepository)
    apAreaEntityFactory = PersistedFactory({ ApAreaEntityFactory() }, apAreaRepository)
    localAuthorityEntityFactory = PersistedFactory({ LocalAuthorityEntityFactory() }, localAuthorityAreaRepository)
    approvedPremisesEntityFactory = PersistedFactory({ ApprovedPremisesEntityFactory() }, approvedPremisesRepository)
    temporaryAccommodationPremisesEntityFactory = PersistedFactory({ TemporaryAccommodationPremisesEntityFactory() }, temporaryAccommodationPremisesRepository)
    bookingEntityFactory = PersistedFactory({ BookingEntityFactory() }, bookingRepository)
    arrivalEntityFactory = PersistedFactory({ ArrivalEntityFactory() }, arrivalRepository)
    confirmationEntityFactory = PersistedFactory({ ConfirmationEntityFactory() }, confirmationRepository)
    departureEntityFactory = PersistedFactory({ DepartureEntityFactory() }, departureRepository)
    destinationProviderEntityFactory = PersistedFactory({ DestinationProviderEntityFactory() }, destinationProviderRepository)
    departureReasonEntityFactory = PersistedFactory({ DepartureReasonEntityFactory() }, departureReasonRepository)
    moveOnCategoryEntityFactory = PersistedFactory({ MoveOnCategoryEntityFactory() }, moveOnCategoryRepository)
    nonArrivalEntityFactory = PersistedFactory({ NonArrivalEntityFactory() }, nonArrivalRepository)
    cancellationEntityFactory = PersistedFactory({ CancellationEntityFactory() }, cancellationRepository)
    cancellationReasonEntityFactory = PersistedFactory({ CancellationReasonEntityFactory() }, cancellationReasonRepository)
    cas3VoidBedspaceEntityFactory = PersistedFactory({ Cas3VoidBedspaceEntityFactory() }, cas3VoidBedspacesTestRepository)
    cas3VoidBedspaceReasonEntityFactory = PersistedFactory({ Cas3VoidBedspaceReasonEntityFactory() }, cas3VoidBedspaceReasonTestRepository)
    cas3VoidBedspaceCancellationEntityFactory = PersistedFactory({ Cas3VoidBedspaceCancellationEntityFactory() }, cas3VoidBedspaceCancellationTestRepository)
    extensionEntityFactory = PersistedFactory({ ExtensionEntityFactory() }, extensionRepository)
    dateChangeEntityFactory = PersistedFactory({ DateChangeEntityFactory() }, dateChangeRepository)
    nonArrivalReasonEntityFactory = PersistedFactory({ NonArrivalReasonEntityFactory() }, nonArrivalReasonRepository)
    approvedPremisesApplicationEntityFactory = PersistedFactory({ ApprovedPremisesApplicationEntityFactory() }, approvedPremisesApplicationRepository)
    cas2ApplicationEntityFactory = PersistedFactory({ Cas2ApplicationEntityFactory() }, cas2ApplicationRepository)
    cas2AssessmentEntityFactory = PersistedFactory({ Cas2AssessmentEntityFactory() }, cas2AssessmentRepository)
    cas2StatusUpdateEntityFactory = PersistedFactory({ Cas2StatusUpdateEntityFactory() }, cas2StatusUpdateRepository)
    cas2StatusUpdateDetailEntityFactory = PersistedFactory({ Cas2StatusUpdateDetailEntityFactory() }, cas2StatusUpdateDetailRepository)
    cas2NoteEntityFactory = PersistedFactory({ Cas2NoteEntityFactory() }, cas2NoteRepository)
    temporaryAccommodationApplicationEntityFactory = PersistedFactory({ TemporaryAccommodationApplicationEntityFactory() }, temporaryAccommodationApplicationRepository)
    offlineApplicationEntityFactory = PersistedFactory({ OfflineApplicationEntityFactory() }, offlineApplicationRepository)
    approvedPremisesApplicationJsonSchemaEntityFactory = PersistedFactory({ ApprovedPremisesApplicationJsonSchemaEntityFactory() }, approvedPremisesApplicationJsonSchemaRepository)
    cas2ApplicationJsonSchemaEntityFactory = PersistedFactory({ Cas2ApplicationJsonSchemaEntityFactory() }, cas2ApplicationJsonSchemaRepository)
    temporaryAccommodationApplicationJsonSchemaEntityFactory = PersistedFactory({ TemporaryAccommodationApplicationJsonSchemaEntityFactory() }, temporaryAccommodationApplicationJsonSchemaRepository)
    approvedPremisesAssessmentJsonSchemaEntityFactory = PersistedFactory({ ApprovedPremisesAssessmentJsonSchemaEntityFactory() }, approvedPremisesAssessmentJsonSchemaRepository)
    temporaryAccommodationAssessmentJsonSchemaEntityFactory = PersistedFactory({ TemporaryAccommodationAssessmentJsonSchemaEntityFactory() }, temporaryAccommodationAssessmentJsonSchemaRepository)
    approvedPremisesPlacementApplicationJsonSchemaEntityFactory = PersistedFactory({ ApprovedPremisesPlacementApplicationJsonSchemaEntityFactory() }, approvedPremisesPlacementApplicationJsonSchemaRepository)
    nomisUserEntityFactory = PersistedFactory({ NomisUserEntityFactory() }, nomisUserRepository)
    externalUserEntityFactory = PersistedFactory({ ExternalUserEntityFactory() }, externalUserRepository)
    userEntityFactory = PersistedFactory({ UserEntityFactory() }, userRepository)
    userRoleAssignmentEntityFactory = PersistedFactory({ UserRoleAssignmentEntityFactory() }, userRoleAssignmentRepository)
    userQualificationAssignmentEntityFactory = PersistedFactory({ UserQualificationAssignmentEntityFactory() }, userQualificationAssignmentRepository)
    approvedPremisesAssessmentEntityFactory = PersistedFactory({ ApprovedPremisesAssessmentEntityFactory() }, approvedPremisesAssessmentRepository)
    temporaryAccommodationAssessmentEntityFactory = PersistedFactory({ TemporaryAccommodationAssessmentEntityFactory() }, temporaryAccommodationAssessmentRepository)
    assessmentClarificationNoteEntityFactory = PersistedFactory({ AssessmentClarificationNoteEntityFactory() }, assessmentClarificationNoteRepository)
    assessmentReferralHistoryUserNoteEntityFactory = PersistedFactory({ AssessmentReferralHistoryUserNoteEntityFactory() }, assessmentReferralUserNoteRepository)
    assessmentReferralHistorySystemNoteEntityFactory = PersistedFactory({ AssessmentReferralHistorySystemNoteEntityFactory() }, assessmentReferralSystemNoteRepository)
    characteristicEntityFactory = PersistedFactory({ CharacteristicEntityFactory() }, characteristicRepository)
    roomEntityFactory = PersistedFactory({ RoomEntityFactory() }, roomRepository)
    bedEntityFactory = PersistedFactory({ BedEntityFactory() }, bedRepository)
    domainEventFactory = PersistedFactory({ DomainEventEntityFactory() }, domainEventRepository)
    postCodeDistrictFactory = PersistedFactory({ PostCodeDistrictEntityFactory() }, postCodeDistrictRepository)
    placementRequestFactory = PersistedFactory({ PlacementRequestEntityFactory() }, placementRequestRepository)
    placementRequirementsFactory = PersistedFactory({ PlacementRequirementsEntityFactory() }, placementRequirementsRepository)
    bookingNotMadeFactory = PersistedFactory({ BookingNotMadeEntityFactory() }, bookingNotMadeRepository)
    probationDeliveryUnitFactory = PersistedFactory({ ProbationDeliveryUnitEntityFactory() }, probationDeliveryUnitRepository)
    applicationTeamCodeFactory = PersistedFactory({ ApplicationTeamCodeEntityFactory() }, applicationTeamCodeRepository)
    turnaroundFactory = PersistedFactory({ TurnaroundEntityFactory() }, turnaroundRepository)
    placementApplicationFactory = PersistedFactory({ PlacementApplicationEntityFactory() }, placementApplicationRepository)
    placementDateFactory = PersistedFactory({ PlacementDateEntityFactory() }, placementDateRepository)
    probationAreaProbationRegionMappingFactory = PersistedFactory({ ProbationAreaProbationRegionMappingEntityFactory() }, probationAreaProbationRegionMappingRepository)
    applicationTimelineNoteEntityFactory = PersistedFactory({ ApplicationTimelineNoteEntityFactory() }, applicationTimelineNoteRepository)
    appealEntityFactory = PersistedFactory({ AppealEntityFactory() }, appealTestRepository)
    cas1ApplicationUserDetailsEntityFactory = PersistedFactory({ Cas1ApplicationUserDetailsEntityFactory() }, cas1ApplicationUserDetailsRepository)
    referralRejectionReasonEntityFactory = PersistedFactory({ ReferralRejectionReasonEntityFactory() }, referralRejectionReasonRepository)
    cas1OutOfServiceBedEntityFactory = PersistedFactory({ Cas1OutOfServiceBedEntityFactory() }, cas1OutOfServiceBedTestRepository)
    cas1OutOfServiceBedReasonEntityFactory = PersistedFactory({ Cas1OutOfServiceBedReasonEntityFactory() }, cas1OutOfServiceBedReasonTestRepository)
    cas1OutOfServiceBedCancellationEntityFactory = PersistedFactory({ Cas1OutOfServiceBedCancellationEntityFactory() }, cas1OutOfServiceBedCancellationTestRepository)
    cas1OutOfServiceBedRevisionEntityFactory = PersistedFactory({ Cas1OutOfServiceBedRevisionEntityFactory() }, cas1OutOfServiceBedDetailsTestRepository)
    bedMoveEntityFactory = PersistedFactory({ BedMoveEntityFactory() }, bedMoveRepository)
    cas1SpaceBookingEntityFactory = PersistedFactory({ Cas1SpaceBookingEntityFactory() }, cas1SpaceBookingRepository)
    cas1CruManagementAreaEntityFactory = PersistedFactory({ Cas1CruManagementAreaEntityFactory() }, cas1CruManagementAreaRepository)
  }

  fun mockClientCredentialsJwtRequest(
    username: String? = null,
    roles: List<String> = listOf(),
    authSource: String = "none",
  ) {
    wiremockServer.stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              objectMapper.writeValueAsString(
                GetTokenResponse(
                  accessToken = jwtAuthHelper.createClientCredentialsJwt(
                    username = username,
                    roles = roles,
                    authSource = authSource,
                  ),
                  tokenType = "bearer",
                  expiresIn = Duration.ofHours(1).toSeconds().toInt(),
                  scope = "read",
                  sub = username?.uppercase() ?: "integration-test-client-id",
                  authSource = authSource,
                  jti = UUID.randomUUID().toString(),
                  iss = "http://localhost:9092/auth/issuer",
                ),
              ),
            ),
        ),
    )
  }

  fun mockStaffMembersContextApiCall(staffMember: StaffMember, qCode: String) = wiremockServer.stubFor(
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

  fun mockInmateDetailPrisonsApiCall(inmateDetail: InmateDetail) = wiremockServer.stubFor(
    WireMock.get(urlEqualTo("/api/offenders/${inmateDetail.offenderNo}"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(inmateDetail),
          ),
      ),
  )

  fun mockSuccessfulGetCallWithJsonResponse(url: String, responseBody: Any, responseStatus: Int = 200) =
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        WireMock.get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(responseStatus)
              .withBody(
                objectMapper.writeValueAsString(responseBody),
              ),
          ),
      )
    }

  fun mockSuccessfulPostCallWithJsonResponse(url: String, requestBody: StringValuePattern, responseBody: Any, responseStatus: Int = 200) =
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(requestBody)
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(responseStatus)
              .withBody(
                objectMapper.writeValueAsString(responseBody),
              ),
          ),
      )
    }

  fun mockSuccessfulGetCallWithBodyAndJsonResponse(
    url: String,
    requestBody: StringValuePattern,
    responseBody: Any,
    responseStatus: Int = 200,
    additionalConfig: MappingBuilder.() -> Unit = { },
  ) =
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        WireMock.get(urlPathEqualTo(url))
          .withRequestBody(requestBody)
          .willReturn(
            aResponse()
              .withHeader("Content-Type", "application/json")
              .withStatus(responseStatus)
              .withBody(
                objectMapper.writeValueAsString(responseBody),
              ),
          )
          .apply(additionalConfig),
      )
    }

  fun editGetStubWithBodyAndJsonResponse(
    url: String,
    uuid: UUID,
    requestBody: StringValuePattern,
    responseBody: Any,
    additionalConfig: MappingBuilder.() -> Unit = { },
  ) = wiremockServer.editStub(
    WireMock.get(urlPathEqualTo(url)).withId(uuid)
      .withRequestBody(requestBody)
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(200)
          .withBody(
            objectMapper.writeValueAsString(responseBody),
          ),
      )
      .apply(additionalConfig),
  )

  fun mockUnsuccessfulGetCall(url: String, responseStatus: Int) =
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        WireMock.get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(responseStatus),
          ),
      )
    }

  fun mockUnsuccessfulPostCall(url: String, responseStatus: Int) =
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(responseStatus),
          ),
      )
    }

  fun mockUnsuccessfulGetCallWithDelayedResponse(url: String, responseStatus: Int, delayMs: Int) =
    mockOAuth2ClientCredentialsCallIfRequired {
      wiremockServer.stubFor(
        WireMock.get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withFixedDelay(delayMs)
              .withStatus(responseStatus),
          ),
      )
    }

  fun mockOAuth2ClientCredentialsCallIfRequired(block: () -> Unit = {}) {
    if (!clientCredentialsCallMocked) {
      mockClientCredentialsJwtRequest()

      clientCredentialsCallMocked = true
    }

    block()
  }

  fun loadPreemptiveCacheForInmateDetails(nomsNumber: String) = prisonsApiClient.getInmateDetailsWithCall(nomsNumber)
}

/**
 * If an integration test extends this class instead of IntegrationTestBase,
 * the database will only be populated once and will not be 'reset' between
 * tests
 *
 * This should be used where possible as tests will run significantly faster
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("isPerClass")
abstract class InitialiseDatabasePerClassTestBase : IntegrationTestBase() {
  @BeforeAll
  fun beforeAll() {
    this.setupTests()
  }

  @AfterAll
  fun afterAll() {
    this.teardownTests()
  }
}
