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

package org.diehl.dcs.kalinka.sub.cache;

import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class BrokerCache implements IBrokerCache {

	private static final Logger LOG = LoggerFactory.getLogger(BrokerCache.class);

	private final ZkClient zkClient;

	private final Cache<String, String> cache;

	public BrokerCache(final ZkClient zkClient, final int initialSize, final long maxSize, final long evictionTimeoutHours) {

		this.zkClient = Preconditions.checkNotNull(zkClient);
		this.cache =
				CacheBuilder.newBuilder().initialCapacity(initialSize).maximumSize(maxSize).expireAfterAccess(evictionTimeoutHours, TimeUnit.HOURS).build();
	}


	@Override
	public String get(final String id) {

		String host = this.cache.getIfPresent(id);
		if (host != null) {
			return host;
		}
		final String path = "/" + id;
		try {
			host = this.zkClient.readData(path);
		} catch (final ZkNoNodeException nex) {
			LOG.debug("Node={} not existing in ZK", path);
			return null;
		}
		this.put(id, host);
		this.zkClient.subscribeDataChanges(path, new IZkDataListener() {

			@Override
			public void handleDataDeleted(final String dataPath) throws Exception {

				cache.invalidate(id);
				LOG.info("Removed host for hostIdentifier={} from cache", id);
			}

			@Override
			public void handleDataChange(final String dataPath, final Object data) throws Exception {

				cache.put(id, (String) data);
				LOG.info("Updated host for hostIdentifier={} with new host={}", id, data);

			}
		});
		return host;
	}

	void put(final String id, final String host) {

		this.cache.put(id, host);
	}

	void del(final String id) {

		this.cache.invalidate(id);
	}

	void delAll() {

		this.cache.invalidateAll();
	}
}
