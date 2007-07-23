/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.ResourceUtil;


public class ManifestEditorMatchingStrategy implements IEditorMatchingStrategy {

    public boolean matches(IEditorReference editorRef, IEditorInput input) {    	
        IFile inputFile = ResourceUtil.getFile(input);
        if (input instanceof IFileEditorInput) {
            try {
            	// a positive match if there is an editor already open on the same file
	        	if (input.equals(editorRef.getEditorInput()))
	        		return true;  
	        	
	        	// a quick no-match if the project of the file being opened is not the
	        	// same as the project of the file associated with the open editor
	        	IFile editorFile = ResourceUtil.getFile(editorRef.getEditorInput());
	        	if (editorFile == null || !inputFile.getProject().equals(editorFile.getProject()))
	        		return false;
	        	
	        	// If a MANIFEST.MF file is being opened, make sure we return a positive match
	        	// only if it is colocated with the plugin.xml/fragment.xml file already open
	        	if (inputFile.getName().equals("MANIFEST.MF")) { //$NON-NLS-1$
	        		IContainer parent = inputFile.getParent();
	        		return parent instanceof IFolder 
	        				&& parent.getName().equals("META-INF")  //$NON-NLS-1$
	        				&& parent.getParent().equals(editorFile.getParent());
	        	}
	        	
	        	// if a plugin.xml/fragment.xml is being opened, make sure we return a positive match
	        	// only if the editor that is open is associated with a colocated MANIFEST.MF
	        	if (inputFile.getName().equals("plugin.xml") || inputFile.getName().equals("fragment.xml")) { //$NON-NLS-1$ //$NON-NLS-2$
	        		IContainer parent = inputFile.getParent();
	        		IFile file = parent.getFile(ICoreConstants.MANIFEST_PATH);
	        		return file.exists() && editorFile.equals(file);
	        	}
	        	
	        	// if a build.properties is being opened, only return a positive match
	        	// if an editor is already open on a sibling plugin.xml/fragment.xml or a META-INF/MANIFEST.MF
	            if (inputFile.getName().equals("build.properties")) { //$NON-NLS-1$ 
                    IContainer parent = inputFile.getParent();
                    if (parent.equals(editorFile.getParent())) {
                    	return editorFile.getName().equals("plugin.xml") || editorFile.getName().equals("fragment.xml"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    IFile file = parent.getFile(ICoreConstants.MANIFEST_PATH);
                    return file.exists() && editorFile.equals(file);
	            }
            } catch (PartInitException e) {
                return false;
            }
        } else if (input instanceof IStorageEditorInput) {
        	try {
    			IEditorInput existing = editorRef.getEditorInput();
    			return input.equals(existing);
    		} catch (PartInitException e1) {
    		}      	
        }
        return false;
    }


}

