/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.net.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;

public class WorkspaceSiteModel
	extends AbstractSiteModel
	implements IEditableModel {
	private static final long serialVersionUID = 1L;
	private boolean fDirty;
	private IFile file;
	private boolean editable = true;

	public WorkspaceSiteModel() {
		super();
	}
	public WorkspaceSiteModel(IFile file) {
		setFile(file);
	}
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType()!=IModelChangedEvent.WORLD_CHANGED);
		super.fireModelChanged(event);
	}

	protected NLResourceHelper createNLResourceHelper() {
		try {
			IPath path = file.getLocation().removeLastSegments(1);
			String installLocation = path.toOSString();
			if (installLocation.startsWith("file:") == false) //$NON-NLS-1$
				installLocation = "file:" + installLocation; //$NON-NLS-1$
			URL url = new URL(installLocation + "/"); //$NON-NLS-1$
			String name = "site"; //$NON-NLS-1$
			NLResourceHelper helper =
				new NLResourceHelper(name, new URL[] { url });
			return helper;
		} catch (MalformedURLException e) {
			return null;
		}
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
		return fDirty;
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
			this.site = new Site();
			site.model = this;
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
			//writer.println("<!DOCTYPE site SYSTEM \"dtd/site.dtd\">");
			site.write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}
	public void setEditable(boolean newEditable) {
		editable = newEditable;
	}
	public void setFile(IFile newFile) {
		file = newFile;
	}
}
