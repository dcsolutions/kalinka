/*
 * Copyright [2017] [DCS <Info-dcs@diehl.com>]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dcsolutions.kalinka.cluster.zk;

import static org.junit.Assert.assertEquals;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.apache.curator.test.TestingServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ZkConnectionStoreTest {

	@Rule
	public ExpectedException ee = ExpectedException.none();

	private static final String ZK_CHROOT_PATH = "/apachemq/connections";

	private TestingServer zkServer;
	private ZkClient zkClient;

	@Before
	public void setUp() throws Exception {

		this.zkServer = new TestingServer(2181);
		this.zkClient = new ZkClient(this.zkServer.getConnectString());
		// this must be done after Zookeeper install in "real-world-setup"
		this.zkClient.createPersistent(ZK_CHROOT_PATH, true);
	}

	@Test
	public void testUpsertZkNode() throws Exception {

		final ZkConnectionStore store = new ZkConnectionStore(this.zkServer.getConnectString() + ZK_CHROOT_PATH, "mysuperhost");
		store.upsertZkNode("123");

		assertEquals("mysuperhost", this.zkClient.readData(ZK_CHROOT_PATH + "/123").toString());

		final ZkConnectionStore store2 = new ZkConnectionStore(this.zkServer.getConnectString() + ZK_CHROOT_PATH, "myevenbetterhost");
		store2.upsertZkNode("123");

		assertEquals("myevenbetterhost", this.zkClient.readData(ZK_CHROOT_PATH + "/123").toString());

	}

	@Test
	public void testDeleteZkNode() throws Exception {

		final ZkConnectionStore store = new ZkConnectionStore(this.zkServer.getConnectString(), "mysuperhost");
		// no exception is thrown when trying to delete a not existing node
		store.deleteZkNode("123");
		store.upsertZkNode("123");

		store.deleteZkNode("123");
		this.ee.expect(ZkNoNodeException.class);
		this.zkClient.readData("/123");
	}


	@After
	public void tearDown() throws Exception {

		if (this.zkServer != null) {
			this.zkServer.stop();
			this.zkServer.close();
		}
	}
}
