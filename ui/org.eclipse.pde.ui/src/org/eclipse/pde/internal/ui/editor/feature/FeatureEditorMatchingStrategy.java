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

    public boolean matches(IEditorReference editorRef, IEditorInput input) {
    	if (!(input instanceof IFileEditorInput))
    		return false;
        IFile inputFile = ResourceUtil.getFile(input);
        if (inputFile != null && 
        		(inputFile.getName().equals("feature.xml") || //$NON-NLS-1$
        		 inputFile.getName().equals("build.properties"))) { //$NON-NLS-1$
            try {
                return ResourceUtil.getFile(editorRef.getEditorInput()) == inputFile;
            } catch (PartInitException e) {
                return false;
            }
        }
        return false;
    }


}

