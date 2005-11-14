/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

/**
 * Runs JUnit tests contained inside a plugin.
 */
public class LegacyRemotePluginTestRunner extends RemoteTestRunner {

	private String fTestPluginName;
	
	/** 
	 * The main entry point. Supported arguments in addition
	 * to the ones supported by RemoteTestRunner:
	 * <pre>
	 * -testpluginname: the name of the plugin containing the tests.
      * </pre>
     * @see RemoteTestRunner
     */

	public static void main(String[] args) {
		LegacyRemotePluginTestRunner testRunner= new LegacyRemotePluginTestRunner();
		testRunner.init(args);
		testRunner.run();
	}
	
	/**
	 * Returns the Plugin class loader of the plugin containing the test.
	 * @see RemotePluginTestRunner#getClassLoader()
	 */
	protected ClassLoader getClassLoader() {
		if (Platform.getPluginRegistry().getPluginDescriptor(fTestPluginName) != null)
			return Platform
				.getPluginRegistry()
				.getPluginDescriptor(fTestPluginName)
				.getPluginClassLoader();
		throw new IllegalArgumentException("No Classloader found for plug-in " + fTestPluginName); //$NON-NLS-1$
	}

	protected void init(String[] args) {
		defaultInit(args);
		setTestPluginName(args);
	}

	protected void setTestPluginName(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].toLowerCase(Locale.ENGLISH).equals("-testpluginname")) { //$NON-NLS-1$
				if (i < args.length - 1)
					fTestPluginName = args[i + 1];
				return;
			}
		}
		throw new IllegalArgumentException("Parameter -testpluginnname not specified."); //$NON-NLS-1$
	}
}	
