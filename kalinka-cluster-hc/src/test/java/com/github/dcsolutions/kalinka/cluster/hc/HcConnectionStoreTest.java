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

import org.junit.Before;
import org.junit.Test;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class HcConnectionStoreTest {

	private static final String GROUP_NAME = "abc-group";

	private HazelcastInstance hzInstance;

	@Before
	public void setUp() throws Exception {

		final Config config = new Config();
		config.getGroupConfig().setName(GROUP_NAME);
		config.getNetworkConfig().setAd
		this.hzInstance = Hazelcast.newHazelcastInstance();
	}

	@Test
	public void testUpsertNotPresent() throws Exception {

	}

}
