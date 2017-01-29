package org.diehl.dcs.kalinka.pub.publisher.impl;

import javax.jms.Message;

import org.diehl.dcs.kalinka.pub.publisher.IJmsMessageToKafkaPublisher;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsMessageToKafkaPublisher implements IJmsMessageToKafkaPublisher<String, byte[]> {

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		System.out.println(message);
	}

}
