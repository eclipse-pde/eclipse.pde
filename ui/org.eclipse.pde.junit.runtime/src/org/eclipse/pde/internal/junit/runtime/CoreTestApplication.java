/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.boot.IPlatformRunnable;

/**
 * A an application that launches tests once it is started.
 */
public class CoreTestApplication implements IPlatformRunnable {

	/**
	 * Runs a set of tests as defined by the given command line args.
	 * This is the platform application entry point.
	 * @see IPlatformRunnable
	 */
	public Object run(Object arguments) throws Exception {
		RemotePluginTestRunner.main((String[])arguments);
		return null;
	}
}
