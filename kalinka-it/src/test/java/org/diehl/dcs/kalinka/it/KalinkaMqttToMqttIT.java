package org.diehl.dcs.kalinka.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.diehl.dcs.kalinka.it.model.MqttClient;
import org.diehl.dcs.kalinka.it.testutil.TestUtils;
import org.diehl.dcs.kalinka.it.testutil.ZookeeperCountdownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KalinkaMqttToMqttIT {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaMqttToMqttIT.class);

	String zkConnection = "192.168.33.20:2181,192.168.33.21:2181,192.168.33.22:2181/activemq/connections";

	private ZkClient zkClient;
	private List<String> clients;
	private List<MqttClient> connectors;

	@Before
	public void setUp() throws Exception {

		this.zkClient = new ZkClient(zkConnection);

		clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		connectors = new ArrayList<>();
	}

	@After
	public void tearDown() throws Exception {
		this.zkClient.close();
		this.zkClient = null;
		clients.clear();
		clients = null;
		connectors.clear();
		connectors = null;
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingSingleNode() throws Exception {
		final long intervalInMillis = 5L;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.startWithFixExecutions(3));

		connectors.stream().forEach(con -> {
			TestUtils.stopMqttClientWhenDone(con, LOG);
		});

		assertThat(connectors.get(0).getOut().size(), is(9));
		assertThat(connectors.get(0).getIn().size(), is(6));
		assertThat(connectors.get(1).getOut().size(), is(9));
		assertThat(connectors.get(1).getIn().size(), is(6));
		assertThat(connectors.get(2).getOut().size(), is(9));
		assertThat(connectors.get(2).getIn().size(), is(6));

	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingWithIntermediateDisconnect() throws Exception {
		final long intervalInMillis = 1000;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.get(0).startWithFixExecutions(4);
		connectors.get(1).startWithFixExecutions(8);
		connectors.get(2).startWithFixExecutions(8);

		TestUtils.stopMqttClientWhenDone(connectors.get(0), LOG);
		connectors.stream().forEach(con -> {
			TestUtils.stopMqttClientWhenDone(con, LOG);
		});

		LOG.info("IN:");
		connectors.get(1).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(12));
		assertThat(connectors.get(0).getIn().size(), is(10));
		assertThat(connectors.get(1).getOut().size(), is(24));
		assertThat(connectors.get(1).getIn().size(), is(12));
		assertThat(connectors.get(2).getOut().size(), is(24));
		assertThat(connectors.get(2).getIn().size(), is(12));
	}


	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingWithIntermediateReconnect() throws Exception {
		final long intervalInMillis = 5000;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		LOG.info("START PUBLISHING WITH ALL CLIENTS");
		connectors.get(0).startWithFixExecutions(2);
		connectors.get(1).startWithFixExecutions(3);
		connectors.get(2).startWithFixExecutions(3);

		LOG.info("STOP PUBLISHING WITH BEAST");
		TestUtils.stopMqttClientWhenDone(connectors.get(0), LOG);

		connectors.get(0).setUrl("tcp://192.168.33.22:1883");
		connectors.get(0).reconnect();
		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		LOG.info("START PUBLISHING WITH BEAST AGAIN");
		connectors.get(0).restartWithFixExecutions(1);

		connectors.stream().forEach(con -> {
			TestUtils.stopMqttClientWhenDone(con, LOG);
		});

		assertThat(connectors.get(0).getOut().size(), is(6));
		assertThat(connectors.get(0).getIn().size(), is(6));
		assertThat(connectors.get(1).getOut().size(), is(9));
		assertThat(connectors.get(1).getIn().size(), is(5));
		assertThat(connectors.get(2).getOut().size(), is(9));
		assertThat(connectors.get(2).getIn().size(), is(5));
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingMultipleNodes() throws Exception {
		final long intervalInMillis = 100;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.startWithFixExecutions(3));

		connectors.stream().forEach(con -> {
			TestUtils.stopMqttClientWhenDone(con, LOG);
		});

		assertThat(connectors.get(0).getOut().size(), is(9));
		assertThat(connectors.get(0).getIn().size(), is(6));
		assertThat(connectors.get(1).getOut().size(), is(9));
		assertThat(connectors.get(1).getIn().size(), is(6));
		assertThat(connectors.get(2).getOut().size(), is(9));
		assertThat(connectors.get(2).getIn().size(), is(6));
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingMultipleNodesMultipleMessagesInSmallIntervals() throws Exception {
		final long intervalInMillis = 5L;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.startWithFixExecutions(1000));

		Thread.sleep(10000);

		connectors.stream().forEach(con -> {
			TestUtils.stopMqttClientWhenDone(con, LOG);
		});


		assertThat(connectors.get(0).getOut().size(), is(3000));
		assertThat(connectors.get(0).getIn().size(), is(2000));
		assertThat(connectors.get(1).getOut().size(), is(3000));
		assertThat(connectors.get(1).getIn().size(), is(2000));
		assertThat(connectors.get(2).getOut().size(), is(3000));
		assertThat(connectors.get(2).getIn().size(), is(2000));
	}
}
