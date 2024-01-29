package com.dynatracese.OpenTelemetryLab.FrontEnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@SpringBootApplication
public class PubsubOteLdemoApplication {
	private static Tracer tracer;
	public static void main(String[] args) {
		String topicName = System.getenv("TOPIC_NAME");
		String subscriptionName = System.getenv("SUBSCRIPTION_NAME");
		if ((topicName == null) || (subscriptionName == null)){
			System.out.println("TOPIC_NAME or SUBSCRIPTION_NAME variable not properly set, please set those variables and restart the application.");
			System.exit(-1);
		}
		SpringApplication.run(PubsubOteLdemoApplication.class, args);
	}

	@Bean
 	public OpenTelemetry openTelemetry() {
    	return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  	}

}
