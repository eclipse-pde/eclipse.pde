/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.core.runtime.model.*;
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
		String name = isFragmentModel() ? "fragment" : "plugin";
		return new NLResourceHelper(name, getNLLookupLocations());
	}
	
	public URL getNLLookupLocation() {
		String installLocation = getInstallLocation();
		if (installLocation.startsWith("file:") == false)
			installLocation = "file:" + installLocation;
		try {
			URL url = new URL(installLocation + "/");
			return url;
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
	
	protected abstract File getFile();

	public String getInstallLocation() {
		return installLocation;
	}
	public boolean isEditable() {
		return false;
	}
	public void load() {
		File file = getFile();
		if (file == null)
			return;
		if (file.exists()) {
			try {
				InputStream stream = new FileInputStream(file); 
				load(stream, false);
				stream.close();
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.logException(e);
			}
		} else {
			pluginBase = (PluginBase) createPluginBase();
			pluginBase.setModel(this);
			loaded = true;
		}
	}
	public void load(PluginModel descriptorModel) {
		PluginBase pluginBase = (PluginBase) getPluginBase();
		if (pluginBase == null) {
			pluginBase = (PluginBase) createPluginBase();
			this.pluginBase = pluginBase;
		} else {
			pluginBase.reset();
		}
		pluginBase.load(descriptorModel);
		updateTimeStamp();
		loaded = true;
	}

	public boolean isInSync() {
		return isInSync(getLocalFile());
	}

	private File getLocalFile() {
		String manifest = isFragmentModel() ? "fragment.xml" : "plugin.xml";
		String prefix = getInstallLocation();
		if (prefix.startsWith("file:"))
			prefix = prefix.substring(5);
		return new File(prefix + File.separator + manifest);
	}

	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	public void setInstallLocation(String newInstallLocation) {
		installLocation = newInstallLocation;
	}
}
