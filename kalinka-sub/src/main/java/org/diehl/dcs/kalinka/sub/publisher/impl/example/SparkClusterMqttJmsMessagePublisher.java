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
package org.diehl.dcs.kalinka.sub.publisher.impl.example;

import java.util.regex.Pattern;

import javax.jms.BytesMessage;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.diehl.dcs.kalinka.sub.publisher.IMessagePublisher;
import org.diehl.dcs.kalinka.sub.sender.ISenderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

/**
 * @author michas <michas@jarmoni.org>
 *
 * This is an example user-implementation. It matches the topic names and topic-mapping-semantics described in README.md
 *
 */
public class SparkClusterMqttJmsMessagePublisher implements IMessagePublisher<JmsTemplate, String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(SparkClusterMqttJmsMessagePublisher.class);

	private static final Pattern REGEX_PATTERN = Pattern.compile("\\d+\\.sparkcluster.mqtt");

	@Override
	public void publish(final ConsumerRecord<String, byte[]> message, final ISenderProvider<JmsTemplate> senderProvider) {

		try {
			final String destId = message.key();
			final String topic = "sparkcluster" + ".mqtt." + destId + ".sub";

			final JmsTemplate sender = senderProvider.getSender(destId);
			if (sender == null) {
				LOG.warn("Cannot send, no sender available for destId={}", destId);
				return;
			}
			sender.send(topic, (MessageCreator) session -> {
				final BytesMessage byteMessage = session.createBytesMessage();
				try {
					byteMessage.writeBytes(message.value());
					return byteMessage;
				} catch (final Throwable t) {
					throw new RuntimeException(t);
				}
			});
		} catch (final Throwable t) {
			LOG.error("Exception while sending", t);
		}
	}

	@Override
	public Pattern getSourceTopicRegex() {

		return REGEX_PATTERN;
	}

}
