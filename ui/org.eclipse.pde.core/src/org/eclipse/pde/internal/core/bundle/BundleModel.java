/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public abstract class BundleModel extends AbstractModel implements IBundleModel {

	private static final long serialVersionUID = 1L;

	private final Bundle fBundle;

	public BundleModel() {
		fBundle = new Bundle();
		fBundle.setModel(this);
	}

	@Override
	public IBundle getBundle() {
		if (!isLoaded()) {
			load();
		}
		return fBundle;
	}

	@Override
	public String getInstallLocation() {
		return null;
	}

	@Override
	public abstract void load();

	@Override
	public boolean isFragmentModel() {
		return fBundle.getHeader(Constants.FRAGMENT_HOST) != null;
	}

	@Override
	public void load(InputStream source, boolean outOfSync) {
		try {
			setLoaded(true); // Must be set before loading the manifest otherwise calls to getModel() cause a stack overflow
			fBundle.load(ManifestElement.parseBundleManifest(source, null));
			if (!outOfSync) {
				updateTimeStamp();
			}

		} catch (BundleException e) {
			PDECore.log(e);
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

	public void load(BundleDescription desc, PDEState state) {
		long id = desc.getBundleId();
		Properties properties = new Properties();
		properties.put(Constants.BUNDLE_SYMBOLICNAME, desc.getSymbolicName());
		String value = state.getPluginName(id);
		if (value != null) {
			properties.put(Constants.BUNDLE_NAME, value);
		}
		value = state.getProviderName(id);
		if (value != null) {
			properties.put(Constants.BUNDLE_VENDOR, value);
		}
		value = state.getClassName(id);
		if (value != null) {
			properties.put(Constants.BUNDLE_ACTIVATOR, value);
		}
		value = state.getBundleLocalization(id);
		if (value != null) {
			properties.put(Constants.BUNDLE_LOCALIZATION, value);
		}
		if (state.hasExtensibleAPI(id))
		 {
			properties.put(ICoreConstants.EXTENSIBLE_API, "true"); //$NON-NLS-1$
		}
		if (state.isPatchFragment(id))
		 {
			properties.put(ICoreConstants.PATCH_FRAGMENT, "true"); //$NON-NLS-1$
		}
		String[] libraries = state.getLibraryNames(id);
		if (libraries.length > 0) {
			StringBuilder buffer = new StringBuilder();
			for (String library : libraries) {
				if (buffer.length() > 0) {
					buffer.append(","); //$NON-NLS-1$
					buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
					buffer.append(" "); //$NON-NLS-1$
				}
				buffer.append(library);
			}
			properties.put(Constants.BUNDLE_CLASSPATH, buffer.toString());
		}
		if (desc.getHost() != null) {
			properties.put(Constants.FRAGMENT_HOST, writeFragmentHost(desc.getHost()));
		}
		setLoaded(true); // Must set loaded before creating headers as calls to getModel() throw stack overflows
		fBundle.load(properties);
		updateTimeStamp();
	}

	private String writeFragmentHost(HostSpecification host) {
		String id = host.getName();
		String version = host.getVersionRange().toString();
		StringBuilder buffer = new StringBuilder();
		if (id != null) {
			buffer.append(id);
		}

		if (version != null && version.trim().length() > 0) {
			buffer.append(";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + version + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return buffer.toString();
	}

	@Override
	public void reload(InputStream source, boolean outOfSync) {
		load(source, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
	}

	@Override
	public String toString() {
		if (fBundle != null) {
			StringBuilder buf = new StringBuilder();
			buf.append(fBundle.getHeader(Constants.BUNDLE_SYMBOLICNAME));
			buf.append(" ("); //$NON-NLS-1$
			buf.append(fBundle.getHeader(Constants.BUNDLE_VERSION));
			buf.append(')');
			return buf.toString();
		}
		return "Unknown bundle model"; //$NON-NLS-1$
	}
}
