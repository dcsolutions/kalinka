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
package org.diehl.dcs.kalinka.pub.publisher.impl.example;

import java.util.regex.Pattern;

import javax.jms.BytesMessage;
import javax.jms.Message;

import org.diehl.dcs.kalinka.pub.jms.util.JmsUtil;
import org.diehl.dcs.kalinka.pub.publisher.IMessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttSparkClusterJmsMessagePublisher implements IMessagePublisher<Message, String, byte[]> {

	private static final Pattern REGEX_PATTERN = Pattern.compile("\\S+//{0,1}mqtt\\.\\S+\\.spark_cluster.pub");

	private static final Logger LOG = LoggerFactory.getLogger(MqttSparkClusterJmsMessagePublisher.class);

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		try {
			final byte[] effectivePayload = JmsUtil.getPayload((BytesMessage) message);
			kafkaTemplate.send("mqtt.spark_cluster", effectivePayload);
		} catch (final Throwable t) {
			LOG.error("Exception occured", t);
		}
	}

	@Override
	public Pattern getSourceTopicRegex() {

		return REGEX_PATTERN;
	}

}
