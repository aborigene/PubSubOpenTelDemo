package com.dynatracese.gcp.PubSubDemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.google.cloud.pubsub.v1.Subscriber;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

//import brave.Span;
//import brave.Tracer;
import brave.Tracer.SpanInScope;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.*;
import java.util.HashMap;
//import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;

@SpringBootApplication
public class PubSubDemoApplication {
	private static Tracer tracer;

	public static void main(String[] args) {
		tracer = GlobalOpenTelemetry
			.getTracerProvider()
			//.setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
			.tracerBuilder("PubSubDemo") //TODO Replace with the name of your tracer
			.build();
		// SpringApplication.run(PubSubDemoApplication.class, args);
		
    	String subscriptionName;// = "projects/sales-engineering-latam/subscriptions/price-sub";
		String topicName;// = "projects/sales-engineering-latam/topics/price-topic";
		
		//SpringApplication.run(PubSubDemoApplication.class, args);
		if (args[0].equals("-p")){
			topicName = System.getenv("TOPIC_NAME");
			if (topicName == "") {
				System.out.println("TOPIC_NAME environment variable has not been defined, please set that and restart the program.");
				System.exit(-1);
			}
			System.out.println("This app is part of the lab on PubSub demonstration. The -p is used to publish messages, this option works but is deprecated, in favor of the other app PubSubOteldemoApplication. Please generate the messages on that application.");
			while (true){
				try{
					publisherExample(topicName);
					Thread.sleep(1000);
				}
				catch(Exception ex){
					System.out.println("There as an error..."+ex.getMessage());
				}
			}
		}
		if (args[0].equals("-s")){
			subscriptionName = System.getenv("SUBSCRIPTION_NAME");
			if (subscriptionName == null) {
				System.out.println("SUBSCRIPTION_NAME environment variable has not been defined, please set that and restart the program.");
				System.exit(-1);
			}
			subscribeAsyncExample(subscriptionName);
		}
		if (args[0].equals(null)){
			System.out.println("Please inform either pub (-p) or sub (-s) during app launch.");
			System.exit(-1);
		}
	}

	public static TextMapGetter<HashMap<String, String>> getter =
    new TextMapGetter<>() {
        @Override
        public String get(HashMap<String, String> carrier, String key) {
            if (carrier.containsKey(key)) {
				System.out.println("Getting "+key+": "+carrier.get(key));
                return carrier.get(key);
            }
            return null;
		}

        @Override
        public Iterable<String> keys(HashMap<String, String> carrier) {
            return carrier.keySet();
        }
    };

	public static TextMapSetter<HashMap<String, String>> setter =
		(carrier, key, value) -> {
			System.out.println("Starting setter");
			assert carrier != null;
			// Insert the context as Header
			carrier.put(key, value);
			System.out.println("Putting "+key+": "+value);
		};

	public static void publisherExample(String topicName) throws IOException, ExecutionException, InterruptedException {
		//TopicName topicName = TopicName.of(projectId, topicId);
		//nextSpan().name("Pub "+topicName).start();
		System.out.println(GlobalOpenTelemetry.getPropagators());
		Span span = tracer.spanBuilder("Publish price-topic").setSpanKind(SpanKind.PRODUCER).startSpan();//TODO COLLECT THE RIGHT NAME FROM THE TOPIC NAME
		// Set demo span attributes using semantic naming
		span.setAttribute("pupsub.action", "Publish");

		try (Scope scope = span.makeCurrent()){
			Publisher publisher = null;
			try {
				System.out.println("Producer scope: "+scope.toString());
				// Create a publisher instance with default settings bound to the topic
				publisher = Publisher.newBuilder(topicName).build();
				HashMap<String, String> attributes = new HashMap<String, String>();
				attributes.put("test", "test value");
				System.out.println("Attributes before: "+attributes);
				GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), attributes, setter);
				//GlobalOpenTelemetry.
				System.out.println("Attributes after: " + attributes);
				System.out.println("Attributes after: " + attributes.get("traceparent"));
				

				String message = "Hello World!";
				ByteString data = ByteString.copyFromUtf8(message);
				PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).putAllAttributes(attributes)
				.build();

				// Once published, returns a server-assigned message id (unique within the topic)
				ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
				String messageId = messageIdFuture.get();
				System.out.println("Published message ID: " + messageId);
			}
			finally {
				if (publisher != null) {
					// When finished with the publisher, shutdown to free up resources.
					publisher.shutdown();
					publisher.awaitTermination(1, TimeUnit.MINUTES);
				}
			}
		} finally {
		 	span.end();
		}
  	}

	public static void subscribeAsyncExample(String subscriptionName) {
	//ProjectSubscriptionName subscriptionName =
		//  ProjectSubscriptionName.of(projectId, subscriptionId);

	// Instantiate an asynchronous message receiver.
		MessageReceiver receiver =
			(PubsubMessage message, AckReplyConsumer consumer) -> {				
				Context extractedContext = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
        			.extract(Context.current(), new HashMap<String, String>(message.getAttributesMap()), getter);

				try (Scope scope = extractedContext.makeCurrent()) {
					System.out.println("Current context: "+extractedContext.toString());
					Span span = tracer.spanBuilder("Sub price-sub").setSpanKind(SpanKind.CONSUMER).startSpan();//TODO COLLECT THE RIGHT NAME FROM THE TOPIC NAME
					// Set demo span attributes using semantic naming
					span.setAttribute("pupsub.action", "Subscribe");
					// Handle incoming message, then ack the received message.
					System.out.println("Id: " + message.getMessageId());
					System.out.println("Data: " + message.getData().toStringUtf8());
					//System.out.println("Traceparent: "+  message.getAttributesOrThrow("traceparent"));
					//.out.println("Tracestate: "+  message.getAttributesOrThrow("tracestate"));
					consumer.ack();
					span.end();
				}

			};

		Subscriber subscriber = null;
		try {
			subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
			// Start the subscriber.
			subscriber.startAsync().awaitRunning();
			System.out.printf("Listening for messages on %s:\n", subscriptionName.toString());
			// Allow the subscriber to run for 30s unless an unrecoverable error occurs.
			subscriber.awaitTerminated(300, TimeUnit.SECONDS);
		} 
		catch (TimeoutException timeoutException) {
			// Shut down the subscriber after 30s. Stop receiving messages.
			subscriber.stopAsync();
		}
	}

  	
}
