package org.eclipse.pde.internal.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.PDEPlugin;

public class WorkspaceComponentModel extends AbstractComponentModel implements IEditable {
	private boolean dirty;
	private IFile file;
	private boolean editable;

public WorkspaceComponentModel() {
	super();
}
public WorkspaceComponentModel(IFile file) {
	this.file = file;
}
public void fireModelChanged(IModelChangedEvent event) {
	dirty = true;
	super.fireModelChanged(event);
}
public String getContents() {
	StringWriter swriter = new StringWriter();
	PrintWriter writer = new PrintWriter(swriter);
	loaded=true;
	save(writer);
	writer.flush();
	try {
		swriter.close();
	} catch (IOException e) {
	}
	return swriter.toString();
}
public IFile getFile() {
	return file;
}
public String getInstallLocation() {
	return Platform.getLocation().toOSString();
}
public IResource getUnderlyingResource() {
	return file;
}
public boolean isDirty() {
	return dirty;
}
public boolean isEditable() {
	return editable;
}
public void load() {
	if (file == null)
		return;
	if (file.exists()) {
		try {
			InputStream stream = file.getContents(false);
			load(stream);
			stream.close();
		} catch (CoreException e) {
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	} else {
		this.component = new Component();
		component.model = this;
		loaded = true;
	}
}
public void save() {
	if (file == null)
		return;
	try {
		String contents = getContents();
		ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes("UTF8"));
		if (file.exists()) {
			file.setContents(stream, false, false, null);
		} else {
			file.create(stream, false, null);
		}
		stream.close();
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	} catch (IOException e) {
	}
}
public void save(PrintWriter writer) {
	if (isLoaded()) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<!-- File written by PDE 1.0 -->");
		component.write("", writer);
	}
	dirty = false;
}
public void setDirty(boolean dirty) {
	this.dirty = dirty;
}
public void setEditable(boolean newEditable) {
	editable = newEditable;
}
public void setFile(IFile newFile) {
	file = newFile;
}
}
