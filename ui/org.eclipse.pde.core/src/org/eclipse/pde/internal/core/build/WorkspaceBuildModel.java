/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;

public class WorkspaceBuildModel extends BuildModel implements IEditableModel {
	private static final long serialVersionUID = 1L;
	private IFile fUnderlyingResource;
	private boolean fDirty;
	private boolean fEditable = true;

	public WorkspaceBuildModel(IFile file) {
		fUnderlyingResource = file;
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(event.getChangeType() != IModelChangedEvent.WORLD_CHANGED);
		super.fireModelChanged(event);
	}

	public String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		save(writer);
		writer.flush();
		try {
			swriter.close();
			writer.close();
		} catch (IOException e) {
			PDECore.logException(e);
		}
		return swriter.toString();
	}

	@Override
	public IResource getUnderlyingResource() {
		return fUnderlyingResource;
	}

	@Override
	public boolean isDirty() {
		return fDirty;
	}

	@Override
	public boolean isEditable() {
		return fEditable;
	}

	@Override
	public void load() {
		if (fUnderlyingResource.exists()) {
			InputStream stream = null;
			try {
				stream = fUnderlyingResource.getContents(true);
				load(stream, false);
			} catch (Exception e) {
				PDECore.logException(e);
			} finally {
				try {
					if (stream != null)
						stream.close();
				} catch (IOException e) {
					PDECore.logException(e);
				}
			}
		} else {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		}
	}

	@Override
	public boolean isInSync() {
		return true;
	}

	@Override
	protected void updateTimeStamp() {
		updateTimeStamp(fUnderlyingResource.getLocation().toFile());
	}

	@Override
	public void save() {
		if (fUnderlyingResource == null)
			return;
		ByteArrayInputStream stream = null;
		try {
			String contents = fixLineDelimiter(getContents(), fUnderlyingResource);
			stream = new ByteArrayInputStream(contents.getBytes("8859_1")); //$NON-NLS-1$
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
			PDECore.logException(e);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
				PDECore.logException(e);
			}
		}
	}

	@Override
	public void save(PrintWriter writer) {
		getBuild().write("", writer); //$NON-NLS-1$
		fDirty = false;
	}

	@Override
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	public void setEditable(boolean editable) {
		fEditable = editable;
	}

	@Override
	public String getInstallLocation() {
		return fUnderlyingResource.getLocation().toOSString();
	}
}
