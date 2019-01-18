/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IEditableModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;

public class WorkspaceBuildModel extends BuildModel implements IEditableModel {
	private static final long serialVersionUID = 1L;
	private final IFile fUnderlyingResource;
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
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			save(writer);
			writer.flush();
			return swriter.toString();
		} catch (IOException e) {
			PDECore.logException(e);
			return ""; //$NON-NLS-1$
		}
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
			try (InputStream stream = fUnderlyingResource.getContents(true)) {
				load(stream, false);
			} catch (Exception e) {
				PDECore.logException(e);
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
		if (fUnderlyingResource == null) {
			return;
		}
		String contents = fixLineDelimiter(getContents(), fUnderlyingResource);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.ISO_8859_1))) {
			if (fUnderlyingResource.exists()) {
				fUnderlyingResource.setContents(stream, false, false, null);
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException | IOException e) {
			PDECore.logException(e);
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
