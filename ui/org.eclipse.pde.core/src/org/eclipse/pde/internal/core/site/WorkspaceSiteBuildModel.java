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
package org.eclipse.pde.internal.core.site;

import java.io.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;

public class WorkspaceSiteBuildModel
	extends AbstractSiteBuildModel
	implements IEditableModel {
	private boolean dirty;
	private IFile file;
	private boolean editable = true;

	public WorkspaceSiteBuildModel() {
	}
	public WorkspaceSiteBuildModel(IFile file) {
		setFile(file);
	}
	public void fireModelChanged(IModelChangedEvent event) {
		dirty = true;
		super.fireModelChanged(event);
	}

	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		loaded = true;
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
			} catch (CoreException e) {
				outOfSync = true;
				try {
					stream = file.getContents(true);
				} catch (CoreException ex) {
					return;
				}
			}
			try {
				load(stream, outOfSync);
				stream.close();
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.logException(e);
			}
		} else {
			this.siteBuild = new SiteBuild();
			siteBuild.model = this;
			loaded = true;
		}
	}
	public void save() {
		if (file == null)
			return;
		try {
			String contents = getContents();
			ByteArrayInputStream stream =
				new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
				createFolder(file, null);
				file.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
		}
	}
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			siteBuild.write("", writer); //$NON-NLS-1$
		}
		dirty = false;
	}
	private void createFolder(IFile file, IProgressMonitor monitor) throws CoreException {
		if (file.exists())
			return;
		IProject project = file.getProject();
		IPath path = file.getProjectRelativePath();
		for (int i = path.segmentCount()-1; i>0; i--){
			IFolder folder = project.getFolder(path.removeLastSegments(i));
			if (!folder.exists())
				folder.create(true, true, monitor);
		}
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
