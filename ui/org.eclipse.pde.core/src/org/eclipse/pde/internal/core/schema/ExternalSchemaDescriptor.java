package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.resource.*;
import java.net.*;
import org.eclipse.pde.core.plugin.*;
import java.io.File;


public class ExternalSchemaDescriptor extends AbstractSchemaDescriptor {
	private IPluginExtensionPoint info;

public ExternalSchemaDescriptor(IPluginExtensionPoint info) {
	this.info = info;
}
public ImageDescriptor createImageDescriptor(String imageName) {
	if (imageName.indexOf(':') != -1) return createAbsoluteImageDescriptor(imageName);
	return null;
}
public String getPointId() {
	return info.getFullId();
}

private URL getInstallURL() {
	IPluginModelBase model = info.getModel();
	String installLocation = model.getInstallLocation()+File.separator;
	try {
		return new URL("file:" + installLocation);
	} catch (MalformedURLException e) {
	}
	return null;
}

public URL getSchemaURL() {
	URL url = getInstallURL();
	if (url==null) return null;
	try {
		return new URL(url, info.getSchema());
	} catch (MalformedURLException e) {
	}
	return null;
}

public boolean isEnabled() {
	return info.getModel().isEnabled();
}
}
