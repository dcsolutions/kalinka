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

package com.github.dcsolutions.kalinka.benchmarks;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.github.jamm.MemoryMeter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.cluster.zk.AsyncUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gruchalski.kafka.java8.KafkaCluster;
import com.gruchalski.kafka.java8.KafkaTopicStatus;
import com.gruchalski.kafka.scala.ConsumedItem;
import com.gruchalski.kafka.scala.KafkaTopicConfiguration;
import com.gruchalski.kafka.scala.KafkaTopicCreateResult;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
/**
 * @author michas <michas@jarmoni.org>
 * See: http://lemire.me/blog/2015/10/15/on-the-memory-usage-of-maps-in-java/
 *
 */
public class CacheSizeTest {

	private static final Logger LOG = LoggerFactory.getLogger(CacheSizeTest.class);

	private static final char[] SUBSET = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final Random RANDOM = new Random();
	private static final int NUM_CONTROL_KEYS = 10000;

	@Test
	public void testCacheSize() throws Exception {

		final ExecutionResult result01Mio = this.execute(01 * 1000 * 1000);
		final ExecutionResult result05Mio = this.execute(05 * 1000 * 1000);
		final ExecutionResult result10Mio = this.execute(10 * 1000 * 1000);
		final ExecutionResult result15Mio = this.execute(15 * 1000 * 1000);

		result01Mio.print();
		result05Mio.print();
		result10Mio.print();
		result15Mio.print();
	}

	public ExecutionResult execute(final int numExecutions) {

		final ExecutionResult executionResult = new ExecutionResult();
		new KafkaCluster().start().ifPresent(kafkaClusterSafe -> {
			final Properties props = new Properties();
			props.put("cleanup.policy", "compact");
			final ArrayList<KafkaTopicConfiguration> topics =
					Lists.newArrayList(new KafkaTopicConfiguration("test-topic", 1, 1, props, KafkaTopicConfiguration.toRackAwareMode("enforced").get()));
			final CountDownLatch topcicCreatorLatch = new CountDownLatch(topics.size());
			final CompletableFuture<List<KafkaTopicCreateResult>> topicCreateStatuses = kafkaClusterSafe.cluster.withTopics(topics);
			topicCreateStatuses.thenAccept(results -> {
				for (final KafkaTopicCreateResult result1 : results) {
					if (result1.status().toString().equals(KafkaTopicStatus.Exists)) {
						topcicCreatorLatch.countDown();
					} else {
						fail("Expected topic " + result1.topicConfig().name() + " to be created.");
					}
				}
			});
			try {
				if (!topcicCreatorLatch.await(10L, TimeUnit.SECONDS)) {
					fail("Expected topcicCreatorLatch=0, was=" + topcicCreatorLatch.getCount());
				}
			} catch (final InterruptedException ex1) {
				fail(ex1.getMessage());
			}
			try {
				final List<String> controlList = Lists.newArrayList();
				final int controlMod = numExecutions / NUM_CONTROL_KEYS;

				final CountDownLatch producerLatch = new CountDownLatch(numExecutions);
				IntStream.range(0, numExecutions).forEach(c -> {
					final String key = this.getRandom(15);
					try {
						kafkaClusterSafe.cluster.produce(topics.get(0).name(), key, getRandom(15)).thenAccept(result2 -> {
							producerLatch.countDown();
						});
					} catch (final Throwable t) {
						fail(t.getMessage());
					}
					if (c % controlMod == 0) {
						controlList.add(key);
					}
				});
				try {
					if (!producerLatch.await(2L, TimeUnit.MINUTES)) {
						fail("Expected producerLatch=0, was=" + producerLatch.getCount());
					}
				} catch (final InterruptedException ex2) {
					fail(ex2.getMessage());
				}
				final ConcurrentMap<String, String> map = Maps.newConcurrentMap();
				final CountDownLatch consumerLatch = new CountDownLatch(numExecutions);

				// Give a bit of time to write to disk
				Thread.sleep(numExecutions / 200);

				final long consumerStart = System.nanoTime();
				AsyncUtil.eventually(() -> {
					try {
						while (consumerLatch.getCount() > 0) {
							final Optional<ConsumedItem<String, String>> consumed =
									kafkaClusterSafe.cluster.consume(topics.get(0).name(), String.class, String.class);
							map.put(consumed.get().key().get(), consumed.get().value());
							consumerLatch.countDown();
						}
					} catch (final Throwable t1) {
						fail(t1.getMessage());
					}
				}, 3 * 60 * 1000L);

				assertThat(map.size(), is(numExecutions));
				assertThat(controlList.size(), is(NUM_CONTROL_KEYS));

				// Give a bit of time to cool down....
				Thread.sleep(numExecutions / 200);

				final long loadTimeSec = (System.nanoTime() - consumerStart) / (1000 * 1000 * 1000L);
				final OptionalDouble average = controlList.stream().mapToLong(s -> {
					final long start = System.nanoTime();
					map.get(s);
					return (System.nanoTime() - start);
				}).average();

				final MemoryMeter mm = new MemoryMeter();
				executionResult.items = numExecutions;
				executionResult.size = mm.measureDeep(map);
				executionResult.loadTime = loadTimeSec;
				executionResult.executionTime = average.getAsDouble();
			} catch (final Throwable t2) {
				fail(t2.getMessage());
			} finally {
				kafkaClusterSafe.cluster.stop();
			}
		});
		return executionResult;
	}

	private void execute2(final int numExecutions) {

		// ConcurrentMap uses a little bit more of memory than a regular map
		final ConcurrentMap<String, String> map = Maps.newConcurrentMap();
		final List<String> controlList = Lists.newArrayList();
		final int controlMod = numExecutions / NUM_CONTROL_KEYS;

		IntStream.range(0, numExecutions).forEach(c -> {
			final String key = this.getRandom(15);
			map.put(key, getRandom(15));
			if (c % controlMod == 0) {
				controlList.add(key);
			}
		});

		assertThat(map.size(), is(numExecutions));
		assertThat(controlList.size(), is(NUM_CONTROL_KEYS));

		final OptionalDouble average = controlList.stream().mapToLong(s -> {
			final long start = System.nanoTime();
			map.get(s);
			return (System.nanoTime() - start);
		}).average();

		final MemoryMeter mm = new MemoryMeter();
		LOG.info("Memory consumption for {} items: {} byte. Access-Time: {} ns", numExecutions, mm.measureDeep(map), average.getAsDouble());

	}

	private String getRandom(final int len) {

		final char buf[] = new char[len];
		for (int i = 0; i < buf.length; i++) {
			final int index = RANDOM.nextInt(SUBSET.length);
			buf[i] = SUBSET[index];
		}

		return new String(buf);
	}

	private static class ExecutionResult {

		public int items;
		public long size;
		public long loadTime;
		public double executionTime;

		public void print() {
			LOG.info("*** Number items: {}, Memory consumption: {} byte, Load time={} s, Access-Time: {} ns", this.items, this.size, this.loadTime,
					this.executionTime);
		}
	}
}
