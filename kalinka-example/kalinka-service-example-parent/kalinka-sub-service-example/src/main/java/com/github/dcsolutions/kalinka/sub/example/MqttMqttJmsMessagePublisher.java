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
package com.github.dcsolutions.kalinka.sub.example;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

import javax.jms.BytesMessage;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dcsolutions.kalinka.sub.publisher.IMessagePublisher;
import com.github.dcsolutions.kalinka.sub.sender.ISenderProvider;

/**
 * @author michas <michas@jarmoni.org>
 *
 * This is an example user-implementation. It matches the topic names and topic-mapping-semantics described in README.md
 *
 */
public class MqttMqttJmsMessagePublisher implements IMessagePublisher<JmsTemplate, String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(MqttMqttJmsMessagePublisher.class);

	private static final Pattern REGEX_PATTERN = Pattern.compile("\\d+\\.mqtt.mqtt");

	@Override
	public void publish(final ConsumerRecord<String, byte[]> message, final ISenderProvider<JmsTemplate> senderProvider) {

		try {
			final String destId = message.key();
			final byte[] headerBytes = new byte[64];
			System.arraycopy(message.value(), 0, headerBytes, 0, 64);
			final ObjectMapper objMapper = new ObjectMapper();
			final JsonNode headerAsJson = objMapper.readTree(new String(headerBytes, StandardCharsets.UTF_8));
			final String srcId = headerAsJson.get("srcId").asText();
			final String topic = "mqtt." + srcId + ".mqtt." + destId + ".sub";
			final int effectiveLength = message.value().length - 64;
			final byte[] effectivePayloadFromResult = new byte[effectiveLength];
			System.arraycopy(message.value(), 64, effectivePayloadFromResult, 0, effectiveLength);

			final Set<JmsTemplate> senders = senderProvider.getSenders(destId);
			if (senders.isEmpty()) {
				LOG.warn("Cannot send, no sender available for destId={}", destId);
				return;
			}
			senders.forEach(sender -> {
				try {
					sender.send(topic, (MessageCreator) session -> {
						final BytesMessage byteMessage = session.createBytesMessage();
						try {
							byteMessage.writeBytes(effectivePayloadFromResult);
							return byteMessage;
						} catch (final Throwable t) {
							throw new RuntimeException("Exception during sending", t);
						}
					});
				} catch (final Throwable t) {
					LOG.error("Exception while sending", t);
				}
			});
		} catch (final Throwable t) {
			LOG.error("Unexpected exception", t);
		}
	}

	@Override
	public Pattern getSourceTopicRegex() {

		return REGEX_PATTERN;
	}

}
