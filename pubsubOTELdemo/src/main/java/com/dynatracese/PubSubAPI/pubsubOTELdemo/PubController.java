package com.dynatracese.PubSubAPI.pubsubOTELdemo;

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
public class PubController {
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
    PubController(OpenTelemetry openTelemetry) {
        tracer = openTelemetry.getTracer(PubController.class.getName(), "0.1.0");
    }


    @CrossOrigin(origins = "*")
	@GetMapping("/pubSomething")
	public String publish(@RequestParam(value = "message", defaultValue = "Hello World!") String message){
		UUID uuid = UUID.randomUUID();
        try{
            pubSubHelper.publisherExample(message);
        }
        catch(Exception ex){
            message = "There was an error publishing, the message '"+message+"' has not been sent.\n"+ex.getMessage();
        }
		System.out.println(String.format(template, message));
		return uuid+" - "+message;
	}
    
}
