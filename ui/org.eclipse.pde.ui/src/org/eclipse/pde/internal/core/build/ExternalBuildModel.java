package org.eclipse.pde.internal.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.io.*;
import java.net.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class ExternalBuildModel extends BuildModel {
	private String installLocation;
	private long timeStamp;

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
