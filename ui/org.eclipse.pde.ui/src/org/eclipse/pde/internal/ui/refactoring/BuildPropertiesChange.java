/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public class BuildPropertiesChange {

	public static Change createRenameChange(IFile file, Object[] affectedElements, String[] newNames, IProgressMonitor monitor) throws CoreException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);

			IDocument document = buffer.getDocument();

			try {
				BuildModel model = new BuildModel(document, false);
				model.load();
				if (!model.isLoaded())
					return null;
				PropertiesTextChangeListener listener = new PropertiesTextChangeListener(document);
				model.addModelChangedListener(listener);

				IBuild build = model.getBuild();
				IBuildEntry[] entries = build.getBuildEntries();
				for (int i = 0; i < affectedElements.length; i++) {
					if (affectedElements[i] instanceof IJavaElement)
						continue;
					IResource res = (IResource) affectedElements[i];
					// if resource instanceof IProject, then the project is being renamed and there is no action to do in the build.properties for the resource
					if (res instanceof IProject)
						continue;
					for (IBuildEntry entry : entries) {
						addBuildEntryEdit(entry, res, newNames[i]);
					}
				}

				TextEdit[] operations = listener.getTextOperations();
				if (operations.length > 0) {
					MoveFromChange change = new MoveFromChange("", file); //$NON-NLS-1$
					MultiTextEdit edit = new MultiTextEdit();
					edit.addChildren(operations);
					change.setEdit(edit);
					PDEModelUtility.setChangeTextType(change, file);
					return change;
				}
			} catch (CoreException e) {
				return null;
			}
			return null;
		} finally {
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, monitor);
		}
	}

	private static void addBuildEntryEdit(IBuildEntry entry, IResource res, String string) {
		IPath resPath = res.getProjectRelativePath();
		String[] tokens = entry.getTokens();
		for (String token : tokens) {
			if (resPath.isPrefixOf(new Path(token))) {
				try {
					entry.renameToken(token, string.concat(token.substring(resPath.toString().length())));
				} catch (CoreException e) {
				}
			}
		}
	}

}
