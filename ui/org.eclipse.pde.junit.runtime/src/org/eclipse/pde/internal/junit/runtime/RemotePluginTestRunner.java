/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.junit.runtime;

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;

/**
 * Runs JUnit tests contained inside a plugin.
 */
public class RemotePluginTestRunner extends RemoteTestRunner {

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
		RemotePluginTestRunner testRunner= new RemotePluginTestRunner();
		testRunner.init(args);
		testRunner.run();
	}
	
	/**
	 * Returns the Plugin class loader of the plugin containing the test.
	 * @see RemotePluginTestRunner#getClassLoader()
	 */
	protected ClassLoader getClassLoader() {
		if (Platform.getPluginRegistry().getPluginDescriptor(fTestPluginName) != null)
			return Platform.getPluginRegistry().getPluginDescriptor(fTestPluginName).getPluginClassLoader();	
		throw new IllegalArgumentException("No ClassLoader found for testplugin: " + fTestPluginName);
	}

	protected void init(String[] args) {
		defaultInit(args);
		setTestPluginName(args);
	}

	protected void setTestPluginName(String[] args) {
		for(int i= 0; i < args.length; i++) {
			if(args[i].toLowerCase().equals("-testpluginname")) {
				if (i < args.length-1)
					fTestPluginName= args[i+1];
				return;
			}
		}
		throw new IllegalArgumentException("Parameter -testpluginname not specified");
	}
}	