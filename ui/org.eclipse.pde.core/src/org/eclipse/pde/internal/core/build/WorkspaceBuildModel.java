/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.SequenceInputStream;
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
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		save(writer);
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

	private String getLineDelimiter(IFile f) {
		String lineDelimiter = getLineDelimiterPreference(f);
		if (lineDelimiter == null) {
			lineDelimiter = System.lineSeparator();
		}
		return lineDelimiter;

	}

	private StringBuilder getHeaderComments(IFile f) throws IOException, CoreException {
		StringBuilder str = null;
		String lineDelimiter = getLineDelimiter(f);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(f.getContents()))) {
			for (String line; (line = br.readLine()) != null;) {
				if (line.startsWith("#")) { //$NON-NLS-1$
					if (str == null) {
						str = new StringBuilder();
					}
					str.append(line + lineDelimiter);
				} else {
					break;
				}
			}
		}
		// returns null for no header comment case
		return str;
	}

	@Override
	public void save() {
		if (fUnderlyingResource == null) {
			return;
		}
		String contents = fixLineDelimiter(getContents(), fUnderlyingResource);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.ISO_8859_1))) {
			if (fUnderlyingResource.exists()) {

				StringBuilder str = getHeaderComments(fUnderlyingResource);
				if (str != null) {
					ByteArrayInputStream headerComment = new ByteArrayInputStream(str.toString().getBytes());
					try (InputStream totalContent = new SequenceInputStream(headerComment, stream)) {
						fUnderlyingResource.setContents(totalContent, false, false, null);
					}
				} else {
					fUnderlyingResource.setContents(stream, false, false, null);
				}
			} else {
				fUnderlyingResource.create(stream, false, null);
			}
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
