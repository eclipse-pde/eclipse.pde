/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

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
 * SimpleCSWorkspaceModel
 *
 */
public class SimpleCSWorkspaceModel extends SimpleCSModel implements
		IWorkspaceModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private IFile fFile;

	private boolean fDirty;

	private boolean fEditable;	
	
	/**
	 * 
	 */
	public SimpleCSWorkspaceModel(IFile file, boolean editable) {
		fFile = file;
		fEditable = editable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSModel#load()
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
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSModel#isInSync()
	 */
	public boolean isInSync() {
		IPath path = fFile.getLocation();
		if (path == null) {
			return false;
		}
		return isInSync(path.toFile());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.AbstractModel#getUnderlyingResource()
	 */
	public IResource getUnderlyingResource() {
		return fFile;
	}
	
	/**
	 * @return
	 */
	public String getInstallLocation() {
		return fFile.getLocation().toOSString();
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
		}
	}

	/**
	 * @return
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
			getSimpleCS().write("", writer); //$NON-NLS-1$
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
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSModel#isEditable()
	 */
	public boolean isEditable() {
		return fEditable;
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
