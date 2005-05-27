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

package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.internal.ui.editor.JarEntryEditorInput;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.ResourceUtil;


public class ManifestEditorMatchingStrategy implements IEditorMatchingStrategy {

    public boolean matches(IEditorReference editorRef, IEditorInput input) {    	
        IFile inputFile = ResourceUtil.getFile(input);
        if (inputFile != null && input instanceof IFileEditorInput) {
            String path = inputFile.getProjectRelativePath().toString();
            if (path.equals("plugin.xml") || path.equals("fragment.xml")  //$NON-NLS-1$ //$NON-NLS-2$
            		|| path.equals("META-INF/MANIFEST.MF") || path.equals("build.properties")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                try {
                    IFile editorFile = ResourceUtil.getFile(editorRef.getEditorInput());
                    return editorFile != null && inputFile.getProject().equals(editorFile.getProject());
                } catch (PartInitException e) {
                    return false;
                }
            }
        } else if (input instanceof SystemFileEditorInput || input instanceof JarEntryEditorInput) {
        	try {
    			IEditorInput existing = editorRef.getEditorInput();
    			return input.equals(existing);
    		} catch (PartInitException e1) {
    		}      	
        }
        return false;
    }


}

