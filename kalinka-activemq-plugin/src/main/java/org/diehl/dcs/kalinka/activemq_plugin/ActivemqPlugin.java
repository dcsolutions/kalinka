/*
 * Copyright [2017] [DCS <Info-dcs@diehl.com>] Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

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

	private static final String CLIENT_ID_PREFIX_KAFKA = "kafka-client";

	private final ZkClient zkClient;
	private final String host;

	public ActivemqPlugin(final String zkServers, final String host) {

		LOG.debug("Trying to create ZkClient for zkServers={}, currentHost={}", zkServers, host);
		this.zkClient = new ZkClient(zkServers);
		this.host = host;
		LOG.info("Created ZkClient for zkServers=" + zkServers + ", currentHost=" + host);
	}


	@Override
	public Broker installPlugin(final Broker broker) throws Exception {

		return new BrokerFilter(broker) {

			@Override
			public void addConnection(final ConnectionContext context, final ConnectionInfo info)
					throws Exception {

				LOG.debug("Received connect from clientId={}", context.getClientId());

				super.addConnection(context, info);
				if (context.getClientId().startsWith(CLIENT_ID_PREFIX_KAFKA)) {
					return;
				}
				upsertZkNode(context.getClientId());
			}

			@Override
			public void removeConnection(final ConnectionContext context, final ConnectionInfo info,
					final Throwable error) throws Exception {

				LOG.debug("Received disconnect from clientId={}", context.getClientId());

				super.removeConnection(context, info, error);
				if (context.getClientId().startsWith(CLIENT_ID_PREFIX_KAFKA)) {
					return;
				}
				deleteZkNode(context.getClientId());
			}
		};
	};

	void upsertZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to upsert node={}, to host={}", node, this.host);
		try {
			this.zkClient.createPersistent(node, this.host);
		} catch (final ZkNodeExistsException e) {
			this.deleteZkNode(clientId);
			this.zkClient.createPersistent(node, this.host);
		}
		LOG.info("Upserted node={}, to host={}", node, this.host);
	}

	void deleteZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to delete node={}", node);
		this.zkClient.delete(node);
		LOG.info("Deleted node={}", node);
	}
}
