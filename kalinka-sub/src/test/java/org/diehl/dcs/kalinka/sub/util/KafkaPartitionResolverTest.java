package org.diehl.dcs.kalinka.sub.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class KafkaPartitionResolverTest {

	@Rule
	public ExpectedException ee = ExpectedException.none();

	@Test
	public void testPartitionsFromStringBadArgs() throws Exception {

		this.ee.expect(NullPointerException.class);
		KafkaPartitionResolver.partitionsFromString(null);

		this.ee.expect(IllegalStateException.class);
		KafkaPartitionResolver.partitionsFromString("");

		this.ee.expect(IllegalStateException.class);
		KafkaPartitionResolver.partitionsFromString("a,b-c");

		this.ee.expect(IllegalStateException.class);
		KafkaPartitionResolver.partitionsFromString("1,2-4,5,");

		this.ee.expect(IllegalStateException.class);
		KafkaPartitionResolver.partitionsFromString("1,2-,4,5");
	}

	@Test
	public void testPartitionsFromStringOK() throws Exception {

		List<Integer> result = KafkaPartitionResolver.partitionsFromString("1,3-5,9-11");
		assertThat(result.size(), is(7));
		assertThat(result.contains(1), is(true));
		assertThat(result.contains(3), is(true));
		assertThat(result.contains(4), is(true));
		assertThat(result.contains(5), is(true));
		assertThat(result.contains(9), is(true));
		assertThat(result.contains(10), is(true));
		assertThat(result.contains(11), is(true));

		result = KafkaPartitionResolver.partitionsFromString("0-3");
		assertThat(result.size(), is(4));
		assertThat(result.contains(0), is(true));
		assertThat(result.contains(1), is(true));
		assertThat(result.contains(2), is(true));
		assertThat(result.contains(3), is(true));
	}

	@Test
	public void testHashPartitionKey() throws Exception {

		assertThat(KafkaPartitionResolver.hashPartitionKey("cyclops", 3), is(0));
		assertThat(KafkaPartitionResolver.hashPartitionKey("beast", 3), is(0));
		assertThat(KafkaPartitionResolver.hashPartitionKey("pyro", 3), is(1));
		assertThat(KafkaPartitionResolver.hashPartitionKey("rouge", 3), is(1));
		assertThat(KafkaPartitionResolver.hashPartitionKey("wolverine", 3), is(2));
		assertThat(KafkaPartitionResolver.hashPartitionKey("iceman", 3), is(2));
	}
}
