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

package org.diehl.dcs.kalinka.pub.context;

import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.env.Environment;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import com.google.common.base.Splitter;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ConnectionFactoryBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final List<String> jmsHosts;
	private final List<String> jmsDestinations;
	private final String kafkaClientIdPrefix;

	// Found no better way to configure multiple ConnectionFactories dynamically
	// See: https://dzone.com/articles/how-create-your-own-dynamic
	public ConnectionFactoryBeanFactoryPostProcessor(final Environment springEnvironment) {

		this.jmsHosts = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(springEnvironment.getProperty("jms.hosts"));
		this.jmsDestinations = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(springEnvironment.getProperty("jms.destinations"));
		this.kafkaClientIdPrefix = springEnvironment.getProperty("kafka.client.id.prefix");
	}

	@Override
	public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// TODO Auto-generated method stub

	}

	@Override
	public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {

		this.jmsHosts.forEach(jmsHost -> {

			final BeanDefinitionBuilder connectionFactoryBuilder = BeanDefinitionBuilder.genericBeanDefinition(CachingConnectionFactory.class);
			connectionFactoryBuilder.addConstructorArgValue(new ActiveMQConnectionFactory(jmsHost));
			connectionFactoryBuilder.addPropertyValue("cacheConsumers", true);
			connectionFactoryBuilder.addPropertyValue("clientId", this.kafkaClientIdPrefix);
			registry.registerBeanDefinition("connectionFactory-" + jmsHost, connectionFactoryBuilder.getBeanDefinition());

			this.jmsDestinations.forEach(dest -> {

				final BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageListenerContainer.class);
				listenerBuilder.addPropertyReference("messageListener", "messageListener");
				listenerBuilder.addPropertyReference("connectionFactory", "connectionFactory-" + jmsHost);
				listenerBuilder.addPropertyValue("destinationName", dest);
				registry.registerBeanDefinition("messageListenerContainer-" + jmsHost + "-" + dest, listenerBuilder.getBeanDefinition());

			});
		});
	}
}
