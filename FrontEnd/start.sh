#! /bin/bash
set OTEL_METRICS_EXPORTER=none
set OTEL_TRACES_EXPORTER=none
set OTEL_LOGS_EXPORTER=none
set TOPIC_NAME=<put your topic name here>
set SUBSCRIPTION_NAME=<put your subscription name here>
java -Dotel.java.global-autoconfigure.enabled=true -jar target/pubsubOTELdemo-0.0.1-SNAPSHOT.jar