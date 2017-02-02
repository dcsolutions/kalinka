/*
Copyright [2017] [DCS <Info-dcs@diehl.com>]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.diehl.dcs.kalinka.pub.publisher;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsMessageListener<K, V> implements MessageListener {

	private static final Logger LOG = LoggerFactory.getLogger(JmsMessageListener.class);

	private final MessagePublisherProvider<Message, K, V> messagePublisherProvider;
	private final KafkaTemplate<K, V> kafkaTemplate;

	public JmsMessageListener(final MessagePublisherProvider<Message, K, V> messagePublisherProvider, final KafkaTemplate<K, V> kafkaTemplate) {

		this.messagePublisherProvider = messagePublisherProvider;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void onMessage(final Message msg) {

		try {
			final String destination = msg.getStringProperty("JMSDestination");
			final IMessagePublisher<Message, K, V> publisher = this.messagePublisherProvider.getPublisher(msg.getStringProperty("JMSDestination"));
			if (publisher == null) {
				LOG.warn("No publisher found for destination={}", destination);
				return;
			}
			publisher.publish(msg, this.kafkaTemplate);
		} catch (final Throwable t) {
			throw new RuntimeException(t);
		}
	}
}
