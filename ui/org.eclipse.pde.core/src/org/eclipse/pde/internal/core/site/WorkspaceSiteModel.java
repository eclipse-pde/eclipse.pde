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
	private IFile fFile;
	private boolean fEditable = true;

	public WorkspaceSiteModel(IFile file) {
		fFile = file;
	}
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType()!=IModelChangedEvent.WORLD_CHANGED);
		super.fireModelChanged(event);
	}

	protected NLResourceHelper createNLResourceHelper() {
		try {
			IPath path = fFile.getLocation().removeLastSegments(1);
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
		return fFile;
	}
	public String getInstallLocation() {
		return fFile.getParent().getLocation().toOSString();
	}
	public IResource getUnderlyingResource() {
		return fFile;
	}
	public boolean isDirty() {
		return fDirty;
	}
	public boolean isEditable() {
		return fEditable;
	}

	public boolean isInSync() {
		return isInSync(fFile.getLocation().toFile());
	}

	protected void updateTimeStamp() {
		updateTimeStamp(fFile.getLocation().toFile());
	}
	public void load() {
		if (fFile.exists()) {
			InputStream stream = null;
			try {
				stream = fFile.getContents(true);
				load(stream, false);
			} catch (CoreException e) {
			} finally {
				try {
					if (stream != null)
						stream.close();
				} catch (IOException e) {
				}
			}
		} else {
			this.site = new Site();
			site.model = this;
			setLoaded(true);
		}
	}
	public void save() {
		try {
			String contents = getContents();
			ByteArrayInputStream stream =
				new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
			if (fFile.exists()) {
				fFile.setContents(stream, false, false, null);
			} else {
				fFile.create(stream, false, null);
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
			site.write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean newEditable) {
		fEditable = newEditable;
	}
}
