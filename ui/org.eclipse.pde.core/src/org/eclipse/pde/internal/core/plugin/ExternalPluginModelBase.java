/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.*;

public abstract class ExternalPluginModelBase extends AbstractPluginModelBase {

	private static final long serialVersionUID = 1L;

	private String fInstallLocation;

	private String fLocalization;

	public ExternalPluginModelBase() {
		super();
	}

	protected NLResourceHelper createNLResourceHelper() {
		return (fLocalization == null) ? null : new NLResourceHelper(fLocalization, PDEManager.getNLLookupLocations(this));
	}

	public URL getNLLookupLocation() {
		try {
			if (fInstallLocation != null && new File(fInstallLocation).isDirectory() && !fInstallLocation.endsWith("/")) //$NON-NLS-1$
				return new URL("file:" + fInstallLocation + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			return new URL("file:" + fInstallLocation); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public IBuildModel getBuildModel() {
		return null;
	}

	public String getInstallLocation() {
		return fInstallLocation;
	}

	public boolean isEditable() {
		return false;
	}

	public void load() {
	}

	public void load(BundleDescription description, PDEState state) {
		IPath path = new Path(description.getLocation());
		String device = path.getDevice();
		if (device != null)
			path = path.setDevice(device.toUpperCase());
		setInstallLocation(path.toOSString());
		fLocalization = state.getBundleLocalization(description.getBundleId());
		super.load(description, state);
	}

	public boolean isInSync() {
		return isInSync(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		if (file.isFile())
			return file;

		file = new File(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		if (!file.exists()) {
			String manifest = isFragmentModel() ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR : ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
			file = new File(getInstallLocation(), manifest);
		}
		return file;
	}

	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	public void setInstallLocation(String newInstallLocation) {
		fInstallLocation = newInstallLocation;
	}

	public String getLocalization() {
		return fLocalization;
	}
}
