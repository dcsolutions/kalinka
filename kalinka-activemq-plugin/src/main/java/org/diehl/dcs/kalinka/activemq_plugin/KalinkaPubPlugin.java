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

package org.diehl.dcs.kalinka.activemq_plugin;

import java.nio.charset.StandardCharsets;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.command.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michas <michas@jarmoni.org>
 * Just a test. Is it possible to "overwrite" the regular send-process???
 *
 */
public class KalinkaPubPlugin implements BrokerPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaPubPlugin.class);


	@Override
	public Broker installPlugin(final Broker broker) throws Exception {
		return new BrokerFilter(broker) {

			@Override
			public void send(final ProducerBrokerExchange producerExchange, final Message messageSend) throws Exception {

				LOG.info("message: dest={}, content={}", messageSend.getDestination().getPhysicalName(),
						new String(messageSend.getContent().getData(), StandardCharsets.UTF_8));

			}
		};

	}

}
