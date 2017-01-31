package org.diehl.dcs.kalinka.util;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

public class LangUtil {

	public static <T> Class<? extends T> createClass(final String className, final Class<T> superClass) {

		try {
			final Class<?> clazz = Class.forName(className);
			final Class<? extends T> subClass = clazz.asSubclass(superClass);
			return subClass;
		} catch (final ClassNotFoundException e) {
			throw new RuntimeException("Cannot create class=" + className, e);
		}
	}

	public static <T> T createObject(final String className, final Class<T> superClass) {

		try {
			return createClass(className, superClass).newInstance();

		} catch (final Throwable t) {
			throw new RuntimeException("Could not create object of type=" + className);
		}
	}

	@SafeVarargs
	public static <T> List<T> combine(final List<T>... lists) {

		if (lists == null || lists.length == 0) {
			throw new RuntimeException("Cannot combine null|empty lists");
		}
		return Arrays.asList(lists).stream().reduce(Lists.newArrayList(), (l1, l2) -> {
			l1.addAll(l2);
			return l1;
		});
	}

	@SafeVarargs
	public static <T> List<T> toList(final T... args) {

		return Lists.newArrayList(args);
	}


}
