package org.eclipse.pde.internal.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.*;

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class WorkspaceBuildModel extends BuildModel implements IEditable {
	private IFile file;
	private boolean dirty;
	private boolean editable = true;

public WorkspaceBuildModel(IFile file) {
	this.file = file;
}
public void fireModelChanged(IModelChangedEvent event) {
	dirty = true;
	super.fireModelChanged(event);
}
public String getContents() {
	StringWriter swriter = new StringWriter();
	PrintWriter writer = new PrintWriter(swriter);
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
		InputStream stream = null;
		boolean outOfSync=false;
		try {
			stream = file.getContents(false);
		}
		catch (CoreException e) {
			outOfSync = true;
		}
		if (outOfSync) {
			try {
				stream = file.getContents(true);
			}
			catch (CoreException e) {
				return;
			}
		}
		try {			
			load(stream, outOfSync);
			stream.close();
		}
		catch (IOException e) {
			PDEPlugin.logException(e);
		}
	}
	else {
		build = new Build();
		build.setModel(this);
		loaded=true;
	}
}

public boolean isInSync() {
	return isInSync(file.getLocation().toFile());
}

protected void updateTimeStamp() {
	updateTimeStamp(file.getLocation().toFile());
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
	getBuild().write("", writer);
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
