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

public class ConnectionFactoryBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final List<String> jmsHosts;
	private final List<String> jmsDestinations;
	private final String kafkaClientIdPrefix;

	public ConnectionFactoryBeanFactoryPostProcessor(final Environment springEnvironment) {

		this.jmsHosts = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(springEnvironment.getProperty("jms.hosts"));
		this.jmsDestinations = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(springEnvironment.getProperty("jms.destinations"));
		this.kafkaClientIdPrefix = springEnvironment.getProperty("kafka.client.id.prefix");
		// TODO Auto-generated constructor stub
	}

	@Override
	public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// TODO Auto-generated method stub

	}

	@Override
	public void postProcessBeanDefinitionRegistry(final BeanDefinitionRegistry registry) throws BeansException {

		for (final String jmsHost : this.jmsHosts) {
			final BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(CachingConnectionFactory.class);
			definitionBuilder.addConstructorArgValue(new ActiveMQConnectionFactory(jmsHost));
			definitionBuilder.addPropertyValue("cacheConsumers", true);
			definitionBuilder.addPropertyValue("clientId", this.kafkaClientIdPrefix);
			registry.registerBeanDefinition("connectionFactory" + jmsHost, definitionBuilder.getBeanDefinition());
			for (final String dest : this.jmsDestinations) {
				final BeanDefinitionBuilder listenerBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMessageListenerContainer.class);
				listenerBuilder.addPropertyReference("messageListener", "messageListener");
				listenerBuilder.addPropertyReference("connectionFactory", "connectionFactory" + jmsHost);
				listenerBuilder.addPropertyValue("destinationName", dest);
				registry.registerBeanDefinition("messageListenerContainer_" + jmsHost + "_" + dest, listenerBuilder.getBeanDefinition());
			}
		}
		// TODO Auto-generated method stub

	}

}
