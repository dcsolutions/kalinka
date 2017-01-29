package org.diehl.dcs.kalinka.pub.publisher.impl;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.diehl.dcs.kalinka.pub.publisher.IJmsMessageToKafkaPublisher;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsMessageListener<K, V> implements MessageListener {

	private final IJmsMessageToKafkaPublisher<K, V> messageToKafkaPublisher;
	private final KafkaTemplate<K, V> kafkaTemplate;

	public JmsMessageListener(final IJmsMessageToKafkaPublisher<K, V> messageToKafkaPublisher, final KafkaTemplate<K, V> kafkaTemplate) {

		this.messageToKafkaPublisher = messageToKafkaPublisher;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void onMessage(final Message arg0) {

		this.messageToKafkaPublisher.publish(arg0, this.kafkaTemplate);
		// TODO Auto-generated method stub

	}
}
