package org.eclipse.pde.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * This model factory should be used to
 * create new instances of plugin.jars model
 * objects.
 */
public interface IBuildModelFactory {
	/**
	 * Creates a new build entry with
	 * the provided name.
	 * @return a new build.properties entry instance
	 */
	IBuildEntry createEntry(String name);
}