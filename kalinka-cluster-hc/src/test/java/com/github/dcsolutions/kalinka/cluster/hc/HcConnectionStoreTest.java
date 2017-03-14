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

package com.github.dcsolutions.kalinka.cluster.hc;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class HcConnectionStoreTest {

	private static final Logger LOG = LoggerFactory.getLogger(HcConnectionStoreTest.class);

	private static final String GROUP_NAME = "abc-group";
	private static final String MAP_NAME = "xyz-map";

	private HazelcastInstance hzInstance;
	private HcConnectionStore hcConnectionStore;

	@Before
	public void setUp() throws Exception {

		final Config config = new Config();
		config.getGroupConfig().setName(GROUP_NAME);
		config.setProperty("hazelcast.logging.type", "slf4j");
		this.hzInstance = Hazelcast.newHazelcastInstance(config);
		this.hcConnectionStore = new HcConnectionStore("127.0.0.1:5701", GROUP_NAME, MAP_NAME, "192.168.90.1");
	}

	@After
	public void tearDown() throws Exception {

		if (this.hzInstance != null) {
			this.hzInstance.shutdown();
		}
		if (this.hcConnectionStore != null) {
			this.hcConnectionStore.shutdown();
		}
	}

	@Test(timeout = 500)
	public void testUpsertNotPresent() throws Exception {

		this.hcConnectionStore.upsertConnection("ABC123");
		final Future<String> future = this.getFutureResult("ABC123", false);
		assertThat(future.get(), is("192.168.90.1"));
	}

	@Test(timeout = 500)
	public void testUpsertAlreadyPresent() throws Exception {

		this.hzInstance.getMap(MAP_NAME).put("ABC123", "192.168.90.2");
		final Future<String> future = this.getFutureResult("ABC123", false);
		assertThat(future.get(), is("192.168.90.2"));

		this.hcConnectionStore.upsertConnection("ABC123");
		final Future<String> future2 = this.getFutureResult("ABC123", false);
		assertThat(future2.get(), is("192.168.90.1"));
	}

	@Test(timeout = 500)
	public void testDeleteNotPresent() throws Exception {

		this.hcConnectionStore.removeConnection("ABC123");
		final Future<String> future = this.getFutureResult("ABC123", true);
		assertThat(future.get(), is(nullValue()));
	}

	@Test(timeout = 500)
	public void testDeleteAlreadyPresent() throws Exception {

		this.hzInstance.getMap(MAP_NAME).put("ABC123", "192.168.90.1");
		final Future<String> future = this.getFutureResult("ABC123", false);
		assertThat(future.get(), is("192.168.90.1"));

		this.hcConnectionStore.removeConnection("ABC123");
		final Future<String> future2 = this.getFutureResult("ABC123", true);
		assertThat(future2.get(), is(nullValue()));
	}

	private Future<String> getFutureResult(final String key, final boolean expectNull) {

		final Map<Object, Object> map = this.hzInstance.getMap(MAP_NAME);
		return Executors.newSingleThreadExecutor().submit(() -> {
			String host = null;
			while (true) {
				host = (String) map.get(key);
				LOG.info("Result={}", host);
				if ((!expectNull && host != null) || (expectNull && host == null)) {
					return host;
				}
				try {
					Thread.sleep(10L);
				} catch (final InterruptedException iex) {
					LOG.warn("Interrupted...");
				}
			}
		});
	}
}
