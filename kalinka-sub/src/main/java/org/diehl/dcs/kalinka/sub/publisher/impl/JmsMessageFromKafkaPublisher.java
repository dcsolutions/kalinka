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
package org.diehl.dcs.kalinka.sub.publisher.impl;

import javax.jms.BytesMessage;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.diehl.dcs.kalinka.sub.publisher.IJmsMessageFromKafkaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsMessageFromKafkaPublisher implements IJmsMessageFromKafkaPublisher<String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(JmsMessageFromKafkaPublisher.class);

	@Override
	public void publish(final ConsumerRecord<String, byte[]> message, final JmsTemplate publisher) {

		final String topic = message.topic();
		publisher.send(topic, (MessageCreator) session -> {
			final BytesMessage byteMessage = session.createBytesMessage();
			try {
				byteMessage.writeBytes(message.value());
				return byteMessage;
			} catch (final Throwable t) {
				throw new RuntimeException(t);
			}
		});
	}

	@Override
	public String getHostIdentifier(final ConsumerRecord<String, byte[]> message) {

		final ObjectMapper om = new ObjectMapper();
		try {
			return om.readTree(message.value()).get("to").asText();
		} catch (final Throwable t) {
			LOG.error("Exception while parsing hostIdentifier", t);
		}
		return null;
	}

}
