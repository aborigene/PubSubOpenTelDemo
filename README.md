### What is this lab

This is a simple application that will be used to demonstrate the usage of OpenTelemetry + Dynatrace. The focus of this lab is to cover some important concepts on OpenTel, and provide examples on how to use it with Dynatrace.
The application on this lab is composed of 3 microservices:
1. FrontEnd publisher
2. Middleware consumer
3. Backend database

These are Java microservices that talk to each other either through HTTP or through a PubSub GCP Topic. This is important so we can cover
1. HTTP autoinstrumentation using OpenTel
2. Context propagation on GPC PubSub.

This lab will also be composed of 3 scenarios:
1. OneAgent + OpentTel:
    1. FrontEnd instrumented with OneAgent, OpenTel just for PubSub integration
    2. Middleware instrumented with OneAgent, OpenTel just for PubSub integration
    3. Backend instrumented purely with OneAgent
2. OneAgent + Pure OpenTel
    1. FrontEnd instrumented with OneAgent, OpenTel just for PubSub integration
    2. Middleware instrumented with just OpenTel, no OneAgent for trace recollection. OpenTel used for PubSub integration.
    3. Backend instrumented purely with OneAgent
3. Pure OpenTel
    1. FrontEnd entirely instrumented with OpenTel
    2. Middleware entirely instrumented with OpenTel
    3. Backend entirely instrumented with OpenTel
    
When this labs refers to instrumentaion as "pure OpenTel" it means that the traces will be sent to the Dynatrace OTLP endpoint, either directly or through the usage of an OpenTelemetry collector.

### Pre Requisites

1. A Dynatrace tenant, if you don't have one you can start your free trial [here](https://www.dynatrace.com/trial).
2. A OTLP ingest token to the tenant.
    1. Go to Accesst tokens -> Generate new token
    2. Define a name for your token and choose the scope to be "Ingest logs", "Ingest metrics" and "Ingest OpenTelemetry traces"
3. GCP ADC configuration so the applications can reach your GCP project, [here](https://cloud.google.com/docs/authentication/application-default-credentials)
4. PubSub topic an subscription

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
 