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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;

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
	protected Long getResourceTimeStamp() {
		java.nio.file.Path installFile = java.nio.file.Path.of(getInstallLocation());
		BasicFileAttributes installFileAttribute;
		try {
			installFileAttribute = Files.readAttributes(installFile, BasicFileAttributes.class);
		} catch (IOException e) {
			return null;
		}
		if (!installFileAttribute.isDirectory()) {
			return installFileAttribute.lastModifiedTime().toMillis();
		}
		try {
			java.nio.file.Path manifestFile = java.nio.file.Path.of(getInstallLocation(),
					ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
			return Files.getLastModifiedTime(manifestFile).toMillis();
		} catch (IOException e) {
			try {
				String xml = isFragmentModel() ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR
						: ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
				File xmlFile = new File(getInstallLocation(), xml);
				return Files.getLastModifiedTime(xmlFile.toPath()).toMillis();
			} catch (IOException e2) {
				return null;
			}
		}
	}

	public void setInstallLocation(String newInstallLocation) {
		fInstallLocation = newInstallLocation;
	}

	public String getLocalization() {
		return fLocalization;
	}

}
