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

package org.diehl.dcs.kalinka.sub.publisher;

import java.util.Map;

import javax.jms.ConnectionFactory;

import org.diehl.dcs.kalinka.sub.cache.IBrokerCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

import com.google.common.base.Preconditions;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsTemplateProvider implements ISenderProvider<JmsTemplate> {

	private static final Logger LOG = LoggerFactory.getLogger(JmsTemplateProvider.class);

	private final Map<String, ConnectionFactory> connectionFactories;

	private final IBrokerCache brokerCache;


	public JmsTemplateProvider(final Map<String, ConnectionFactory> connectionFactories, final IBrokerCache brokerCache) {

		this.connectionFactories = Preconditions.checkNotNull(connectionFactories);
		this.brokerCache = Preconditions.checkNotNull(brokerCache);
	}

	@Override
	public JmsTemplate getSender(final String hostIdentifier) {

		final String host = this.brokerCache.get(hostIdentifier);
		if (host == null) {
			LOG.warn("HostIdentifier={} is not registered in ZK. Cannot publish", hostIdentifier);
			return null;
		}
		final ConnectionFactory connectionFactory = this.connectionFactories.get(host);
		if (connectionFactory == null) {
			LOG.warn("No connectionFactory available for host={}", host);
			return null;
		}
		final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setPubSubDomain(true);
		return jmsTemplate;
	}
}
