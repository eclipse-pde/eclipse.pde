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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.osgi.framework.Bundle;

/**
 * Runs JUnit tests contained inside a plugin.
 */
public class RemotePluginTestRunner extends RemoteTestRunner {

	private String fTestPluginName;
	
	class BundleClassLoader extends ClassLoader {
		  private Bundle bundle;
		  public BundleClassLoader(Bundle target) {
		    this.bundle = target;
		  }
		  protected Class findClass(String name) throws ClassNotFoundException {
		    return bundle.loadClass(name);
		  }
		  protected URL findResource(String name) {
		    return bundle.getResource(name);
		  }
		  protected Enumeration findResources(String name) throws IOException {
			   return bundle.getResources(name);
		  }
	}
	
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
		Bundle bundle = Platform.getBundle(fTestPluginName);
		if (bundle == null)
			throw new IllegalArgumentException("No Classloader found for plug-in " + fTestPluginName); //$NON-NLS-1$
		return new BundleClassLoader(bundle);
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
