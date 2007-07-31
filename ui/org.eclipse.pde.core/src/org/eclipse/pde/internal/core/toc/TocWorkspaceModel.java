/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.toc;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.IWorkspaceModel;
import org.eclipse.pde.internal.core.PDECore;

/**
 * TocWorkspaceModel - models a TOC that has an underlying resource
 * in the workspace.
 *
 */
public class TocWorkspaceModel extends TocModel implements IWorkspaceModel {

	//The underlying file associated with the model
	private IFile fFile;

	//Signals if there are any unsaved changes to the model 
	private boolean fDirty;

	//Signals if the model is editable or read-only
	private boolean fEditable;
	
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new workspace model for a TOC
	 * 
	 * @param file The underlying file that contains the TOC
	 * @param editable Whether or not the model is editable
	 */
	public TocWorkspaceModel(IFile file, boolean editable) {
		fFile = file;
		fEditable = editable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditableModel#save()
	 */
	public void save() {
		try {
			String contents = getContents();
			ByteArrayInputStream stream =
				new ByteArrayInputStream(contents.getBytes("UTF8")); //$NON-NLS-1$
			if (fFile.exists()) {
				fFile.setContents(stream, false, false, null);
			} else {
				fFile.create(stream, false, null);
			}
			stream.close();
		} catch (CoreException e) {
			PDECore.logException(e);
		} catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * @return the contents of the model
	 */
	private String getContents() {
		StringWriter swriter = new StringWriter();
		PrintWriter writer = new PrintWriter(swriter);
		setLoaded(true);
		save(writer);
		writer.flush();
		try {
			swriter.close();
		} catch (IOException e) {
			// Ignore
		}
		return swriter.toString();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#isDirty()
	 */
	public boolean isDirty() {
		return fDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#save(java.io.PrintWriter)
	 */
	public void save(PrintWriter writer) {
		if (isLoaded()) {
			getToc().write("", writer); //$NON-NLS-1$
		}
		setDirty(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IEditable#setDirty(boolean)
	 */
	public void setDirty(boolean dirty) {
		fDirty = dirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#fireModelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void fireModelChanged(IModelChangedEvent event) {
		setDirty(true);
		super.fireModelChanged(event);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.comp.CompCSModel#isEditable()
	 */
	public boolean isEditable() {
		return fEditable;
	}

	/**
	 * @return the absolute path of the underlying resource
	 */
	public String getInstallLocation() {
		return fFile.getLocation().toOSString();
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#getUnderlyingResource()
	 */
	public IResource getUnderlyingResource() {
		return fFile;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocModel#isInSync()
	 */
	public boolean isInSync() {
		IPath path = fFile.getLocation();
		if (path == null) {
			return false;
		}
		return isInSync(path.toFile());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocModel#load()
	 */
	public void load() throws CoreException {
		if (fFile.exists()) {
			InputStream stream = null;
			try {
				stream = new BufferedInputStream(fFile.getContents(true));
				load(stream, false);
			} catch (CoreException e) {
			} 
		} 
	}	
	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.IWorkspaceModel#reload()
	 */
	public void reload() {
		// Underlying file has to exist in order to reload the model
		if (fFile.exists()) {
			InputStream stream = null;
			try {
				// Get the file contents
				stream = new BufferedInputStream(fFile.getContents(true));
				// Load the model using the last saved file contents
				reload(stream, false);
				// Remove the dirty (*) indicator from the editor window
				setDirty(false);
			} catch (CoreException e) {
				// Ignore
			} 
		} 		
	}
	
	/**
	 * @param document
	 */
	public void reload(IDocument document) {
		// Get the document's text
		String text = document.get();
		InputStream stream = null;

		try {
			// Turn the document's text into a stream
			stream = new ByteArrayInputStream(text.getBytes("UTF8")); //$NON-NLS-1$
			// Reload the model using the stream
			reload(stream, false);
			// Remove the dirty (*) indicator from the editor window
			setDirty(false);
		} catch (UnsupportedEncodingException e) {
			PDECore.logException(e);
		} catch (CoreException e) {
			// Ignore
		}
	}		
}
