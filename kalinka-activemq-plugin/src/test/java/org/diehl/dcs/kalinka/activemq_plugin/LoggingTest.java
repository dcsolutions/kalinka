package org.diehl.dcs.kalinka.activemq_plugin;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTest {

	private static final Logger LOG = LoggerFactory.getLogger(LoggingTest.class);

	@Test
	public void testLogging() throws Exception {

		LOG.debug("test");
	}

}
