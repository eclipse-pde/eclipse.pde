package org.eclipse.pde.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;
import java.io.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.jars.*;
import org.eclipse.pde.internal.base.model.build.*;
import org.eclipse.pde.internal.PDEPlugin;
import java.net.*;

public abstract class WorkspacePluginModelBase extends AbstractPluginModelBase implements IEditable {
	private IFile file;
	private boolean dirty;
	private boolean editable = true;
	private IBuildModel buildModel;

public WorkspacePluginModelBase() {
	this(null);
}

protected NLResourceHelper createNLResourceHelper() {
	try {
		IPath path = file.getLocation().removeLastSegments(1);
		String installLocation = path.toOSString();
		if (installLocation.startsWith("file:")==false)
		   installLocation = "file:"+installLocation;
		URL url = new URL(installLocation+"/");
		String name = isFragmentModel() ? "fragment" : "plugin";
		WorkspaceResourceHelper helper = new WorkspaceResourceHelper(name, url);
		helper.setFile(file);
		return helper;
	} catch (MalformedURLException e) {
		return null;
	}
}

public WorkspacePluginModelBase(IFile file) {
	this.file = file;
	setEnabled(true);
}
public void fireModelChanged(IModelChangedEvent event) {
	dirty = true;
	super.fireModelChanged(event);
}
public IBuildModel getBuildModel() {
	return buildModel;
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

public void dispose() {
	super.dispose();
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
		pluginBase = (PluginBase) createPluginBase();
		pluginBase.setModel(this);
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
	if (isLoaded()) {
		pluginBase.write("", writer);
	}
	dirty = false;
}
public void setBuildModel(IBuildModel newBuildModel) {
	buildModel = newBuildModel;
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
