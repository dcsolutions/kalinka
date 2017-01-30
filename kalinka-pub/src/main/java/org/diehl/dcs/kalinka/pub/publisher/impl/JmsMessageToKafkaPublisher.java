/*
 * Copyright [2017] [DCS <Info-dcs@diehl.com>] Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

package org.diehl.dcs.kalinka.pub.publisher.impl;

import javax.jms.BytesMessage;
import javax.jms.Message;

import org.diehl.dcs.kalinka.pub.publisher.IJmsMessageToKafkaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsMessageToKafkaPublisher implements IJmsMessageToKafkaPublisher<String, byte[]> {

	private static final Logger LOG = LoggerFactory.getLogger(JmsMessageToKafkaPublisher.class);

	@Override
	public void publish(final Message message, final KafkaTemplate<String, byte[]> kafkaTemplate) {

		try {
			final String dest = message.getStringProperty("JMSDestination");
			final String topic = dest.replaceFirst("topic://", "");
			final BytesMessage bytesMessage = (BytesMessage) message;
			final int len = Long.valueOf(bytesMessage.getBodyLength()).intValue();
			final byte[] bytes = new byte[len];
			bytesMessage.readBytes(bytes, len);
			kafkaTemplate.send(topic, bytes);
		} catch (final Throwable t) {
			LOG.error("Exception occured", t);
		}
	}

}
