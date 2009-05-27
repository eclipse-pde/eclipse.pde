/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import com.ibm.icu.text.MessageFormat;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public final class RenameProjectChange extends Change {

	public static IPath renamedResourcePath(IPath path, String newName) {
		return path.removeLastSegments(1).append(newName);
	}

	private final String fComment;

	private final RefactoringDescriptor fDescriptor;

	private final String fNewName;

	private final IPath fResourcePath;

	private final long fStampToRestore;

	private RenameProjectChange(RefactoringDescriptor descriptor, IPath resourcePath, String newName, String comment, long stampToRestore) {
		fDescriptor = descriptor;
		fResourcePath = resourcePath;
		fNewName = newName;
		fComment = comment;
		fStampToRestore = stampToRestore;
	}

	public RenameProjectChange(RefactoringDescriptor descriptor, IResource resource, String newName, String comment) {
		this(descriptor, resource.getFullPath(), newName, comment, IResource.NULL_STAMP);
	}

	public ChangeDescriptor getDescriptor() {
		if (fDescriptor != null)
			return new RefactoringChangeDescriptor(fDescriptor);
		return null;
	}

	public Object getModifiedElement() {
		return getResource();
	}

	public String getName() {
		return MessageFormat.format(PDEUIMessages.RenameProjectChange_name, new String[] {fResourcePath.lastSegment(), fNewName});
	}

	public String getNewName() {
		return fNewName;
	}

	private IResource getResource() {
		return ResourcesPlugin.getWorkspace().getRoot().findMember(fResourcePath);
	}

	public RefactoringStatus isValid(IProgressMonitor pm) throws CoreException {
		IResource resource = getResource();
		if (resource == null || !resource.exists())
			return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(PDEUIMessages.RenameProjectChange_projectDoesNotExist, new String[] {fResourcePath.toString()}));
		if (ResourcesPlugin.getWorkspace().getRoot().getProject(fNewName).exists())
			return RefactoringStatus.createFatalErrorStatus(MessageFormat.format(PDEUIMessages.RenameProjectChange_destinationExists, new String[] {fNewName}));
		return new RefactoringStatus();
	}

	public Change perform(IProgressMonitor pm) throws CoreException {
		try {
			pm.beginTask(PDEUIMessages.RenameProjectChange_taskTitle, 1);

			IResource resource = getResource();
			long currentStamp = resource.getModificationStamp();
			IPath newPath = renamedResourcePath(fResourcePath, fNewName);
			resource.move(newPath, IResource.SHALLOW, pm);
			if (fStampToRestore != IResource.NULL_STAMP) {
				IResource newResource = ResourcesPlugin.getWorkspace().getRoot().findMember(newPath);
				newResource.revertModificationStamp(fStampToRestore);
			}
			String oldName = fResourcePath.lastSegment();
			return new RenameProjectChange(null, newPath, oldName, fComment, currentStamp);
		} finally {
			pm.done();
		}
	}

	public void initializeValidationData(IProgressMonitor pm) {
		// nothing to do
	}

}
