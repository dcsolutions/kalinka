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
package org.diehl.dcs.kalinka.pub.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class HashUtilTest {

	@Test
	public void testHashKey() throws Exception {

		assertThat(HashUtil.hashKey("cyclops", 3), is(0));
		assertThat(HashUtil.hashKey("beast", 3), is(0));
		assertThat(HashUtil.hashKey("archangel", 3), is(0));
		assertThat(HashUtil.hashKey("mystique", 3), is(0));
		assertThat(HashUtil.hashKey("pyro", 3), is(1));
		assertThat(HashUtil.hashKey("phoenix", 3), is(1));
		assertThat(HashUtil.hashKey("rouge", 3), is(1));
		assertThat(HashUtil.hashKey("colossus", 3), is(1));
		assertThat(HashUtil.hashKey("wolverine", 3), is(2));
		assertThat(HashUtil.hashKey("iceman", 3), is(2));
		assertThat(HashUtil.hashKey("sabretooth", 3), is(2));
		assertThat(HashUtil.hashKey("jean", 3), is(2));

		assertThat(HashUtil.hashKey("cyclops", 2), is(0));
		assertThat(HashUtil.hashKey("beast", 2), is(0));
		assertThat(HashUtil.hashKey("archangel", 2), is(1));
		assertThat(HashUtil.hashKey("mystique", 2), is(1));
		assertThat(HashUtil.hashKey("pyro", 2), is(0));
		assertThat(HashUtil.hashKey("phoenix", 2), is(0));
		assertThat(HashUtil.hashKey("rouge", 2), is(1));
		assertThat(HashUtil.hashKey("colossus", 2), is(1));
		assertThat(HashUtil.hashKey("wolverine", 2), is(0));
		assertThat(HashUtil.hashKey("iceman", 2), is(0));
		assertThat(HashUtil.hashKey("sabretooth", 2), is(1));
		assertThat(HashUtil.hashKey("jean", 2), is(1));

		assertThat(HashUtil.hashKey("src", 2), is(1));
	}

}
