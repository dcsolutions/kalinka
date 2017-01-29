package org.diehl.dcs.kalinka.pub.publisher;

import javax.jms.Message;

import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public interface IJmsMessageToKafkaPublisher<K, V> extends IMessageToKafkaPublisher<Message, K, V> {

	@Override
	void publish(Message message, KafkaTemplate<K, V> kafkaTemplate);
}
