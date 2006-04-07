/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

public class BundleHelper {
	private Bundle bundle;
	private BundleContext context;
	private static BundleHelper defaultInstance;
	private boolean debug = false;

	public static BundleHelper getDefault() {
		return defaultInstance;
	}

	static void close() {
		if (defaultInstance != null) {
			defaultInstance.context = null;
			defaultInstance.bundle = null;
			defaultInstance = null;
		}
	}

	BundleHelper(BundleContext context) throws RuntimeException {
		if (defaultInstance != null)
			throw new RuntimeException("Can not instantiate bundle helper"); //$NON-NLS-1$
		this.context = context;
		defaultInstance = this;
		bundle = context.getBundle();
		debug = "true".equalsIgnoreCase(Platform.getDebugOption(IPDEBuildConstants.PI_PDEBUILD + "/debug")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public final URL find(IPath path) {
		return FileLocator.find(bundle, path, null);
	}

	public final URL find(IPath path, Map override) {
		return FileLocator.find(bundle, path, override);
	}

	public final ILog getLog() {
		return Platform.getLog(bundle);
	}

	public final IPath getStateLocation() throws IllegalStateException {
		return Platform.getStateLocation(getDefault().bundle);
	}

	public final InputStream openStream(IPath file) throws IOException {
		return FileLocator.openStream(bundle, file, false);
	}

	public final InputStream openStream(IPath file, boolean localized) throws IOException {
		return FileLocator.openStream(bundle, file, localized);
	}

	public String toString() {
		return bundle.getSymbolicName();
	}

	public Bundle getBundle() {
		return bundle;
	}

	public Object acquireService(String serviceName) {
		ServiceReference reference = context.getServiceReference(serviceName);
		if (reference == null)
			return null;
		return context.getService(reference);
	}

	public boolean isDebugging() {
		return debug;
	}
	
	public Filter createFilter(String filter) {
		try {
			return context.createFilter(filter);
		} catch (InvalidSyntaxException e) {
		    //Ignore, this has been caught when resolving the state.
			return null;
		}
	}
}
