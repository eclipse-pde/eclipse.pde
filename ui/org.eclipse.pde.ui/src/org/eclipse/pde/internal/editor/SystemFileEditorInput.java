package org.eclipse.pde.internal.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.*;
import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.*;

public class SystemFileEditorInput implements IStorageEditorInput {
	private File file;

public SystemFileEditorInput(File file) {
	this.file = file;
}
public boolean exists() {
	return false;
}
public Object getAdapter(Class adapter) {
	if (adapter.equals(File.class)) return file;
	return null;
}
public InputStream getContents() throws CoreException {
	InputStream stream = null;
	try {
	   stream = new FileInputStream(file);
	}
	catch (IOException e) {
		Status status = new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, e.getMessage(), e);
		throw new CoreException(status);
	}
	return stream;
}
public org.eclipse.jface.resource.ImageDescriptor getImageDescriptor() {
	return null;
}
public String getName() {
	return file.getName();
}
public IPersistableElement getPersistable() {
	return null;
}
public org.eclipse.core.resources.IStorage getStorage() throws org.eclipse.core.runtime.CoreException {
	return null;
}
public String getToolTipText() {
	return getName();
}
}
