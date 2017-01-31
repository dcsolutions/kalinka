package org.diehl.dcs.kalinka.util;

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


}
