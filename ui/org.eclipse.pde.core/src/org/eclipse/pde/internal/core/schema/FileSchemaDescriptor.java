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

import java.net.*;

import org.eclipse.core.resources.*;

public class FileSchemaDescriptor extends DevelopmentSchemaDescriptor {
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

public String getPointId() {
	IProject project = file.getProject();
	String projectName = project.getName();
	String fileName = file.getName();
	int dotLoc = fileName.lastIndexOf('.');
	return projectName + "."+fileName.substring(0, dotLoc); //$NON-NLS-1$
}

public URL getSchemaURL() {
	try {
/*
		return new URL(
			"file:"
				+ file.getProject().getLocation().removeLastSegments(1)
				+ file.getFullPath().toString());
*/
		return new URL("file:"+file.getLocation().toOSString()); //$NON-NLS-1$
	} catch (MalformedURLException e) {
	}
	return null;
}

public boolean isEnabled() {
	return true;
}
}
