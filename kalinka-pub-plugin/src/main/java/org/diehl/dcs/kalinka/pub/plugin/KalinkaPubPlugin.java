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

package org.diehl.dcs.kalinka.pub.plugin;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.command.Message;
import org.diehl.dcs.kalinka.pub.publisher.IMessagePublisher;
import org.diehl.dcs.kalinka.pub.publisher.MessagePublisherProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class KalinkaPubPlugin<K, V> implements BrokerPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaPubPlugin.class);

	private final MessagePublisherProvider<Message, K, V> messagePublisherProvider;
	private final KafkaTemplate<K, V> kafkaTemplate;

	public KalinkaPubPlugin(final MessagePublisherProvider<Message, K, V> messagePublisherProvider, final KafkaTemplate<K, V> kafkaTemplate) {

		if (messagePublisherProvider == null) {
			throw new IllegalStateException("'messagePublisherProvider' must not be null");
		}

		if (kafkaTemplate == null) {
			throw new IllegalStateException("'kafkaTemplate' must not be null");
		}
		this.messagePublisherProvider = messagePublisherProvider;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public Broker installPlugin(final Broker broker) throws Exception {

		LOG.info("Installing plugin...");

		return new BrokerFilter(broker) {

			@Override
			public void send(final ProducerBrokerExchange producerExchange, final Message messageSend) throws Exception {

				try {
					final String destination = messageSend.getDestination().getPhysicalName();
					final IMessagePublisher<Message, K, V> publisher = messagePublisherProvider.getPublisher(destination);
					if (publisher == null) {
						LOG.debug("No kakfa-publisher found for destination={}. Will forward message.", destination);
						super.send(producerExchange, messageSend);
					} else {
						publisher.publish(messageSend, kafkaTemplate);
					}
				} catch (final Throwable t) {
					LOG.error("Exception occured", t);
					throw t;
				}
			}
		};

	}

}
