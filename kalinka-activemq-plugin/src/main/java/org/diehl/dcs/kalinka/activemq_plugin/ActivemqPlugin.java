package org.diehl.dcs.kalinka.activemq_plugin;


import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ActivemqPlugin implements BrokerPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(ActivemqPlugin.class);

	private final ZkClient zkClient;
	private final String host;

	public ActivemqPlugin(final String zkServers, final String host) {

		LOG.info("Creating ZkClient for zkServers=" + zkServers + ", currentHost=" + host);
		this.zkClient = new ZkClient(zkServers);
		this.host = host;
	}


	@Override
	public Broker installPlugin(final Broker broker) throws Exception {

		return new BrokerFilter(broker) {

			@Override
			public void addConnection(final ConnectionContext context, final ConnectionInfo info)
					throws Exception {

				super.addConnection(context, info);
				upsertZkNode(context.getClientId());
			}

			@Override
			public void removeConnection(final ConnectionContext context, final ConnectionInfo info,
				final Throwable error) throws Exception {

				super.removeConnection(context, info, error);
				deleteZkNode(context.getClientId());
			}
		};
	};

	void upsertZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to upsert node={}, to host{}", node, this.host);
		try {
			this.zkClient.createPersistent(node, this.host);
		}
		catch(final ZkNodeExistsException e) {
			this.deleteZkNode(clientId);
			this.zkClient.createPersistent(node, this.host);
		}
		LOG.info("Upserted node={}, to host{}", node, this.host);
	}

	void deleteZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to delete node={}", node);
		this.zkClient.delete(node);
		LOG.info("Deleted node={}", node);
	}

}
