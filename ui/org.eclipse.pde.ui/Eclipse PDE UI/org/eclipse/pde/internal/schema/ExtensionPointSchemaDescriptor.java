package org.eclipse.pde.internal.schema;

import org.eclipse.jface.resource.*;
import java.net.*;
import org.eclipse.pde.internal.base.model.plugin.*;


public class ExtensionPointSchemaDescriptor extends AbstractSchemaDescriptor {
	private IPluginExtensionPoint info;

public ExtensionPointSchemaDescriptor(IPluginExtensionPoint info) {
	this.info = info;
}
public ImageDescriptor createImageDescriptor(String imageName) {
	if (imageName.indexOf(':') != -1) return createAbsoluteImageDescriptor(imageName);
	return null;
}
public String getPointId() {
	return info.getFullId();
}
public URL getSchemaURL() {
	return null;
}
}
