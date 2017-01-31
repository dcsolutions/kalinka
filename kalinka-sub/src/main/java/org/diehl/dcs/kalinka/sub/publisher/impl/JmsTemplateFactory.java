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
package org.diehl.dcs.kalinka.sub.publisher.impl;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jms.ConnectionFactory;

import org.diehl.dcs.kalinka.sub.publisher.IConnectionFactoryFactory;
import org.diehl.dcs.kalinka.sub.publisher.IJmsTemplateFactory;
import org.springframework.jms.core.JmsTemplate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class JmsTemplateFactory implements IJmsTemplateFactory {

	private final Map<String, ConnectionFactory> connectionFactories = Maps.newHashMap();

	private final IConnectionFactoryFactory connectionFactoryFactory;

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock(true);

	public JmsTemplateFactory(final IConnectionFactoryFactory connectionFactoryFactory) {

		this.connectionFactoryFactory = Preconditions.checkNotNull(connectionFactoryFactory);
	}

	@Override
	public JmsTemplate createJmsTemplate(final String host) {

		ConnectionFactory connectionFactory = this.connectionFactories.get(host);
		if (connectionFactory == null) {
			connectionFactory = this.connectionFactoryFactory.createConnectionFactory(host);
			this.connectionFactories.put(host, connectionFactory);
		}

		return new JmsTemplate(connectionFactory);
	}

}
