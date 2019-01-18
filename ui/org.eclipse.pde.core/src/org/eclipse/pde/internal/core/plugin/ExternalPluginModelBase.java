/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.PDEState;

public abstract class ExternalPluginModelBase extends AbstractPluginModelBase {

	private static final long serialVersionUID = 1L;

	private String fInstallLocation;

	private String fLocalization;

	public ExternalPluginModelBase() {
		super();
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return (fLocalization == null) ? null : new NLResourceHelper(fLocalization, PDEManager.getNLLookupLocations(this));
	}

	@Override
	@Deprecated
	public URL getNLLookupLocation() {
		try {
			if (fInstallLocation != null && new File(fInstallLocation).isDirectory() && !fInstallLocation.endsWith("/")) { //$NON-NLS-1$
				return new URL("file:" + fInstallLocation + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return new URL("file:" + fInstallLocation); //$NON-NLS-1$
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	@Deprecated
	public IBuildModel getBuildModel() {
		return null;
	}

	@Override
	public String getInstallLocation() {
		return fInstallLocation;
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public void load() {
	}

	@Override
	public void load(BundleDescription description, PDEState state) {
		IPath path = new Path(description.getLocation());
		String device = path.getDevice();
		if (device != null) {
			path = path.setDevice(device.toUpperCase());
		}
		setInstallLocation(path.toOSString());
		fLocalization = state.getBundleLocalization(description.getBundleId());
		super.load(description, state);
	}

	@Override
	public boolean isInSync() {
		return isInSync(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		if (file.isFile()) {
			return file;
		}

		file = new File(file, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
		if (!file.exists()) {
			String manifest = isFragmentModel() ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR : ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
			file = new File(getInstallLocation(), manifest);
		}
		return file;
	}

	@Override
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
