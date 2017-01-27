package org.diehl.dcs.kalinka.pub;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTest {

	private static final Logger LOG = LoggerFactory.getLogger(MainTest.class);

	@Test
	public void testLoggingOk() throws Exception {

		LOG.debug("test");
	}
}
