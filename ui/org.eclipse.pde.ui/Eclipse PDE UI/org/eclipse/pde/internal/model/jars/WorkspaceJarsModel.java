package org.eclipse.pde.internal.model.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.PDEPlugin;

public class WorkspaceJarsModel extends JarsModel implements IEditable {
	private IFile file;
	private boolean dirty;
	private boolean editable = true;

public WorkspaceJarsModel(IFile file) {
	this.file = file;
}
public void fireModelChanged(IModelChangedEvent event) {
	dirty = true;
	super.fireModelChanged(event);
}
public String getContents() {
	ByteArrayOutputStream bstream = new ByteArrayOutputStream();
	PrintWriter writer = new PrintWriter(bstream);
	save(writer);
	writer.flush();
	try {
		bstream.close();
	} catch (IOException e) {
	}
	return bstream.toString();
}
public IFile getFile() {
	return file;
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
		jars = new Jars();
		jars.setModel(this);
		loaded = true;
	}
}
public void save() {
	if (file == null)
		return;
	try {
		String contents = getContents();
		ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
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
	getJars().write("", writer);
	dirty = false;
}
public void setDirty(boolean newDirty) {
	dirty = newDirty;
}
public void setEditable(boolean newEditable) {
	editable = newEditable;
}
public void setFile(IFile newFile) {
	file = newFile;
}
}
