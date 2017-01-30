package org.diehl.dcs.kalinka.pub.context;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class MyApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(final ConfigurableApplicationContext applicationContext) {
		final ConfigurableEnvironment env = applicationContext.getEnvironment();
		applicationContext.addBeanFactoryPostProcessor(new ConnectionFactoryBeanFactoryPostProcessor(env));

	}

}
