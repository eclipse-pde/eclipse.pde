/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.internal.core.PDECore;

/**
 * A handle to a target stored in the workspace as a <code>.target</code> file.
 *
 * @since 3.5
 */
public class WorkspaceFileTargetHandle extends AbstractTargetHandle {

	private IFile fFile;

	/**
	 * Scheme for resource target handle
	 */
	static final String SCHEME = "resource"; //$NON-NLS-1$

	/**
	 * Map of all target editor file and the workspace editor
	 */
	public static HashMap<IFile, Object> mapFileTarget = new HashMap<>();

	/**
	 * Returns a handle for the given URI.
	 *
	 * @param uri URI
	 * @return target handle
	 */
	static ITargetHandle restoreHandle(URI uri) {
		String part = uri.getSchemeSpecificPart();
		Path path = new Path(part);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		return new WorkspaceFileTargetHandle(file);
	}

	/**
	 * Constructs a handle to a target in the given file.
	 *
	 * @param file underlying file - may or may not exist
	 */
	public WorkspaceFileTargetHandle(IFile file) {
		fFile = file;
	}

	@Override
	public String getMemento() throws CoreException {
		try {
			URI uri = new URI(SCHEME, fFile.getFullPath().toPortableString(), null);
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.WorkspaceFileTargetHandle_0, e));
		}
	}

	@Override
	protected ITextFileBuffer getTextFileBuffer() throws CoreException {
		if (!fFile.exists()) {
			fFile.create(new ByteArrayInputStream(new byte[0]), false, null);
		} else {
			// validate edit
			if (fFile.isReadOnly()) {
				IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { fFile }, null);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
			}
		}
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		manager.connect(fFile.getFullPath(), LocationKind.IFILE, null);
		return manager.getTextFileBuffer(fFile.getFullPath(), LocationKind.IFILE);
	}

	@Override
	public boolean exists() {
		return fFile.exists();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WorkspaceFileTargetHandle) {
			WorkspaceFileTargetHandle handle = (WorkspaceFileTargetHandle) obj;
			return fFile.equals(handle.fFile);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fFile.hashCode() + getClass().hashCode();
	}

	@Override
	void delete() throws CoreException {
		if (fFile.exists()) {
			fFile.delete(false, null);
		}
		P2TargetUtils.deleteProfile(this);
	}

	@Override
	public void save(ITargetDefinition definition) throws CoreException {
		((TargetDefinition) definition).write(getTextFileBuffer());
		if (fFile.exists()) {
			fFile.refreshLocal(IResource.DEPTH_ZERO, null);
		}
	}

	@Override
	public String toString() {
		return fFile.getName();
	}

	/**
	 * Returns the target file.
	 *
	 * @return target file
	 */
	public IFile getTargetFile() {
		return fFile;
	}

	/**
	 * Returns the workspace editor from target file.
	 *
	 * @return target
	 */
	public Object getWorkspaceEditor() {
		return mapFileTarget.get(fFile);
	}

	/**
	 * Updates the map with the file and workspace target
	 */
	public void setWorkspaceEditor(Object target) {
		mapFileTarget.put(fFile, target);
	}
}
