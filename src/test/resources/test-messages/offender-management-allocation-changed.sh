#!/usr/bin/env bash
aws --endpoint-url=http://localhost:4566 sns publish \
    --topic-arn arn:aws:sns:eu-west-2:000000000000:domainevents \
    --message-attributes '{"eventType" : { "DataType":"String", "StringValue":"offender-management.allocation.changed"}}' \
    --message '{"type":"offender-management.allocation.changed","id":"1","contents":"allocation_message_contents"}'