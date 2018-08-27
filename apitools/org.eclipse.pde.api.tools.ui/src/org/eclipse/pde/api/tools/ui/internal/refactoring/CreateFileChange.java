/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.ui.internal.refactoring;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.ltk.core.refactoring.resource.ResourceChange;
import org.eclipse.osgi.util.NLS;

public class CreateFileChange extends ResourceChange {

	private String fChangeName;

	private IPath fPath;
	private String fSource;
	private String fEncoding;
	private boolean fExplicitEncoding;
	private long fStampToRestore;

	public CreateFileChange(IPath path, String source, String encoding) {
		this(path, source, encoding, IResource.NULL_STAMP);
	}

	public CreateFileChange(IPath path, String source, String encoding, long stampToRestore) {
		Assert.isNotNull(path, "path"); //$NON-NLS-1$
		Assert.isNotNull(source, "source"); //$NON-NLS-1$
		fPath = path;
		fSource = source;
		fEncoding = encoding;
		fExplicitEncoding = fEncoding != null;
		fStampToRestore = stampToRestore;
	}

	protected void setEncoding(String encoding, boolean explicit) {
		Assert.isNotNull(encoding, "encoding"); //$NON-NLS-1$
		fEncoding = encoding;
		fExplicitEncoding = explicit;
	}

	@Override
	public String getName() {
		if (fChangeName == null) {
			return NLS.bind(RefactoringMessages.CreateFileChange_0, fPath);
		} else {
			return fChangeName;
		}
	}

	public void setName(String name) {
		fChangeName = name;
	}

	protected void setSource(String source) {
		fSource = source;
	}

	protected String getSource() {
		return fSource;
	}

	protected void setPath(IPath path) {
		fPath = path;
	}

	protected IPath getPath() {
		return fPath;
	}

	@Override
	protected IResource getModifiedResource() {
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
	}

	@Override
	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		RefactoringStatus result = new RefactoringStatus();
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);

		URI location = file.getLocationURI();
		if (location == null) {
			result.addFatalError(NLS.bind(RefactoringMessages.CreateFileChange_1, file.getFullPath()));
			return result;
		}

		IFileInfo jFile = EFS.getStore(location).fetchInfo();
		if (jFile.exists()) {
			result.addFatalError(NLS.bind(RefactoringMessages.CreateFileChange_2, file.getFullPath()));
			return result;
		}
		return result;
	}

	@Override
	public Change perform(IProgressMonitor pm) throws CoreException, OperationCanceledException {

		SubMonitor subMonitor = SubMonitor.convert(pm, RefactoringMessages.CreateFileChange_3, 3);

		try {
			initializeEncoding();
			IFile file = getOldFile(subMonitor.split(1));
			try (InputStream is = new ByteArrayInputStream(fSource.getBytes(fEncoding))) {
				file.create(is, false, subMonitor.split(1));
				if (fStampToRestore != IResource.NULL_STAMP) {
					file.revertModificationStamp(fStampToRestore);
				}
				if (fExplicitEncoding) {
					file.setCharset(fEncoding, subMonitor.split(1));
				} else {
					subMonitor.worked(1);
				}
				return new DeleteResourceChange(file.getFullPath(), true);
			} catch (UnsupportedEncodingException e) {
				throw new JavaModelException(e, IJavaModelStatusConstants.IO_EXCEPTION);
			}
		} catch (IOException ioe) {
			throw new JavaModelException(ioe, IJavaModelStatusConstants.IO_EXCEPTION);
		}
	}

	protected IFile getOldFile(IProgressMonitor pm) throws OperationCanceledException {
		SubMonitor.convert(pm, 1);
		return ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
	}

	private void initializeEncoding() {
		if (fEncoding == null) {
			fExplicitEncoding = false;
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(fPath);
			if (file != null) {
				try {
					if (file.exists()) {
						fEncoding = file.getCharset(false);
						if (fEncoding == null) {
							fEncoding = file.getCharset(true);
						} else {
							fExplicitEncoding = true;
						}
					} else {
						IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(file.getName());
						if (contentType != null) {
							fEncoding = contentType.getDefaultCharset();
						}
						if (fEncoding == null) {
							fEncoding = file.getCharset(true);
						}
					}
				} catch (CoreException e) {
					fEncoding = ResourcesPlugin.getEncoding();
					fExplicitEncoding = true;
				}
			} else {
				fEncoding = ResourcesPlugin.getEncoding();
				fExplicitEncoding = true;
			}
		}
		Assert.isNotNull(fEncoding);
	}
}
