package com.example.demo;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureObservability
class DemoApplicationTests {

	public static final String KEY_1 = "key1";
	public static final String VALUE_1 = "value1";
	@Autowired private Tracer tracer;
	@Autowired private Propagator propagator;

	@Test
	void canSetAndGetBaggage() {
		// GIVEN
		var span = tracer.nextSpan().start();
		try(var spanInScope = tracer.withSpan(span)) {
			// WHEN
			tracer.getBaggage(KEY_1).set(VALUE_1);

			// THEN
			assertThat(tracer.getBaggage(KEY_1).get()).isEqualTo(VALUE_1);
		}
	}

	@Test
	void injectAndExtractKeepsTheBaggage() {
		// GIVEN
		var carrier = new HashMap<String, String>();

		var span = tracer.nextSpan().start();
		try(var spanInScope = tracer.withSpan(span)) {
			tracer.createBaggage(KEY_1, VALUE_1);

			// WHEN
			propagator.inject(tracer.currentTraceContext().context(), carrier, HashMap::put);

			// THEN
			assertThat(carrier.get(KEY_1)).isEqualTo(VALUE_1);
		}

		// WHEN
		var extractedSpan = propagator.extract(carrier, HashMap::get).start();

		// THEN
		try(var spanInScope = tracer.withSpan(extractedSpan)) {
			assertThat(tracer.getBaggage(KEY_1).get(extractedSpan.context())).isEqualTo(VALUE_1);
			try(var baggageInScope = tracer.getBaggage(KEY_1).makeCurrent()) {
				assertThat(baggageInScope.get()).isEqualTo(VALUE_1);
			}
		}
	}

}
