package org.eclipse.pde.internal.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.resource.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;

public class MappedSchemaDescriptor extends AbstractSchemaDescriptor {
	public static final String ATT_SCHEMA = "schema";
	public static final String ATT_POINT = "point";
	
	private IConfigurationElement config;

public MappedSchemaDescriptor(IConfigurationElement config) {
	this.config = config;
}
public ImageDescriptor createImageDescriptor(String imageName) {
	if (imageName.indexOf(':') != -1)
		return createAbsoluteImageDescriptor(imageName);
	IPluginDescriptor pd =
		config.getDeclaringExtension().getDeclaringPluginDescriptor();
	URL url = null;
	try {
		url = pd.getInstallURL();
		url = new URL(url, imageName);
	} catch (MalformedURLException e) {
		url = null;
	}
	if (url != null)
		return ImageDescriptor.createFromURL(url);
	else
		return null;
}
public String getPointId() {
	return config.getAttribute(ATT_POINT);
}
public URL getSchemaURL() {
	String schemaName = config.getAttribute(ATT_SCHEMA);
	URL pluginURL =
		config.getDeclaringExtension().getDeclaringPluginDescriptor().getInstallURL();
	URL newURL = null;
	try {
		newURL = new URL(pluginURL.toString() + schemaName);
	} catch (MalformedURLException e) {
	}
	return newURL;
}

public boolean isEnabled() {
	return true;
}
}
