package org.eclipse.pde.internal.model.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.io.*;
import java.net.*;
import org.eclipse.pde.internal.PDEPlugin;

public class ExternalJarsModel extends JarsModel {
	private String installLocation;

public ExternalJarsModel(String installLocation) {
	this.installLocation = installLocation;
}
public String getInstallLocation() {
	return installLocation;
}
public boolean isEditable() {
	return false;
}
public void load() {
	String fileName = isFragment() ? "fragment.jars" : "plugin.jars";
	String location = getInstallLocation() + File.separator + fileName;
	try {
		URL url = new URL(location);
		InputStream stream = url.openStream();
		load(stream);
		stream.close();
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
}
public void setInstallLocation(String newInstallLocation) {
	installLocation = newInstallLocation;
}
}
