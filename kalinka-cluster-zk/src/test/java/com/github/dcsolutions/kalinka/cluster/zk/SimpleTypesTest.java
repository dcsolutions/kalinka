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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.gruchalski.kafka.java8.KafkaCluster;
import com.gruchalski.kafka.java8.KafkaTopicStatus;
import com.gruchalski.kafka.scala.ConsumedItem;
import com.gruchalski.kafka.scala.KafkaTopicConfiguration;
import com.gruchalski.kafka.scala.KafkaTopicCreateResult;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
@Ignore
public class SimpleTypesTest {
	private final String aKey = "a test key";
	private final String aValue = "a test value";

	@Test
	public void testClusterSetup() {
		new KafkaCluster().start().ifPresent(kafkaClusterSafe -> {
			final ArrayList<KafkaTopicConfiguration> topics = new ArrayList<>();
			topics.add(new KafkaTopicConfiguration("test-topic", 1, 1, new Properties(), KafkaTopicConfiguration.toRackAwareMode("enforced").get()));
			final CountDownLatch latch = new CountDownLatch(topics.size());
			final CompletableFuture<List<KafkaTopicCreateResult>> topicCreateStatuses = kafkaClusterSafe.cluster.withTopics(topics);
			topicCreateStatuses.thenAccept(results -> {
				for (final KafkaTopicCreateResult result1 : results) {
					if (result1.status().toString().equals(KafkaTopicStatus.Exists)) {
						latch.countDown();
					} else {
						fail("Expected topic " + result1.topicConfig().name() + " to be created.");
					}
				}
			});
			try {
				if (!latch.await(10000, TimeUnit.MILLISECONDS)) {
					fail("Expected latch=0, was=" + latch.getCount());
				}
			} catch (final InterruptedException ex1) {
				fail(ex1.getMessage());
			}
			try {
				final CountDownLatch producerLatch = new CountDownLatch(1);
				kafkaClusterSafe.cluster.produce(topics.get(0).name(), aKey, aValue).thenAccept(result2 -> {
					producerLatch.countDown();
				});
				try {
					if (!latch.await(10000, TimeUnit.MILLISECONDS)) {
						fail("Expected latch=0, was=" + latch.getCount());
					}
				} catch (final InterruptedException ex2) {
					fail(ex2.getMessage());
				}
				AsyncUtil.eventually(() -> {
					try {
						final Optional<ConsumedItem<String, String>> consumed =
								kafkaClusterSafe.cluster.consume(topics.get(0).name(), String.class, String.class);
						assertEquals(consumed.get().key(), aKey);
						assertEquals(consumed.get().value(), aValue);
					} catch (final Throwable t1) {
						fail(t1.getMessage());
					}
				});
			} catch (final Throwable t2) {
				fail(t2.getMessage());
			} finally {
				kafkaClusterSafe.cluster.stop();
			}
		});
	}
}
