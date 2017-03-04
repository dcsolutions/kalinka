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

package com.github.dcsolutions.kalinka.pub.plugin;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class KalinkaPubPlugin<K, V> implements BrokerPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaPubPlugin.class);

	private final BrokerFilter brokerFilter;

	public KalinkaPubPlugin(final BrokerFilter brokerFilter) {

		this.brokerFilter = Preconditions.checkNotNull(brokerFilter);
	}

	@Override
	public Broker installPlugin(final Broker broker) throws Exception {

		LOG.info("Installing plugin...");

		return this.brokerFilter;

	}

}
