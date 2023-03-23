package de.wyraz.mqtt2om;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MQTT2OpenMetrics {

	protected static final Logger log = LoggerFactory.getLogger(MQTT2OpenMetrics.class);
	
	public static void main(String[] args) throws Exception {
		try {
			SpringApplication.run(MQTT2OpenMetrics.class, args);
		} catch (BeanCreationException ex) {
			Throwable th=ex;
			while (th.getCause()!=null) {
				th=th.getCause();
			}
			StringBuilder error=new StringBuilder();
			error.append("\n");
			error.append("\n");
			error.append("Application startup failed\n");
			error.append("===========================\n");
			error.append("\n");
			error.append(th.getMessage()).append("\n");
			error.append("\n");
					
			if (log.isErrorEnabled()) {
				log.error(error.toString());
			} else {
				System.err.println(error.toString());
			}
			System.exit(1);
		}
	}
	
}
