package de.wyraz.mqtt2om.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = { MetricExtractor.class, },
	properties = "spring.profiles.active:test")
@EnableConfigurationProperties
public class MetricExtractorTests {
	
	@Autowired
	protected MetricExtractor metricExtractor;

	@Test
	public void testExtractNoMetric() {
		assertThat(metricExtractor.extractMetrics("something/somewhere", "4"))
			.isNull();
	}
	
	@Test
	public void testExtractSimpleMetric() {
		assertThat(metricExtractor.extractMetrics("grid/myMeter/powerTotal", "123.45"))
			.matches("powerTotal \\{meter=\"myMeter\"\\} 123\\.45 \\d+");
	}

	@Test
	public void testExtractJsonMetrics() {
		
		String json="{\"Time\":\"2023-03-22T23:20:54\",\"ENERGY\":{\"TotalStartTime\":\"2020-02-02T19:24:42\",\"Total\":11.302,\"Yesterday\":0.878,\"Today\":5.809,\"Period\":0,\"Power\":0,\"ApparentPower\":0,\"ReactivePower\":0,\"Factor\":0.00,\"Voltage\":229,\"Current\":0.000}}";
		
		assertThat(metricExtractor.extractMetrics("steckdose/tasmota1/SENSOR", json))
			.isNotNull()
			.isEqualTo("totalEnergy {steckdose=\"tasmota1\"} 11.302 1679523654\n"
					+ "power {steckdose=\"tasmota1\"} 0 1679523654\n"
					+ "voltage {steckdose=\"tasmota1\"} 229 1679523654");
		
	}
	
}
