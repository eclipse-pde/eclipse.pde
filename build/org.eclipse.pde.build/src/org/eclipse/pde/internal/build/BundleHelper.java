/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.NativeCodeDescription;
import org.eclipse.osgi.service.resolver.NativeCodeSpecification;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class BundleHelper {
	private Bundle bundle;
	private BundleContext context;
	private static BundleHelper defaultInstance;
	private boolean debug = false;
	private ILog log = null;

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

	public final URL find(IPath path, Map<String, String> override) {
		return FileLocator.find(bundle, path, override);
	}

	public final ILog getLog() {
		if (log == null)
			return ILog.of(bundle);
		return log;
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

	@Override
	public String toString() {
		return bundle.getSymbolicName();
	}

	public Bundle getBundle() {
		return bundle;
	}

	public IProvisioningAgent getProvisioningAgent(URI location) {
		//Is there already an agent for this location?
		String filter = "(locationURI=" + String.valueOf(location) + ")"; //$NON-NLS-1$//$NON-NLS-2$
		ServiceReference<?>[] serviceReferences = null;
		try {
			serviceReferences = context.getServiceReferences(IProvisioningAgent.SERVICE_NAME, filter);
			if (serviceReferences != null) {
				return (IProvisioningAgent) context.getService(serviceReferences[0]);
			}
		} catch (InvalidSyntaxException e) {
			// ignore
		} finally {
			if (serviceReferences != null)
				context.ungetService(serviceReferences[0]);
		}

		IProvisioningAgentProvider provider = acquireService(IProvisioningAgentProvider.class);
		try {
			return provider.createAgent(location);
		} catch (ProvisionException e) {
			return null;
		}
	}

	public <T> T acquireService(Class<T> serviceClass) {
		ServiceReference<T> reference = context.getServiceReference(serviceClass);
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

	public Filter getFilter(BundleDescription bundleDescription) {
		if (bundleDescription == null)
			return null;

		String platformFilter = bundleDescription.getPlatformFilter();
		String nativeFilter = null;

		NativeCodeSpecification nativeCodeSpec = bundleDescription.getNativeCodeSpecification();
		if (nativeCodeSpec != null) {
			NativeCodeDescription[] possibleSuppliers = nativeCodeSpec.getPossibleSuppliers();
			ArrayList<Filter> supplierFilters = new ArrayList<>(possibleSuppliers.length);
			for (NativeCodeDescription possibleSupplier : possibleSuppliers) {
				if (possibleSupplier.getFilter() != null)
					supplierFilters.add(possibleSupplier.getFilter());
			}
			if (supplierFilters.size() == 1)
				nativeFilter = supplierFilters.get(0).toString();
			else if (supplierFilters.size() > 1) {
				StringBuffer buffer = new StringBuffer("(|"); //$NON-NLS-1$
				for (Filter filter : supplierFilters) {
					buffer.append(filter.toString());
				}
				buffer.append(")"); //$NON-NLS-1$
				nativeFilter = buffer.toString();
			}
		}

		String filterString = null;
		if (platformFilter != null && nativeFilter != null)
			filterString = "(&" + platformFilter + nativeFilter + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		else
			filterString = platformFilter != null ? platformFilter : nativeFilter;

		if (filterString != null)
			return createFilter(filterString);
		return null;
	}

	public void setLog(Object antLog) {
		if (antLog == null) {
			log = null;
			return;
		}

		try {
			log = new AntLogAdapter(antLog);
		} catch (NoSuchMethodException e) {
			log = null;
		}
	}

	public static String[] getClasspath(Dictionary<String, String> manifest) {
		return org.eclipse.pde.internal.publishing.Utils.getBundleClasspath(manifest);
	}

	public static String getManifestHeader(Dictionary<String, String> manifest, String header) {
		return org.eclipse.pde.internal.publishing.Utils.getBundleManifestHeader(manifest, header);
	}
}
