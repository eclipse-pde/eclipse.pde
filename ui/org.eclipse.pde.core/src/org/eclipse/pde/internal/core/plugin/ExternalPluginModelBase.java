/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDEState;

public abstract class ExternalPluginModelBase extends AbstractPluginModelBase {

	private String fInstallLocation;
	
	private String fLocalization = null;

	public ExternalPluginModelBase() {
		super();
	}
	
	protected NLResourceHelper createNLResourceHelper() {
		return new NLResourceHelper(fLocalization == null ? "plugin" : fLocalization, getNLLookupLocations()); //$NON-NLS-1$
	}
	
	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation()); //$NON-NLS-1$
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
	
	public void load(BundleDescription description, PDEState state, boolean ignoreExtensions) {
		IPath path = new Path(description.getLocation());
		String device = path.getDevice();
		if (device != null)
			path = path.setDevice(device.toUpperCase());
		setInstallLocation(path.toOSString());
		super.load(description, state, ignoreExtensions);
	}
		
	public boolean isInSync() {
		return isInSync(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		if (file.isFile() && new Path(file.getAbsolutePath()).getFileExtension().equals("jar")) //$NON-NLS-1$
			return file;

		file = new File(file, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (!file.exists()) {
			String manifest = isFragmentModel() ? "fragment.xml" : "plugin.xml"; //$NON-NLS-1$ //$NON-NLS-2$
			file = new File(getInstallLocation(), manifest);
		}
		return file;
	}

	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	public void setInstallLocation(String newInstallLocation) {
		fInstallLocation = newInstallLocation;
		File file = new File(newInstallLocation);
		if (file.isDirectory())
			fInstallLocation += File.separator;
	}
}
