/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *
 */
package org.eclipse.pde.internal.junit.runtime;

import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Default implementation used with Java 1.8
 * TODO provide MR variant using stack walker, currently blocked by JDT bug ...
 */
public class Caller {
	private static final String JUNIT_PLATFORM_LAUNCHER = "org.junit.platform.launcher"; //$NON-NLS-1$
	private static final Bundle BUNDLE = FrameworkUtil.getBundle(Caller.class);
	static final Bundle loaderBundle5;
	static final Bundle loaderBundle6;

	static {
		loaderBundle5 = findJUnitBundle("org.eclipse.jdt.junit5.runtime", 5); //$NON-NLS-1$
		loaderBundle6 = findJUnitBundle("org.eclipse.jdt.junit6.runtime", 6); //$NON-NLS-1$
	}

	private static Bundle findJUnitBundle(String bundleId, int junitVersion) {
		Bundle junitRuntimeBundle = Platform.getBundle(bundleId);
		if (junitRuntimeBundle == null) {
			Bundle junit4RuntimeBundle = Platform.getBundle("org.eclipse.jdt.junit4.runtime"); //$NON-NLS-1$
			return findJUnitLauncherByRuntime(junit4RuntimeBundle, junitVersion);
		}
		return junitRuntimeBundle;
	}

	protected static Bundle findJUnitLauncherByRuntime(Bundle junit4RuntimeBundle, int majorVersion) {
		if (junit4RuntimeBundle == null) {
			return BUNDLE;
		}
		for (Bundle bundle : BUNDLE.getBundleContext().getBundles()) {
			BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
			List<BundleCapability> capabilities = bundleWiring.getCapabilities(JUNIT_PLATFORM_LAUNCHER);
			if (!capabilities.isEmpty() && bundle.getVersion().getMajor() == majorVersion) {
				return bundle;
			}
		}

		return BUNDLE;
	}

	static Bundle getBundle(Bundle loaderBundle) {
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTraceElements) {
			try {
				String className = element.getClassName();
				Class<?> clz = loaderBundle.loadClass(className);
				Bundle bundle = FrameworkUtil.getBundle(clz);
				if (bundle == BUNDLE) {
					continue;
				}
				if (bundle != null) {
					return bundle;
				}
			} catch (ClassNotFoundException e) {
				continue;
			}
		}
		return null;
	}

}
