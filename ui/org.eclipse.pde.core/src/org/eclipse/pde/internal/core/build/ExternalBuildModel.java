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
package org.eclipse.pde.internal.core.build;

import java.io.*;
import java.net.*;

public class ExternalBuildModel extends BuildModel {
	private String installLocation;

public ExternalBuildModel(String installLocation) {
	this.installLocation = installLocation;
}
public String getInstallLocation() {
	return installLocation;
}
public boolean isEditable() {
	return false;
}
public void load() {
	String location = getFullPath();
	try {
		URL url = new URL(location);
		InputStream stream = url.openStream();
		load(stream, false);
		stream.close();
	} catch (IOException e) {
		build = new Build();
		build.setModel(this);
		loaded = true;
	}
}

protected void updateTimeStamp() {
	File file = new File(getFullPath());
	updateTimeStamp(file);
}

private String getFullPath() {
	String fileName = "build.properties";
	return getInstallLocation() + File.separator + fileName;
}

public boolean isInSync() {
	File file = new File(getFullPath());
	return isInSync(file);
}

public void setInstallLocation(String newInstallLocation) {
	installLocation = newInstallLocation;
}
}
