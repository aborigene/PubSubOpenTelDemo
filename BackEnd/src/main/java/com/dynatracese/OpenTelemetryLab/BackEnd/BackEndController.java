package com.dynatracese.OpenTelemetryLab.BackEnd;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

@RestController
public class BackEndController {
    private static final String template = "Response: %s";
	private static final String message_template = "This is the messge: %s!";
	private final AtomicLong counter = new AtomicLong();
	private final AtomicLong counterMessage = new AtomicLong();
    private final Tracer tracer;
    private final String topicName = System.getenv("TOPIC_NAME");
    private final String subscriptionName = System.getenv("SUBSCRIPTION_NAME");
    private PubSubHelper pubSubHelper = 
        new PubSubHelper(
            topicName, 
            subscriptionName);
    
    @Autowired
    BackEndController(OpenTelemetry openTelemetry) {
        tracer = openTelemetry.getTracer(PubController.BackEndControllerss.getName(), "0.1.0");
    }


    @CrossOrigin(origins = "*")
	@PostMapping("/storePriceChange")
	public String publish(@RequestBody PriceUpdate newPrice){
		String message = "";
        UUID uuid = UUID.randomUUID();
        try{
            //TODO: store this on a database
            message = "Successfully stored to database";
        }
        catch(Exception ex){
            message = "There was an error writing to the database, the price has not been updated.\n"+ex.getMessage();
        }
		return uuid+" - "+message;
	}

    @CrossOrigin(origins = "*")
	@GetMapping("/getUpdatedPrice")
	public String publish(@RequestParam String requestId){
		String message = "";
        try{
            //TODO: select from the database
            message = "{\"price\":\"10\"}";
        }
        catch(Exception ex){
            message = "There was an error getting the price from the database.\n"+ex.getMessage();
        }
		return uuid+" - "+message;
	}
    
}
