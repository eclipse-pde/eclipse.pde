/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;

public abstract class ExternalPluginModelBase extends AbstractPluginModelBase {
	private String installLocation;
	private transient IBuildModel buildModel;

	public ExternalPluginModelBase() {
		super();
	}
	protected NLResourceHelper createNLResourceHelper() {
		String name = isFragmentModel() ? "fragment" : "plugin"; //$NON-NLS-1$ //$NON-NLS-2$
		return new NLResourceHelper(name, getNLLookupLocations());
	}
	
	public URL getNLLookupLocation() {
		try {
			return new URL("file:" + getInstallLocation());
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public IBuildModel getBuildModel() {
		if (buildModel == null) {
			buildModel = new ExternalBuildModel(getInstallLocation());
			((ExternalBuildModel) buildModel).load();
		}
		return buildModel;
	}
	
	public String getInstallLocation() {
		return installLocation;
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
		setInstallLocation(path.addTrailingSeparator().toOSString());
		setBundleDescription(description);
		((PluginBase)getPluginBase()).load(description, state);
		updateTimeStamp();
		loaded = true;
		
	}

	public boolean isInSync() {
		return isInSync(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		if (file.isFile() && new Path(file.getAbsolutePath()).getFileExtension().equals("jar"))
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
		installLocation = newInstallLocation;
	}
}
