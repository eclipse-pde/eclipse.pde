/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;

public class WorkspaceFeatureModel extends AbstractFeatureModel
		implements
			IEditableModel {
	private static final long serialVersionUID = 1L;
	private boolean dirty;
	private IFile file;
	private boolean editable = true;

	public WorkspaceFeatureModel() {
		super();
	}
	public WorkspaceFeatureModel(IFile file) {
		setFile(file);
	}
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType() != IModelChangedEvent.WORLD_CHANGED);
		super.fireModelChanged(event);
	}

	protected NLResourceHelper createNLResourceHelper() {
		try {
			IPath path = file.getLocation().removeLastSegments(1);
			String installLocation = path.toOSString();
			if (installLocation.startsWith("file:") == false) //$NON-NLS-1$
				installLocation = "file:" + installLocation; //$NON-NLS-1$
			URL url = new URL(installLocation + "/"); //$NON-NLS-1$
			String name = "feature"; //$NON-NLS-1$
			NLResourceHelper helper = new NLResourceHelper(name, new URL[]{url});
			//helper.setFile(file);
			return helper;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		setLoaded(true);
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
			try {
				InputStream stream = file.getContents(true);
				load(stream, false);
				stream.close();
			} catch (CoreException e) {
			} catch (IOException e) {
				PDECore.logException(e);
			}
		} else {
			this.feature = new Feature();
			feature.model = this;
			setLoaded(true);
		}
	}
	public void save() {
		if (file == null)
			return;
		try {
			String contents = getContents();
			ByteArrayInputStream stream = new ByteArrayInputStream(contents
					.getBytes("UTF8")); //$NON-NLS-1$
			if (file.exists()) {
				file.setContents(stream, false, false, null);
			} else {
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
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			//writer.println("<!DOCTYPE feature SYSTEM \"dtd/feature.dtd\">");
			feature.write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}
	public void setEditable(boolean newEditable) {
		editable = newEditable;
	}
	public void setFile(IFile newFile) {
		file = newFile;
		//setEditable(newFile.isReadOnly()==false);
	}
}
