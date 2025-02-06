package uk.gov.justice.digital.hmpps.approvedpremisesapi.integration.setup

import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import java.nio.file.Files
import java.nio.file.Paths

fun publishMessageToTopic(client: SnsAsyncClient, domainTopicArn: String, eventType: String) {
  val message = Files.readString(Paths.get("src/test/resources/fixtures/sqs/domaineventtemplate.json")).replace("EVENT_TYPE", eventType).replace("EVENT_TYPE", eventType)
  val sendMessageRequest = PublishRequest.builder()
    .topicArn(domainTopicArn)
    .message(message)
    .messageAttributes(
      mapOf(
        "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
      ),
    )
    .build()
  client.publish(sendMessageRequest).get()
}

fun publishUnwantedMessageToTopic(client: SnsAsyncClient, domainTopicArn: String) {
  val eventType = "unwanted"
  publishMessageToTopic(client, domainTopicArn, eventType)
}

fun publishWantedMessageToTopic(client: SnsAsyncClient, domainTopicArn: String) {
  val eventType = "offender-management.allocation.changed"
  publishMessageToTopic(client, domainTopicArn, eventType)
}
