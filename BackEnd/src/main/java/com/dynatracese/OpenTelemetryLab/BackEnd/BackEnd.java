package com.dynatracese.OpenTelemetryLab.BackEnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@SpringBootApplication
public class BackEnd {
	private static Tracer tracer;
	public static void main(String[] args) {
		SpringApplication.run(BackEnd.class, args);
	}

	@Bean
 	public OpenTelemetry openTelemetry() {
    	return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
  	}

}
