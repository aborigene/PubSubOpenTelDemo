package com.dynatracese.PubSubAPI.pubsubOTELdemo;

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

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
//import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
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

public class PubSubHelper {
    private final Tracer tracer;
    private String pubTopicName;
    private String subName;
    
    public PubSubHelper(String pubTopicName, String subName, OpenTelemetry openTelemetry){
        this.pubTopicName = pubTopicName;
        this.subName = subName;
        tracer = openTelemetry.getTracer(PubSubHelper.class.getName(), "0.1.0");
    }

    public PubSubHelper(String pubTopicName, String subName) {
        this(pubTopicName, subName, OpenTelemetry.noop());
      }

    private static TextMapGetter<HashMap<String, String>> getter =
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

	private static TextMapSetter<HashMap<String, String>> setter =
		(carrier, key, value) -> {
			System.out.println("Starting setter");
			assert carrier != null;
			// Insert the context as Header
			carrier.put(key, value);
			System.out.println("Putting "+key+": "+value);
		};

	public void publisherExample(String message) throws IOException, ExecutionException, InterruptedException {
		//TopicName topicName = TopicName.of(projectId, topicId);
		//nextSpan().name("Pub "+topicName).start();
		String topicName = this.pubTopicName;//"projects/sales-engineering-latam/topics/price-topic";
		System.out.println(GlobalOpenTelemetry.getPropagators());
		Span span = tracer.spanBuilder("Publish ".replace("projects/", "")).
            setSpanKind(SpanKind.PRODUCER)
            .setAttribute("pupsub.action", "Publish")
            .startSpan();
	

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

	public void subscribeAsyncExample() {
        String subscriptionName = "projects/sales-engineering-latam/subscriptions/price-sub";
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
