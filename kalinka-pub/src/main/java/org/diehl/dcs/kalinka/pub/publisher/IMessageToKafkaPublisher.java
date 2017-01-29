package org.diehl.dcs.kalinka.pub.publisher;

import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public interface IMessageToKafkaPublisher<T, K, V> {

	void publish(T message, KafkaTemplate<K, V> kafkaTemplate);

}
