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
package org.eclipse.pde.internal.core.schema;

import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

public class ExternalSchemaDescriptor extends AbstractSchemaDescriptor {
	private IPluginExtensionPoint info;
	private File file;
	private String fullId;
	private boolean enabled;

	public ExternalSchemaDescriptor(IPluginExtensionPoint info) {
		this.info = info;
		fullId = info.getFullId();
	}

	public ExternalSchemaDescriptor(File file, String fullId, boolean enabled) {
		this.file = file;
		this.fullId = fullId;
		this.enabled = enabled;
	}
	
	public String getPointId() {
		return fullId;
	}

	private File getInstallLocationFile() {
		IPluginModelBase model = info.getPluginModel();
		String installLocation = model.getInstallLocation() + File.separator;
		return new File(installLocation + info.getSchema());
	}

	private File getSourceLocationFile() {
		SourceLocationManager sourceManager =
			PDECore.getDefault().getSourceLocationManager();
		return sourceManager.findSourceFile(
			info.getPluginBase(),
			new Path(info.getSchema()));
	}

	public URL getSchemaURL() {
		try {
			if (file!=null) {
				return new URL("file:" + file.getPath());
			}
			File installFile = getInstallLocationFile();
			if (installFile != null && installFile.exists()) {
				return new URL("file:" + installFile.getPath());
			}
			File sourceLocationFile = getSourceLocationFile();
			if (sourceLocationFile != null && sourceLocationFile.exists()) {
				return new URL("file:" + sourceLocationFile.getPath());
			}
		} catch (MalformedURLException e) {
		}
		return null;
	}

	public boolean isEnabled() {
		return (info!=null)?info.getPluginModel().isEnabled():enabled;
	}
}
