package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.PDEPlugin;

public class WorkspaceFeatureModel extends AbstractFeatureModel implements IEditable {
	private boolean dirty;
	private IFile file;
	private boolean editable;

public WorkspaceFeatureModel() {
	super();
}
public WorkspaceFeatureModel(IFile file) {
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
	return file.getParent().getLocation().toOSString();
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

public boolean isInSync() {
	return isInSync(file.getLocation().toFile());
}

protected void updateTimeStamp() {
	updateTimeStamp(file.getLocation().toFile());
}
public void load() {
	if (file == null)
		return;
	if (file.exists()) {
		boolean outOfSync = false;
		InputStream stream = null;
		try {
			stream = file.getContents(false);
		}
		catch (CoreException e) {
			outOfSync = true;
			try {
				stream = file.getContents(true);
			}
			catch (CoreException ex) {
				return;
			}
		}
		try {
			load(stream, outOfSync);
			stream.close();
		} catch (CoreException e) {
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}
	} else {
		this.feature = new Feature();
		feature.model = this;
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
		feature.write("", writer);
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
