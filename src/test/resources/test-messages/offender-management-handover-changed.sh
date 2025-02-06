#!/usr/bin/env bash
aws --endpoint-url=http://localhost:4566 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents \
    --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"offender-management.handover.changed"}}' \
    --message '{"type":"offender-management.handover.changed","id":"2","contents":"handover_message_contents"}'