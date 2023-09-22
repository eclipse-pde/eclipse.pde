/*******************************************************************************
 *  Copyright (c) 2003, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ralf Ebert - Bug 307076 : JUnit Plug-in test runner exception "No Classloader found for plug-in ..." is confusing
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.junit.runner.RemoteTestRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Runs JUnit tests contained inside a plugin.
 */
public class RemotePluginTestRunner extends RemoteTestRunner {

	private String fTestPluginName;
	private ClassLoader fLoaderClassLoader;

	static class BundleClassLoader extends ClassLoader {
		private final Bundle bundle;

		public BundleClassLoader(Bundle target) {
			this.bundle = target;
		}

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			return bundle.loadClass(name);
		}

		@Override
		protected URL findResource(String name) {
			return bundle.getResource(name);
		}

		@Override
		protected Enumeration<URL> findResources(String name) throws IOException {
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
		RemotePluginTestRunner testRunner = new RemotePluginTestRunner();
		testRunner.init(args);
		ClassLoader currentTCCL = Thread.currentThread().getContextClassLoader();
		if (isJUnit5(args)) {
			//change the classloader so that the test classes in testplugin are discoverable
			//by junit5 framework  see bug 520811
			Thread.currentThread().setContextClassLoader(createJUnit5PluginClassLoader(testRunner.getTestPluginName()));
		}
		testRunner.run();
		if (isJUnit5(args)) {
			Thread.currentThread().setContextClassLoader(currentTCCL);
		}
	}

	private static ClassLoader createJUnit5PluginClassLoader(String testPluginName) {
		Bundle testBundle = Platform.getBundle(testPluginName);
		if (testBundle == null) {
			throw new IllegalArgumentException("Bundle \"" + testPluginName + "\" not found. Possible causes include missing dependencies, too restrictive version ranges, or a non-matching required execution environment."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Bundle junit5RuntimeBundle = Platform.getBundle("org.eclipse.jdt.junit5.runtime"); //$NON-NLS-1$
		List<Bundle> platformEngineBundles = findTestEngineBundles();
		platformEngineBundles.add(testBundle);
		if (junit5RuntimeBundle != null) {
			platformEngineBundles.add(junit5RuntimeBundle);
		}
		return new MultiBundleClassLoader(platformEngineBundles);
	}

	private static List<Bundle> findTestEngineBundles() {
		BundleContext bundleContext = FrameworkUtil.getBundle(RemotePluginTestRunner.class).getBundleContext();
		List<Bundle> engineBundles = new ArrayList<>();
		for (Bundle bundle : bundleContext.getBundles()) {
			try {
				BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
				Collection<String> listResources = bundleWiring.listResources("META-INF/services", "org.junit.platform.engine.TestEngine", BundleWiring.LISTRESOURCES_LOCAL); //$NON-NLS-1$//$NON-NLS-2$
				if (!listResources.isEmpty()) {
					engineBundles.add(bundle);
				}
			} catch (Exception e) {
				// check the next bundle
			}
		}
		return engineBundles;
	}

	/**
	 * Returns the Plugin class loader of the plugin containing the test.
	 * @see RemoteTestRunner#getTestClassLoader()
	 */
	@Override
	protected ClassLoader getTestClassLoader() {
		final String pluginId = getTestPluginName();
		return getClassLoader(pluginId);
	}

	public ClassLoader getClassLoader(final String bundleId) {
		Bundle bundle = Platform.getBundle(bundleId);
		if (bundle == null) {
			throw new IllegalArgumentException("Bundle \"" + bundleId + "\" not found. Possible causes include missing dependencies, too restrictive version ranges, or a non-matching required execution environment."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return new BundleClassLoader(bundle);
	}

	@Override
	public void init(String[] args) {
		readPluginArgs(args);
		if (isJUnit5(args)) {
			// changing the classloader to get the testengines for junit5
			// during initialization - see bug 520811
			ClassLoader currentTCCL = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(new MultiBundleClassLoader(findTestEngineBundles()));
				defaultInit(args);
			} finally {
				Thread.currentThread().setContextClassLoader(currentTCCL);
			}
			return;
		}
		defaultInit(args);
	}

	@SuppressWarnings("nls")
	private static boolean isJUnit5(String[] args) {
		return indexOf(args, "-runasjunit5"::equalsIgnoreCase) > -1 || indexOf(args, "org.eclipse.jdt.internal.junit5.runner.JUnit5TestLoader"::equals) > -1;
	}

	public void readPluginArgs(String[] args) {
		fTestPluginName = getArgumentValue(args, "-testpluginname"); //$NON-NLS-1$
		String loaderPlugin = getArgumentValue(args, "-loaderpluginname"); //$NON-NLS-1$
		if (loaderPlugin != null) {
			fLoaderClassLoader = getClassLoader(loaderPlugin);
		}
		if (getTestPluginName() == null) {
			throw new IllegalArgumentException("Parameter -testpluginnname not specified."); //$NON-NLS-1$
		}
		if (fLoaderClassLoader == null) {
			fLoaderClassLoader = getClass().getClassLoader();
		}
	}

	@Override
	protected Class<?> loadTestLoaderClass(String className) throws ClassNotFoundException {
		return fLoaderClassLoader.loadClass(className);
	}

	static String getArgumentValue(String[] arguments, String key) {
		int index = indexOf(arguments, key::equalsIgnoreCase);
		return 0 <= index && (index + 1) < arguments.length ? arguments[index + 1] : null;
	}

	private static int indexOf(String[] args, Predicate<String> isKey) {
		for (int i = 0; i < args.length; i++) {
			if (isKey.test(args[i])) {
				return i;
			}
		}
		return -1;
	}

	public String getTestPluginName() {
		return fTestPluginName;
	}
}
