package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.runtime.*;


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
