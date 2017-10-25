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

package com.github.dcsolutions.kalinka.sub.sender.jms;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

import com.github.dcsolutions.kalinka.cluster.IHostResolver;
import com.github.dcsolutions.kalinka.sub.sender.AbstractSenderProvider;
import com.google.common.base.Preconditions;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsSenderProvider extends AbstractSenderProvider<JmsTemplate> {

	private static final Logger LOG = LoggerFactory.getLogger(JmsSenderProvider.class);

	private final Map<String, ConnectionFactory> connectionFactories;


	public JmsSenderProvider(final Map<String, ConnectionFactory> connectionFactories, final IHostResolver hostResolver) {

		super(hostResolver);
		this.connectionFactories = Preconditions.checkNotNull(connectionFactories);
	}

	@Override
	public Set<JmsTemplate> getSendersForHosts(final Set<String> hosts) {

		return hosts.stream().filter(host -> {
			if (this.connectionFactories.get(host) == null) {
				LOG.warn("No connectionFactory available for host={}", host);
				return false;
			}
			return true;
		}).map(h -> {
			final JmsTemplate jmsTemplate = new JmsTemplate(connectionFactories.get(h));
			jmsTemplate.setPubSubDomain(true);
			return jmsTemplate;
		}).collect(Collectors.toSet());
	}
}
