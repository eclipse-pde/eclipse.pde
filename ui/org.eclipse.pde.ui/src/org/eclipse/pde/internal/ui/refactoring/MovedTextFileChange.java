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

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.TextFileChange;

/*
 * Class is meant to be used to perform a TextFileChange on a file which will be moved during the refactoring execution.  This
 * is useful for editing text files when a project is renamed, since the resource will be moved during the project refactoring.
 */

public class MovedTextFileChange extends TextFileChange {

	private IFile fCurrentFile;

	public MovedTextFileChange(String name, IFile newFile, IFile currentFile) {
		super(name, newFile);
		fCurrentFile = currentFile;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.TextChange#getCurrentDocument(org.eclipse.core.runtime.IProgressMonitor)
	 * 
	 * Override getCurrentDocument to return the document of the fCurrentFile instead of the fFile.  Since fFile
	 * does not exist, it creates problems when displaying preview information
	 */
	public IDocument getCurrentDocument(IProgressMonitor pm) throws CoreException {
		if (pm == null)
			pm = new NullProgressMonitor();
		IDocument result = null;
		pm.beginTask("", 2); //$NON-NLS-1$
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			IPath path = fCurrentFile.getFullPath();
			manager.connect(path, LocationKind.NORMALIZE, pm);
			ITextFileBuffer buffer = manager.getTextFileBuffer(path, LocationKind.NORMALIZE);
			result = buffer.getDocument();
		} finally {
			if (result != null)
				manager.disconnect(fCurrentFile.getFullPath(), LocationKind.NORMALIZE, pm);
		}
		pm.done();
		return result;
	}

}
