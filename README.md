### What is this lab

This is a simple example on how to use OpenTelemetry and Google PubSub managed services through java.
This lab is composed of two applications:
1. Spring Controller that receives a GET and publishes a message to a TOPIC
2. A command line app that subscribes to a SUBSCRIPTION, and process all messages available.

### How to build this lab

1. Compile the command line app
    1. ```cd PubSubCommandLineApp```
    2. ```./mvnw clean package -DskipTests```
    3. Edit the file `start.sh`, to add the values of your TOPIC_NAME and SUBSCRIPTION_NAME variables
2. Compile the controller app
    1. ```cd pubsubOTELdemo```
    2. ```./mvn2 clean package -DskipTests```
    3. Edit the file `start.sh`, to add the values of your TOPIC_NAME and SUBSCRIPTION_NAME variables

### How to run this lab

1. Open two separate terminals
2. On the first one
    1. ```cd PubSubCommandLineApp```
    2. ```./start.sh```
    3. This commando will run for 5 minutes receiving messages, when this finishes you can restart to continue consuming messages
3. On the second one
    1. ```cd pubsubOTELdemo```
    2. ```./start.sh```
4. Open a third terminal
    1. ```curl 127.0.0.1:8080//pubSomething?message=MySuperTest```
    2. This will produce a message, repeat this step as many times as required to produce more messages

### What is important on this lab

Context propagation. In order for a trace to be connected from the producer side to the consumer side, the context needs to be propagated. On the code you can see there are two functions ```setter``` and ```getter```, these functions are called by OpenTelemetry API to set and retrieve the context.

So on the producer side, a HashMap is created, filled with the right context parameters and added to the message. When the messages is sent to PubSub, this parameters carry the context to the consumer side.

On the consumer side, at the moment the message ir read, the ```getter``` function is used to extract the context and set the current scope.

### How this can be sent on Dynatrace? 

Produced traces can be sent in two ways:

1. Automatically with OneAgent (prefered option used on this lab)
    1. Just install the agent and the spans will be ingested automatically
    2. Add configuration from trace propagation: Go to Settings -> Server Side Monitoring -> Span Context Propagation
    3. On this screen create a rule with the following values: Rule name: any name to identify this rule, Rule action: Propagate
    4. Add a matcher with the following values: Source: Span kind, Comparison type: equals, Value: Producer
2. Through Dynatrace OTLP API
    1. Set the environment variables below on the `start.sh` files
    2. OTEL_EXPORTER_OTLP_ENDPOINT=https://{your-environment-id}.live.dynatrace.com/api/v2/otlp
    3. OTEL_EXPORTER_OTLP_HEADERS="Authorization=Api-Key {Dynatrace token with OpenTel ingestion scope}"
3. Through an OpenTelemetry collector
    1. In this case the collector will receive the traces and forward this to Dynatrace
    2. Create an OpenTelemetry collector anywhere these applications can reach
    3. Add the following configuration to the collector exporter section:
    ```
    exporters:
      otlphttp:
        endpoint: 'https://{your-environment-id}.live.dynatrace.com/api/v2/otlp'
        headers: { 'Authorization': 'Api-Key {Dynatrace token with OpenTel ingestion scope}' }
    ```
    4. This is YAML block so pay attention to the identation
 