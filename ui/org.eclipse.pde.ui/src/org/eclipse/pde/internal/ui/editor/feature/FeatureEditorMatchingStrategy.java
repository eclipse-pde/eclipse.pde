/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.ResourceUtil;

public class FeatureEditorMatchingStrategy implements IEditorMatchingStrategy {

	@Override
	public boolean matches(IEditorReference editorRef, IEditorInput input) {
		if (!(input instanceof IFileEditorInput))
			return false;
		IFile inputFile = ResourceUtil.getFile(input);
		if (inputFile == null)
			return false;
		try {
			IFile currInputFile = ResourceUtil.getFile(editorRef.getEditorInput());
			if (currInputFile == null)
				return false;
			if (!inputFile.getProject().equals(currInputFile.getProject()))
				return false;
			// build.properties matches with editors that have a feature.xml file
			// as their input and that feature.xml is at the root
			if (inputFile.getName().equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)) {
				if (currInputFile.getName().equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR))
					return inputFile.getProjectRelativePath().toString().equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
				return inputFile.equals(currInputFile);
			} else if (inputFile.getName().equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
				if (currInputFile.getName().equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR))
					return currInputFile.getProjectRelativePath().toString().equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
				return inputFile.equals(currInputFile);
			}
			return false;
		} catch (PartInitException e) {
			return false;
		}
	}

}
