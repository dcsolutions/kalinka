package org.diehl.dcs.kalinka.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.diehl.dcs.kalinka.it.model.MqttClient;
import org.diehl.dcs.kalinka.it.testutil.ZookeeperCountdownLatch;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KalinkaMqttToMqttIT {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaMqttToMqttIT.class);

	String zkConnection = "192.168.33.20:2181,192.168.33.21:2181,192.168.33.22:2181/activemq/connections";

	private ZkClient zkClient;

	@Before
	public void setUp() throws Exception {

		this.zkClient = new ZkClient(zkConnection);
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingSingleNode() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttClient> connectors = new ArrayList<>();
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));

		connectors.stream().forEach(con -> con.stop());
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingWithIntermediateDisconnect() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttClient> connectors = new ArrayList<>();
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		connectors.get(0).stop();

		Thread.sleep(intervalInMillis + 1000);

		LOG.info("IN:");
		connectors.get(1).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(6));
		assertThat(connectors.get(1).getIn().size(), is(5));
		assertThat(connectors.get(2).getOut().size(), is(6));
		assertThat(connectors.get(2).getIn().size(), is(5));

		connectors.get(1).stop();
		connectors.get(2).stop();
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingWithIntermediateReconnect() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttClient> connectors = new ArrayList<>();
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		LOG.info("START PUBLISHING WITH ALL CLIENTS");
		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		LOG.info("STOP PUBLISHING WITH BEAST");
		connectors.get(0).stop();
		connectors.get(0).setUrl("tcp://192.168.33.22:1883");
		connectors.get(0).reconnect();
		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		LOG.info("START PUBLISHING WITH BEAST");
		connectors.get(0).restart();

		Thread.sleep(intervalInMillis + 1000);

		assertThat(connectors.get(0).getOut().size(), is(6));
		assertThat(connectors.get(0).getIn().size(), is(6));
		assertThat(connectors.get(1).getOut().size(), is(6));
		assertThat(connectors.get(1).getIn().size(), is(6));
		assertThat(connectors.get(2).getOut().size(), is(6));
		assertThat(connectors.get(2).getIn().size(), is(6));

		connectors.get(0).stop();
		connectors.get(1).stop();
		connectors.get(2).stop();
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingMultipleNodes() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttClient> connectors = new ArrayList<>();
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));

		connectors.stream().forEach(con -> con.stop());
	}
}
