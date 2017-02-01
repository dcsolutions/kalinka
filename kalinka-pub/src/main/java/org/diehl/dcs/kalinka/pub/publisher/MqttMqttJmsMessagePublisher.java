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
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.Message;

import org.diehl.dcs.kalinka.pub.util.JmsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import com.google.common.base.Splitter;
import com.google.common.primitives.Bytes;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttMqttJmsMessagePublisher implements IMessagePublisher<Message, String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(MqttMqttJmsMessagePublisher.class);

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		try {
			final byte[] effectivePayload = JmsUtil.getPayload((BytesMessage) message);
			final MessageContainer messageContainer = this.createMessageContainer(message.getStringProperty("JMSDestination"), effectivePayload);
			kafkaTemplate.send(messageContainer.topic, messageContainer.key, messageContainer.content);
		} catch (final Throwable t) {
			LOG.error("Exception occured", t);
		}
	}

	MessageContainer createMessageContainer(final String rawTopic, final byte[] effectivePayload) {

		final String srcTopic = rawTopic.replaceFirst("topic://", "");
		final String destTopic = "mqtt.mqtt";
		final List<String> srcTopicSplitted = Splitter.on('.').splitToList(srcTopic);
		final String srcId = srcTopicSplitted.get(1);
		final String destId = srcTopicSplitted.get(3);
		final byte[] headerBytes = ("{\"srcId\": \"" + srcId + "\"}").getBytes(StandardCharsets.UTF_8);
		final byte[] header = new byte[64];
		System.arraycopy(headerBytes, 0, header, 0, headerBytes.length);
		final byte[] payload = Bytes.concat(header, effectivePayload);
		return new MessageContainer(destTopic, destId, payload);
	}

	@Override
	public String getSourceTopicRegex() {

		// evtll besser: "\\S*(mqtt\\.\\S+\\.mqtt\\.\\S+)"
		// u. das ganze als Pattern zurück geben, dann könnte man den Regex hier nochmal anwenden.

		return "\\S*mqtt\\.\\S+\\.mqtt\\.\\S+";
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
	}
}
