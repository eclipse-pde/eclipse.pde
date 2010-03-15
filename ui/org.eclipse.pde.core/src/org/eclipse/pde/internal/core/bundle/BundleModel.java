/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.text.bundle.BundleModelFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public abstract class BundleModel extends AbstractModel implements IBundleModel {

	private static final long serialVersionUID = 1L;

	private Bundle fBundle;

	public BundleModel() {
		fBundle = new Bundle();
		fBundle.setModel(this);
	}

	public IBundle getBundle() {
		if (!isLoaded())
			load();
		return fBundle;
	}

	public String getInstallLocation() {
		return null;
	}

	public abstract void load();

	public boolean isFragmentModel() {
		return fBundle.getHeader(Constants.FRAGMENT_HOST) != null;
	}

	public void load(InputStream source, boolean outOfSync) {
		try {
			fBundle.load(ManifestElement.parseBundleManifest(source, null));
			if (!outOfSync)
				updateTimeStamp();
			setLoaded(true);
			// format headers
			BundleModelFactory factory = new BundleModelFactory(this);
			Map headers = fBundle.getHeaders();
			Object[] keys = headers.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				String key = (String) keys[i];
				String value = (String) headers.get(key);
				IManifestHeader header = factory.createHeader(key, value);
				header.update();
				String formatted = header.getValue();
				if (formatted != null && formatted.trim().length() > 0) {
					fBundle.setHeader(key, formatted);
				}
			}
		} catch (BundleException e) {
		} catch (IOException e) {
		} finally {
		}
	}

	public void load(BundleDescription desc, PDEState state) {
		long id = desc.getBundleId();
		Properties properties = new Properties();
		properties.put(Constants.BUNDLE_SYMBOLICNAME, desc.getSymbolicName());
		String value = state.getPluginName(id);
		if (value != null)
			properties.put(Constants.BUNDLE_NAME, value);
		value = state.getProviderName(id);
		if (value != null)
			properties.put(Constants.BUNDLE_VENDOR, value);
		value = state.getClassName(id);
		if (value != null)
			properties.put(Constants.BUNDLE_ACTIVATOR, value);
		value = state.getBundleLocalization(id);
		if (value != null)
			properties.put(Constants.BUNDLE_LOCALIZATION, value);
		if (state.hasExtensibleAPI(id))
			properties.put(ICoreConstants.EXTENSIBLE_API, "true"); //$NON-NLS-1$
		if (state.isPatchFragment(id))
			properties.put(ICoreConstants.PATCH_FRAGMENT, "true"); //$NON-NLS-1$
		String[] libraries = state.getLibraryNames(id);
		if (libraries.length > 0) {
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < libraries.length; i++) {
				if (buffer.length() > 0) {
					buffer.append(","); //$NON-NLS-1$
					buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
					buffer.append(" "); //$NON-NLS-1$
				}
				buffer.append(libraries[i]);
			}
			properties.put(Constants.BUNDLE_CLASSPATH, buffer.toString());
		}
		if (desc.getHost() != null) {
			properties.put(Constants.FRAGMENT_HOST, writeFragmentHost(desc.getHost()));
		}
		fBundle.load(properties);
		updateTimeStamp();
		setLoaded(true);
	}

	private String writeFragmentHost(HostSpecification host) {
		String id = host.getName();
		String version = host.getVersionRange().toString();
		StringBuffer buffer = new StringBuffer();
		if (id != null)
			buffer.append(id);

		if (version != null && version.trim().length() > 0) {
			buffer.append(";" + Constants.BUNDLE_VERSION_ATTRIBUTE + "=\"" + version + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return buffer.toString();
	}

	public void reload(InputStream source, boolean outOfSync) {
		load(source, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
	}
}
