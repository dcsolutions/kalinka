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
package org.diehl.dcs.kalinka.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class LangUtilTest {


	@Test
	public void testCombine() throws Exception {

		final List<String> l1 = Lists.newArrayList("one", "two");
		final List<String> l2 = Lists.newArrayList("three", "four");
		final List<String> l3 = Lists.newArrayList("five");

		final List<String> l4 = LangUtil.combine(l1, l2, l3);

		assertThat(l4.size(), is(5));
		assertThat(l4.get(0), is("one"));
		assertThat(l4.get(1), is("two"));
		assertThat(l4.get(2), is("three"));
		assertThat(l4.get(3), is("four"));
		assertThat(l4.get(4), is("five"));
	}

}
