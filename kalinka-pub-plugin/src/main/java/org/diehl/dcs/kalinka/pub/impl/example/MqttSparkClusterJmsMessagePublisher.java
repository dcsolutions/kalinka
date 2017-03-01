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
package org.diehl.dcs.kalinka.pub.impl.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.activemq.command.Message;
import org.diehl.dcs.kalinka.pub.publisher.IMessagePublisher;
import org.diehl.dcs.kalinka.pub.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttSparkClusterJmsMessagePublisher implements IMessagePublisher<Message, String, byte[]> {

	// TODO: Make this configurable
	private static final int NUM_LOGICAL_PARTITIONS = 2;

	private static final Pattern REGEX_PATTERN = Pattern.compile("mqtt\\.(\\S+)\\.sparkcluster.pub");

	private static final Logger LOG = LoggerFactory.getLogger(MqttSparkClusterJmsMessagePublisher.class);

	private static final String KAFKA_DEST_TOPIC = "{p}.mqtt.sparkcluster";

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		try {
			final byte[] effectivePayload = message.getContent().getData();
			final String sourceTopic = message.getDestination().getPhysicalName();
			final String destTopic = this.getDestTopic(sourceTopic);
			kafkaTemplate.send(destTopic, effectivePayload);
		} catch (final Throwable t) {
			LOG.error("Exception occured", t);
		}
	}

	String getDestTopic(final String sourceTopic) {

		final int logicalPartition = HashUtil.hashKey(this.getSourceId(sourceTopic), NUM_LOGICAL_PARTITIONS);
		final String destTopic = KAFKA_DEST_TOPIC.replace("{p}", String.valueOf(logicalPartition));
		return destTopic;
	}

	String getSourceId(final String sourceTopic) {

		final Matcher m = REGEX_PATTERN.matcher(sourceTopic);
		m.find();
		return m.group(1);
	}

	@Override
	public Pattern getSourceTopicRegex() {

		return REGEX_PATTERN;
	}

}
