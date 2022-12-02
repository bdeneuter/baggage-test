package com.example.demo;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class Kafka {

    private final Tracer tracer;
    private final Propagator propagator;
    private final List<KafkaRecord> topic = new ArrayList<>();
    private int offset = 0;

    public Kafka(Tracer tracer, Propagator propagator) {
        this.tracer = tracer;
        this.propagator = propagator;
    }


    public void publish(String message) {
        var record = new KafkaRecord(message, new HashMap<>());
        propagator.inject(tracer.currentTraceContext().context(), record.headers(), Map::put);
        topic.add(record);
    }

    public void consume(Consumer<String> eventHandler) {
        if (topic.size() > offset) {
            var record = topic.get(offset++);
            var span = propagator.extract(record.headers(), Map::get).start();
            try(var spanInContext = tracer.withSpan(span)) {
                eventHandler.accept(record.message());
            }
        }
    }

}
