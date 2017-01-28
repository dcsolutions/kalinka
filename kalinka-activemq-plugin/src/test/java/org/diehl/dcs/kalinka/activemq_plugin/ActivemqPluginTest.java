package org.diehl.dcs.kalinka.activemq_plugin;

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
public class ActivemqPluginTest {

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

		final ActivemqPlugin plugin = new ActivemqPlugin(this.zkServer.getConnectString() + ZK_CHROOT_PATH, "mysuperhost");
		plugin.upsertZkNode("123");

		assertEquals("mysuperhost", this.zkClient.readData(ZK_CHROOT_PATH + "/123").toString());

		final ActivemqPlugin plugin2 = new ActivemqPlugin(this.zkServer.getConnectString() + ZK_CHROOT_PATH, "myevenbetterhost");
		plugin2.upsertZkNode("123");

		assertEquals("myevenbetterhost", this.zkClient.readData(ZK_CHROOT_PATH + "/123").toString());
	}

	@Test
	public void testDeleteZkNode() throws Exception {

		final ActivemqPlugin plugin = new ActivemqPlugin(this.zkServer.getConnectString(), "mysuperhost");
		// no exception is thrown when trying to delete a not existing node
		plugin.deleteZkNode("123");
		plugin.upsertZkNode("123");

		plugin.deleteZkNode("123");
		this.ee.expect(ZkNoNodeException.class);
		this.zkClient.readData("/123");
	}

	@After
	public void tearDown() throws Exception {

		this.zkServer.stop();
	}

}
