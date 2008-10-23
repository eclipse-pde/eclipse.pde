/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.runtime;

import java.util.Collections;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.tests.macro.MacroPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Utility methods for JUnit tests.
 */
public class TestUtils {

	public static Bundle getBundle(String symbolicName) {
		Bundle[] bundles = MacroPlugin.getDefault().getPackageAdmin().getBundles(symbolicName, null);
		
		if (bundles != null) {
			return bundles[0];
		}
		
		return null;
	}
	
	public static IExtensionPoint getExtensionPoint(String extensionPointId) {
		return Platform.getExtensionRegistry().getExtensionPoint(extensionPointId);
	}
	
	public static IExtension getExtension(String extensionId) {
		return Platform.getExtensionRegistry().getExtension(extensionId);
	}
	
	public static ServiceReference getServiceReference(String clazzName) {
		return MacroPlugin.getBundleContext().getServiceReference(clazzName);
	}

	public static String findPath(String path) {
		return FileLocator.find(MacroPlugin.getBundleContext().getBundle(), new Path(path), Collections.EMPTY_MAP).toString();
	}
}
