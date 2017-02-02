/*
 * Copyright [2017] [DCS <Info-dcs@diehl.com>] Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

package org.diehl.dcs.kalinka.pub.publisher;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.BytesMessage;
import javax.jms.Message;

import org.diehl.dcs.kalinka.pub.util.JmsUtil;
import org.diehl.dcs.kalinka.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttMqttJmsMessagePublisher implements IMessagePublisher<Message, String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(MqttMqttJmsMessagePublisher.class);

	private static final Pattern REGEX_PATTERN = Pattern.compile("(\\S+//|/){0,1}(mqtt[\\./](\\S+)[\\./]mqtt[\\./](\\S+)).pub");

	private static final String KAFKA_DEST_TOPIC = "mqtt.mqtt";

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		try {
			final byte[] effectivePayload = JmsUtil.getPayload((BytesMessage) message);
			final MessageContainer messageContainer = this.createMessageContainer(message.getStringProperty("JMSDestination"), effectivePayload);
			LOG.debug("Will send message={}", messageContainer);
			kafkaTemplate.send(messageContainer.topic, messageContainer.key, messageContainer.content);
		} catch (final Throwable t) {
			LOG.error("Exception occured", t);
		}
	}

	MessageContainer createMessageContainer(final String rawSourceTopic, final byte[] effectivePayload) {

		LOG.debug("Creating MessageContainer for rawSourceTopic={}, effectivePayload={}", rawSourceTopic,
				effectivePayload != null ? new String(effectivePayload) : null);

		final Tuple<String, String> srcDestIds = this.getSourceAndDestId(rawSourceTopic);
		final byte[] payload = this.getEnrichedPayload(effectivePayload, srcDestIds.getFirst());
		return new MessageContainer(KAFKA_DEST_TOPIC, srcDestIds.getSecond(), payload);
	}

	Tuple<String, String> getSourceAndDestId(final String rawTopic) {

		final Matcher m = this.getSourceTopicRegex().matcher(rawTopic);
		while (m.find()) {
			return new Tuple<>(m.group(3), m.group(4));
		}
		return null;
	}

	byte[] getEnrichedPayload(final byte[] effectivePayload, final String srcId) {

		final byte[] headerBytes = ("{\"srcId\": \"" + srcId + "\"}").getBytes(StandardCharsets.UTF_8);
		final byte[] header = new byte[64];
		System.arraycopy(headerBytes, 0, header, 0, headerBytes.length);
		return Bytes.concat(header, effectivePayload);
	}

	@Override
	public Pattern getSourceTopicRegex() {

		return REGEX_PATTERN;
	}

	public static class MessageContainer {

		private final String topic;
		private final String key;
		private final byte[] content;

		public MessageContainer(final String topic, final String key, final byte[] content) {
			super();
			this.topic = topic;
			this.key = key;
			this.content = content;
		}

		public String getTopic() {
			return topic;
		}

		public String getKey() {
			return key;
		}

		public byte[] getContent() {
			return content;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this.getClass()).add("topic", this.topic).add("key", this.key)
					.add("content", content != null ? new String(this.content) : null).toString();
		}
	}
}
