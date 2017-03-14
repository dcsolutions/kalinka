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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.cluster.IConnectionStore;
import com.github.dcsolutions.kalinka.util.LangUtil;
import com.google.common.base.Preconditions;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

/**
 * @author michas <michas@jarmoni.org>
 *
 * This is a simple setup. Should be improved
 */
public class HcConnectionStore implements IConnectionStore {

	private static final Logger LOG = LoggerFactory.getLogger(HcConnectionStore.class);

	private final HazelcastInstance hazelcastClient;

	private final String mapName;
	private final String host;

	public HcConnectionStore(final String connectionString, final String groupName, final String mapName, final String host) {

		final ClientConfig clientConfig = new ClientConfig();
		clientConfig.setProperty("hazelcast.logging.type", "slf4j");
		clientConfig.getNetworkConfig().setAddresses(LangUtil.splitCsStrings(Preconditions.checkNotNull(connectionString)));
		clientConfig.getGroupConfig().setName(Preconditions.checkNotNull(groupName));
		this.hazelcastClient = HazelcastClient.newHazelcastClient(clientConfig);

		this.mapName = Preconditions.checkNotNull(mapName);
		this.host = Preconditions.checkNotNull(host);
	}

	@Override
	public void upsertConnection(final String id) {

		LOG.debug("Received connect from id={}", id);
		this.upsertHcNode(id);
	}

	@Override
	public void removeConnection(final String id) {

		LOG.debug("Received connect from id={}", id);
		this.deleteHcNode(id);
	}

	public void shutdown() {

		this.hazelcastClient.shutdown();
	}

	void upsertHcNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to upsert node={}, to host={}", node, this.host);
		this.hazelcastClient.getMap(this.mapName).set(clientId, this.host);
	}

	void deleteHcNode(final String clientId) {

		LOG.debug("Trying to delete node={}", clientId);
		this.hazelcastClient.getMap(this.mapName).delete(clientId);
		LOG.info("Deleted node={}", clientId);
	}

}
