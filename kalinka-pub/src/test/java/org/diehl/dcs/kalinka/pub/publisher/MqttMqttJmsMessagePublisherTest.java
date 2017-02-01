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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.diehl.dcs.kalinka.pub.publisher.MqttMqttJmsMessagePublisher.MessageContainer;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttMqttJmsMessagePublisherTest {

	private final MqttMqttJmsMessagePublisher publisher = new MqttMqttJmsMessagePublisher();

	@Test
	public void testGetSourceTopicRegex() {

		final Pattern p = Pattern.compile(this.publisher.getSourceTopicRegex());
		assertThat(p.matcher("tcp://mqtt.src.mqtt.dest").matches(), is(true));
	}

	@Test
	public void testCreateMessageContainer() throws Exception {

		final byte[] effectivePayload = "this is payload".getBytes(StandardCharsets.UTF_8);
		final String rawTopic = "tcp://mqtt.src.mqtt.dest";

		final MessageContainer msgContainer = this.publisher.createMessageContainer(rawTopic, effectivePayload);

		final byte[] headerBytes = new byte[64];
		System.arraycopy(msgContainer.getContent(), 0, headerBytes, 0, 64);
		final ObjectMapper objMapper = new ObjectMapper();
		final JsonNode headerAsJson = objMapper.readTree(new String(headerBytes, StandardCharsets.UTF_8));
		assertThat(headerAsJson.get("srcId").asText(), is("src"));

		final int effectiveLength = msgContainer.getContent().length - 64;
		final byte[] effectivePayloadFromResult = new byte[effectiveLength];
		System.arraycopy(msgContainer.getContent(), 64, effectivePayloadFromResult, 0, effectiveLength);
		assertThat(new String(effectivePayloadFromResult, StandardCharsets.UTF_8), is(new String(effectivePayload, StandardCharsets.UTF_8)));
	}

}