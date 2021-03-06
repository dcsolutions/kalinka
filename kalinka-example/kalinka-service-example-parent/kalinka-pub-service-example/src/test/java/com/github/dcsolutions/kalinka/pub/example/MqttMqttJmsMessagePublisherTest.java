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

package com.github.dcsolutions.kalinka.pub.example;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import com.github.dcsolutions.kalinka.pub.example.MqttMqttJmsMessagePublisher.MessageContainer;
import com.github.dcsolutions.kalinka.pub.example.MqttMqttJmsMessagePublisher.SrcDestId;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttMqttJmsMessagePublisherTest {

	private final MqttMqttJmsMessagePublisher publisher = new MqttMqttJmsMessagePublisher(2);

	@Test
	public void testGetSourceTopicRegex() {

		final Pattern p = this.publisher.getSourceTopicRegex();

		assertThat(p.matcher("tcp://mqtt.src.mqtt.dest.pub").matches(), is(true));
		assertThat(p.matcher("mqtt.src.mqtt.dest.pub").matches(), is(true));
		assertThat(p.matcher("tcp://mqtt/src/mqtt/dest.pub").matches(), is(true));
		assertThat(p.matcher("mqtt/src/mqtt/dest.pub").matches(), is(true));
		assertThat(p.matcher("/mqtt/src/mqtt/dest.pub").matches(), is(true));
	}

	@Test
	public void testGetEffectiveSourceTopic() throws Exception {

		final SrcDestId t1 = this.publisher.getSourceAndDestId("tcp://mqtt.src.mqtt.dest.pub");
		assertThat(t1.getSrcId(), is("src"));
		assertThat(t1.getDestId(), is("dest"));

		final SrcDestId t2 = this.publisher.getSourceAndDestId("/mqtt.src.mqtt.dest.pub");
		assertThat(t2.getSrcId(), is("src"));
		assertThat(t2.getDestId(), is("dest"));

		final SrcDestId t3 = this.publisher.getSourceAndDestId("tcp+nio://mqtt/src/mqtt/dest/pub");
		assertThat(t3.getSrcId(), is("src"));
		assertThat(t3.getDestId(), is("dest"));

		final SrcDestId t4 = this.publisher.getSourceAndDestId("/mqtt/src/mqtt/dest/pub");
		assertThat(t4.getSrcId(), is("src"));
		assertThat(t4.getDestId(), is("dest"));

		final SrcDestId t5 = this.publisher.getSourceAndDestId("mqtt.src.mqtt.dest.pub");
		assertThat(t5.getSrcId(), is("src"));
		assertThat(t5.getDestId(), is("dest"));
	}

	@Test
	public void testCreateMessageContainer() throws Exception {

		final byte[] effectivePayload = "this is payload".getBytes(StandardCharsets.UTF_8);
		final String rawTopic = "tcp://mqtt.src.mqtt.dest.pub";

		final MessageContainer msgContainer = this.publisher.createMessageContainer(rawTopic, effectivePayload);
		assertThat(msgContainer.getTopic(), is("1.mqtt.mqtt"));

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

	@Test
	public void testGetDestTopic() throws Exception {

		assertThat(this.publisher.getDestTopic("scr"), is("1.mqtt.mqtt"));
	}

}
