package org.diehl.dcs.kalinka.sub.util;

import static org.diehl.dcs.kalinka.util.LangUtil.combine;
import static org.diehl.dcs.kalinka.util.LangUtil.splitCsStrings;

import java.util.List;

import org.apache.kafka.common.utils.Utils;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

public class KafkaPartitionResolver {

	public static List<Integer> partitionsFromString(final String partitionsString) {

		Preconditions.checkNotNull(partitionsString);
		Preconditions.checkState(!partitionsString.isEmpty() && partitionsString.matches("(\\d+[,-]{1})+\\d+"));

		final List<String> splitted = splitCsStrings(partitionsString);
		return splitted.stream().map(s -> {
			final List<String> ranges = Splitter.on('-').splitToList(s);

			final List<Integer> singleElems = Lists.newArrayList();

			if (ranges.isEmpty()) {
				return singleElems;
			}
			if (ranges.size() == 1) {
				singleElems.add(Integer.valueOf(ranges.get(0)));
				return singleElems;
			}
			for (int k = 0; k < ranges.size() - 1; k++) {
				singleElems.addAll(resolveRange(Integer.valueOf(ranges.get(k)), Integer.valueOf(ranges.get(k + 1))));
			}
			return singleElems;
		}).reduce(Lists.newArrayList(), (l1, l2) -> {
			return combine(l1, l2);
		});
	}

	private static List<Integer> resolveRange(final Integer first, final Integer second) {

		if (second < first) {
			throw new IllegalArgumentException("In a range the second element must be > first element");
		}
		final List<Integer> result = Lists.newArrayList();
		for (int i = first; i <= second; i++) {
			result.add(i);
		}
		return result;
	}

	public static int hashPartitionKey(final String key, final int numPartitions) {

		return Utils.toPositive(Utils.murmur2(key.getBytes())) % numPartitions;
	}

}
