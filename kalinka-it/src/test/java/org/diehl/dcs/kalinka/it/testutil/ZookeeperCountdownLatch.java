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
package org.diehl.dcs.kalinka.it.testutil;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class ZookeeperCountdownLatch {

	private static final Logger LOG = LoggerFactory.getLogger(ZookeeperCountdownLatch.class);

	public static void waitForZookeeper(final List<String> clients, final ZkClient zkClient) throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(1);

		new Thread() {
			@Override
			public void run() {
				boolean tryAgain = true;
				while (tryAgain) {
					try {
						LOG.info("Trying to read data from zookeeper");
						clients.stream().forEach(client -> zkClient.readData("/" + client));
						cdl.countDown();
						tryAgain = false;

					} catch (final Exception e) {
						LOG.error("exception thrown while checking zk data: ", e);
					}
					try {
						Thread.sleep(100L);
					} catch (final InterruptedException e) {
						//
					}
				}
			};
		}.run();

		cdl.await(10, TimeUnit.SECONDS);
	}
}
