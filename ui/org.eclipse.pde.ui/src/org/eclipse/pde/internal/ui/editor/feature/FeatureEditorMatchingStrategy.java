/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.ResourceUtil;


public class FeatureEditorMatchingStrategy implements IEditorMatchingStrategy {

	private static final String BP = "build.properties"; //$NON-NLS-1$
	private static final String FX = "feature.xml"; //$NON-NLS-1$
	
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
			if (inputFile.getName().equals(FX)) {
				if (currInputFile.getName().equals(BP))
					return inputFile.getProjectRelativePath().toString().equals(FX);
				return inputFile.equals(currInputFile);
			} else if (inputFile.getName().equals(BP)) {
				if (currInputFile.getName().equals(FX))
					return currInputFile.getProjectRelativePath().toString().equals(FX);
				return inputFile.equals(currInputFile);
			}
			return false;
		} catch (PartInitException e) {
			return false;
		}
	}

}

