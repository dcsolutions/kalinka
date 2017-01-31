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

package org.diehl.dcs.kalinka.pub.publisher.impl;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.diehl.dcs.kalinka.pub.publisher.IJmsMessageToKafkaPublisher;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsMessageListener<K, V> implements MessageListener {

	private final IJmsMessageToKafkaPublisher<K, V> messageToKafkaPublisher;
	private final KafkaTemplate<K, V> kafkaTemplate;

	public JmsMessageListener(final IJmsMessageToKafkaPublisher<K, V> messageToKafkaPublisher, final KafkaTemplate<K, V> kafkaTemplate) {

		this.messageToKafkaPublisher = messageToKafkaPublisher;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void onMessage(final Message msg) {

		this.messageToKafkaPublisher.publish(msg, this.kafkaTemplate);
	}
}
