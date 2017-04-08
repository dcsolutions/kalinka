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

import java.util.List;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;

import org.github.jamm.MemoryMeter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

		this.execute(05 * 1000 * 1000);
		this.execute(10 * 1000 * 1000);
		this.execute(15 * 1000 * 1000);
	}

	private void execute(final int numExecutions) {

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
}
