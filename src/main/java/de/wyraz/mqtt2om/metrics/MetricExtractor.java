package de.wyraz.mqtt2om.metrics;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import de.wyraz.mqtt2om.metrics.OpenmetricsBuilder.MetricBuilder;

@Service
public class MetricExtractor {
	
	protected List<Config> configs=new ArrayList<>();
	
	@ConfigurationProperties("extract")
	@Validated
	@Bean
	public List<Config> getConfigs() {
		return configs;
	}
	
	public Collection<String> getRequiredTopics() {
		Set<String> topicList=new TreeSet<>();
		for (Config c: configs) {
			topicList.addAll(c.topics);
		}
		return topicList;
	}
	
	
	public String extractMetrics(String topic, String payload) {
		for (Config config: configs) {
			if (config.matches(topic)) {
				return config.process(topic, payload);
			}
		}
		return null;
	}
	
	public static class MetricConfig {
		@NotBlank
		protected String name;
		@NotBlank
		protected String value;
		
		protected String time;

		protected LinkedHashMap<String, String> tags;
		
		public void setName(String name) {
			this.name = name;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public void setTags(LinkedHashMap<String, String> tags) {
			this.tags = tags;
		}
		public void setTime(String time) {
			this.time = time;
		}
	}

	public static class Config {
		
		@NotEmpty
		protected List<String> topics;
		
		protected List<Pattern> topicPatterns;
		
		@NotEmpty
		protected List<MetricConfig> metrics;
		
		public void setTopics(List<String> topics) {
			this.topics=topics;
			this.topicPatterns=new ArrayList<>();
			for (String topic: topics) {
				this.topicPatterns.add(Pattern.compile(
						topic
							.replaceAll("\\$", "\\\\$")
							.replaceAll("\\+", "[^/]+")
							.replaceAll("/\\#$", "\\$|/.+")
							));
			}
		}
		
		public void setMetrics(List<MetricConfig> metrics) {
			this.metrics = metrics;
		}
		
		protected boolean matches(String topic) {
			for (Pattern p: topicPatterns) {
				if (p.matcher(topic).matches()) {
					return true;
				}
			}
			return false;
		}
		
		protected String process(String topic, String payload) {
			return new ConfigProcessor(this,topic,payload).process();
		}
	}
	
	public static class ConfigProcessor {
		
		protected final Config config;
		protected final String topic;
		protected final String payload;
		
		protected String[] topicParts;
		protected DocumentContext json;
		
		protected ConfigProcessor(Config config, String topic, String payload) {
			this.config=config;
			this.topic=topic;
			this.payload=payload;
		}
		
		protected static Pattern placeholderPattern=Pattern.compile("\\$\\(([^\\}]+)\\)"); 
		protected String replacePlaceholders(String template) {
			if (template==null) {
				return null;
			}
	        StringBuilder result = new StringBuilder();
	        Matcher m = placeholderPattern.matcher(template);
	        while (m.find()) {
	            m.appendReplacement(result, "");
	            result.append(replacePlaceholder(m.group(1), m.group()));
	        }
	        m.appendTail(result);

	        return result.toString();
	    }

		protected String replacePlaceholder(String placeholder, String originalPlaceholder) {
			
			if (placeholder.equals("VALUE")) {
				return payload;
			}
			
			if (placeholder.startsWith("TOPIC:")) {
				try {
					int pos=Integer.parseInt(placeholder.substring(6))-1;
					if (topicParts==null) {
						topicParts=topic.split("/");
					}
					if (pos>=0 && pos<topicParts.length) {
						return topicParts[pos];
					}
				} catch (RuntimeException ex) {
					ex.printStackTrace();
					// TODO: log
				}
			}
			
			if (placeholder.startsWith("JSON:")) {
				try {
					if (json==null) {
						json=JsonPath.parse(payload);
					}
					Object result=json.read(placeholder.substring(5));
					return result==null?null:String.valueOf(result);
				} catch (RuntimeException ex) {
					ex.printStackTrace();
					// TODO: log
				}
			}
			
			return originalPlaceholder;
		}
		
		protected Number parseNumber(String value) {
			if (value==null) return null;
			try {
				if (value.contains(".")) {
					return Double.parseDouble(value);
				} else {
					return Long.parseLong(value);
				}
			} catch (RuntimeException ex) {
				ex.printStackTrace();
				// TODO: log
			}
			return null;
		}
		protected ZonedDateTime parseTimestamp(String value) {
			if (value!=null) {
				try {
					return ZonedDateTime.parse(value);
				} catch (Exception ex) {
					//ignored
				}
				try {
					return LocalDateTime.parse(value).atZone(ZoneId.systemDefault());
				} catch (Exception ex) {
					//ignored
				}
			}
			
			return ZonedDateTime.now();
		}
		
		protected String process() {
			OpenmetricsBuilder builder=new OpenmetricsBuilder();
			
			for (MetricConfig metric: config.metrics) {
				MetricBuilder mb=builder.metric(replacePlaceholders(metric.name));
				
				if (metric.tags!=null) {
					for (Entry<String,String> e: metric.tags.entrySet()) {
						String key=replacePlaceholders(e.getKey());
						String value=replacePlaceholders(e.getValue());
						mb.tag(key, value);
					}
				}
				
				mb.timestamp(parseTimestamp(replacePlaceholders(metric.time)));
				
				
				mb.value(parseNumber(replacePlaceholders(metric.value)));
			}
			
			
			return builder.build();
		}
	}
	
}
