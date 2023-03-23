package de.wyraz.mqtt2om.source;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import de.wyraz.mqtt2om.metrics.MetricExtractor;
import de.wyraz.mqtt2om.sink.HttpPushSink;

@Service
public class MqttSource implements CommandLineRunner {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	@Value("${mqtt.host}")
	protected String mqttHost;
	
	@Value("${mqtt.port:1883}")
	protected int mqttPort;
	
	@Value("${mqtt.username:}")
	protected String mqttUsername;

	@Value("${mqtt.password:}")
	protected String mqttPassword;

	@Value("${mqtt.topics:}")
	protected String mqttTopics;
	
	protected IMqttClient mqttClient;
	
	@Autowired
	protected MetricExtractor extractor;
	
	@Autowired
	protected HttpPushSink sink;
	
	@Override
	public void run(String... args) throws Exception {
		mqttClient = new MqttClient("tcp://"+mqttHost+":"+mqttPort,UUID.randomUUID().toString(), new MemoryPersistence());
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(false);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		if (!StringUtils.isAnyBlank(mqttUsername, mqttPassword)) {
			options.setUserName(mqttUsername);
			options.setPassword(mqttPassword.toCharArray());
		}
		mqttClient.setCallback(new MqttCallback() {
			
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				handleMessage(topic,message);
			}
			
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
			}
			
			@Override
			public void connectionLost(Throwable cause) {
				System.err.println("Disconnected");
				System.exit(0);
			}
		});
		
		Collection<String> topicList=new ArrayList<>();
		
		for (String topic: mqttTopics.split("[\\r\\n\\s]+")) {
			topic=topic.trim();
			if (topic.length()>0) {
				topicList.add(topic);
			}
		}
		
		if (topicList.isEmpty()) { // no topics defined - subscribe to topics defined in extractors
			topicList=extractor.getRequiredTopics();
		}

		if (topicList.isEmpty()) { // no topics defined - subscribe to topics defined in extractors
			log.warn("No topics to subscribe");
			return;
		}
		
		mqttClient.connect(options);
		log.info("Connected to MQTT");
		
		log.info("Subscribing to {}",topicList);
		mqttClient.subscribe(topicList.toArray(new String[0]));
		
	}

	protected void handleMessage(String topic, MqttMessage message) throws Exception {
		
		String metrics=extractor.extractMetrics(topic, new String(message.getPayload(),StandardCharsets.UTF_8));
		
		if (StringUtils.isBlank(metrics)) {
			return;
		}
		
		log.debug("Extracted: {}",metrics);
		
		try {
			sink.publish(metrics);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
}
