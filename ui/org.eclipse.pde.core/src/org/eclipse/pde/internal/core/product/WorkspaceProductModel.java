/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.product;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.IWorkspaceModel;
import org.eclipse.pde.internal.core.PDECore;

public class WorkspaceProductModel extends ProductModel implements IWorkspaceModel {

	private static final long serialVersionUID = 1L;

	private final IFile fFile;

	private boolean fDirty;

	private final boolean fEditable;

	public WorkspaceProductModel(IFile file, boolean editable) {
		fFile = file;
		fEditable = editable;
	}

	@Override
	public void load() throws CoreException {
		if (fFile.exists()) {
			try (InputStream stream = new BufferedInputStream(fFile.getContents(true))) {
					if (stream.available() > 0) {
						load(stream, false);
					} else {
						// if we have an empty file, then mark as loaded so users changes will be saved
						setLoaded(true);
					}
			} catch (CoreException | IOException e) {
				PDECore.logException(e);
			}
		}
	}

	@Override
	public boolean isInSync() {
		IPath path = fFile.getLocation();
		return path == null ? false : isInSync(path.toFile());
	}

	@Override
	public IResource getUnderlyingResource() {
		return fFile;
	}

	@Override
	public String getInstallLocation() {
		return fFile.getLocation().toOSString();
	}

	@Override
	public void save() {
		String contents = fixLineDelimiter(getContents(), fFile);
		try (ByteArrayInputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
			if (fFile.exists()) {
				fFile.setContents(stream, false, false, null);
			} else {
				fFile.create(stream, false, null);
			}
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public String getContents() {
		try (StringWriter swriter = new StringWriter(); PrintWriter writer = new PrintWriter(swriter)) {
			setLoaded(true);
			save(writer);
			writer.flush();
			return swriter.toString();
		} catch (IOException e) {
			PDECore.logException(e);
			return ""; //$NON-NLS-1$
		}
	}

	@Override
	public boolean isDirty() {
		return fDirty;
	}

	@Override
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
			writer.println("<?pde version=\"3.5\"?>"); //$NON-NLS-1$
			writer.println();
			getProduct().write("", writer); //$NON-NLS-1$
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
