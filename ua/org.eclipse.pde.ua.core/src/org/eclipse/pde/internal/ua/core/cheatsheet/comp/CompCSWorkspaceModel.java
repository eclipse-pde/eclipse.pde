/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.*;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.IWorkspaceModel;
import org.eclipse.pde.internal.core.PDECore;

public class CompCSWorkspaceModel extends CompCSModel implements
		IWorkspaceModel {

	private IFile fFile;

	private boolean fDirty;

	private boolean fEditable;

	private static final long serialVersionUID = 1L;

	public CompCSWorkspaceModel(IFile file, boolean editable) {
		fFile = file;
		fEditable = editable;
	}

	@Override
	public void save() {
		String contents = getContents();
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
			if (fFile.exists()) {
				fFile.setContents(stream, false, false, null);
			} else {
				fFile.create(stream, false, null);
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
			// Ignore
		}
	}

	private String getContents() {
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			setLoaded(true);
			save(writer);
			writer.flush();
			return swriter.toString();
		} catch (IOException e) {
			return "";
		}
	}

	@Override
	public boolean isDirty() {
		return fDirty;
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			getCompCS().write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	@Override
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	@Override
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(true);
		super.fireModelChanged(event);
	}

	@Override
	public boolean isEditable() {
		return fEditable;
	}

	public String getInstallLocation() {
		return fFile.getLocation().toOSString();
	}

	@Override
	public IResource getUnderlyingResource() {
		return fFile;
	}

	@Override
	public boolean isInSync() {
		IPath path = fFile.getLocation();
		if (path == null) {
			return false;
		}
		return isInSync(path.toFile());
	}

	@Override
	public void load() throws CoreException {
		if (fFile.exists()) {
			try (InputStream stream = new BufferedInputStream(fFile.getContents(true));) {
				if (stream.available() > 0)
					load(stream, false);
				else {
					// if we have an empty file, then mark as loaded so
					// users changes will be saved
					setLoaded(true);
					stream.close();
				}
			} catch (IOException | CoreException e) {
			}
		}
	}

	@Override
	public void reload() {
		// Underlying file has to exist in order to reload the model
		if (fFile.exists()) {
			InputStream stream = null;
			try {
				// Get the file contents
				stream = new BufferedInputStream(fFile.getContents(true));
				// Load the model using the last saved file contents
				reload(stream, false);
				// Remove the dirty (*) indicator from the editor window
				setDirty(false);
			} catch (CoreException e) {
				// Ignore
			}
		}
	}
}
