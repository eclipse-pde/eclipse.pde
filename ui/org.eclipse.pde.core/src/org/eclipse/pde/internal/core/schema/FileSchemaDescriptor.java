package org.eclipse.pde.internal.core.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

public class FileSchemaDescriptor extends AbstractSchemaDescriptor {
	private IFile file;

public FileSchemaDescriptor(IFile file) {
	this.file = file;
}

protected Schema createSchema() {
	URL url = getSchemaURL();
	if (url==null) return null;
	return new EditableSchema(this, url);
}
public IFile getFile() {
	return file;
}
private URL getInstallURL() {
	IPath platformPath = Platform.getLocation();
	try {
		return new URL("file:" + platformPath.toString());
	} catch (MalformedURLException e) {
	}
	return null;
}
public String getPointId() {
	IProject project = file.getProject();
	String projectName = project.getName();
	String fileName = file.getName();
	int dotLoc = fileName.lastIndexOf('.');
	return projectName + "."+fileName.substring(0, dotLoc);
}
public URL getSchemaURL() {
	URL url = getInstallURL();
	try {
		return new URL(url.toString() + file.getFullPath().toString());
	} catch (MalformedURLException e) {
	}
	return null;
}

public boolean isEnabled() {
	return true;
}
}