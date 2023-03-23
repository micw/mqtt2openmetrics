package de.wyraz.mqtt2om.sink;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publishes data in "OpenMetrics" format, e.g. to VictoriaMetrics (https://docs.victoriametrics.com/#how-to-import-data-in-prometheus-exposition-format)
 */
@Service
public class HttpPushSink {
	
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	protected final CloseableHttpClient http = HttpClients.createDefault(); 

	@Value("${openmetrics.url}")
	protected String url;
	
	@Value("${openmetrics.username}")
	protected String username;
	
	@Value("${openmetrics.password}")
	protected String password;
	
	
	public void publish(String payload) throws IOException {
		if (StringUtils.isBlank(payload)) {
			return;
		}
		
		RequestBuilder post = RequestBuilder.post(url);
		post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));
		if (!StringUtils.isAnyBlank(username, password)) {
			post.addHeader("Authorization","Basic "+Base64.encodeBase64String((username+":"+password).getBytes()));
		}

		try (CloseableHttpResponse resp = http.execute(post.build())) {
			if (resp.getStatusLine().getStatusCode()<200 || resp.getStatusLine().getStatusCode()>299) {
				log.warn("Invalid response from openmetrics push endpoint: {}",resp.getStatusLine());
			}
		} catch (Exception ex) {
			log.warn("Unable to publish data to openmetrics push endpoint",ex);
		}

		
		
	}

}
