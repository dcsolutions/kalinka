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

package com.github.dcsolutions.kalinka.cluster.zk;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.cluster.IConnectionStore;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ZkConnectionStore implements IConnectionStore {

	private static final Logger LOG = LoggerFactory.getLogger(ZkConnectionStore.class);

	static final String JMS_CLIENT_ID_KALINKA_PUB_REGEX = "kalinka-pub-.*, kalinka-sub-.*";

	private final ZkClient zkClient;
	private final String host;

	public ZkConnectionStore(final String zkServers, final String host) {

		LOG.debug("Trying to create ZkClient for zkServers={}, currentHost={}, clientIdRegexesToIgnore={}", zkServers, host);
		this.zkClient = new ZkClient(zkServers);
		this.host = host;
		LOG.info("Created ZkClient for zkServers={}, currentHost={}", zkServers, this.host);
	}

	@Override
	public void upsertConnection(final String id) {

		LOG.debug("Received connect from id={}", id);
		upsertZkNode(id);
	}

	@Override
	public void removeConnection(final String id) {

		LOG.debug("Received disconnect from id={}", id);
		deleteZkNode(id);
	}

	void upsertZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to upsert node={}, to host={}", node, this.host);
		try {
			this.zkClient.createPersistent(node, this.host);
		} catch (final ZkNodeExistsException e) {
			this.zkClient.writeData(node, this.host);
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
