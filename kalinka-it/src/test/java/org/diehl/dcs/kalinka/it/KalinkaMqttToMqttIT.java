package org.diehl.dcs.kalinka.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.diehl.dcs.kalinka.it.model.MqttConnector;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KalinkaMqttToMqttIT {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaMqttToMqttIT.class);

	@Test
	public void testBasicSubscribingAndPublishingSingleNode() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "wolverine", clients, intervalInMillis));
		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		//		LOG.info("IN:");
		//		connectors.get(0).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));
	}

	@Test
	public void testBasicSubscribingAndPublishingWithIntermediateReconnect() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "wolverine", clients, intervalInMillis));
		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		connectors.get(0).stop();

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		LOG.info("IN:");
		connectors.get(1).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(6));
		assertThat(connectors.get(1).getIn().size(), is(5));
		assertThat(connectors.get(2).getOut().size(), is(6));
		assertThat(connectors.get(2).getIn().size(), is(5));
	}

	@Test
	public void testBasicSubscribingAndPublishingMultipleNodes() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));
		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		//		LOG.info("IN:");
		//		connectors.get(0).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));
	}
}
