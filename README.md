### What is this lab

This is a simple example on how to use OpenTelemetry and Google PubSub managed services through java

### How to run this lab

1. Change the code adding your topic and subscription
2. Compile the code ```mvn clean package```
3. Run the producer ```java -jar PubSubDemo-0.0.1-SNAPSHOT.jar -p```, this will start a producer
4. On a separate terminal run the consumer ```java -jar PubSubDemo-0.0.1-SNAPSHOT.jar -s```, this will start a consumer that will read messages for 30s

### What is important on this lab

Context propagation. In order for a trace to be connected from the producer side to the consumer side, the context needs to be propagated. On the code you can see there are two functions ```setter``` and ```getter```, these functions are called by OpenTelemetry API to set and retrieve the context.

So on the producer side, a HashMap is created, filled with the right context parameters and added to the message. When the messages is sent to PubSub, this parameters carry the context to the consumer side.

On the consumer side, at the moment the message ir read, the ```getter``` function is used to extract the context and set the current scope.

### How this can be sent on Dynatrace? 

Produced traces can be sent in two ways:

1. Automatically with OneAgent: just install the agent and the spans will be ingested automatically
2. Through and OpenTelemetry endpoint: This can be a OpenTelemetry collector point to the Dynatrace API, or the Dynatrace API directly. Just configure the OpenTelemetry variables and the Java will send the spans to desired collector.