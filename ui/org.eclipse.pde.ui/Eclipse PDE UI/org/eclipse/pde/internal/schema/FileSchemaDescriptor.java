package org.eclipse.pde.internal.schema;

import org.eclipse.core.resources.*;
import org.eclipse.jface.resource.*;
import java.net.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.*;

public class FileSchemaDescriptor extends AbstractSchemaDescriptor {
	private IFile file;

public FileSchemaDescriptor(IFile file) {
	this.file = file;
}
public ImageDescriptor createImageDescriptor(String imageName) {
	if (imageName.indexOf(':') != -1)
		return createAbsoluteImageDescriptor(imageName);
	URL url = null;
	try {
		url = getInstallURL();
		url = new URL(url, imageName);
	} catch (MalformedURLException e) {
		url = null;
	}
	if (url != null)
		return ImageDescriptor.createFromURL(url);
	else
		return null;
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
}
