/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.Locale;
import org.eclipse.core.runtime.PlatformObject;

public class FileAdapter extends PlatformObject {
	private File fFile;
	private Object[] fChildren;
	private FileAdapter fParent;
	private String fEditorId;
	private IFileAdapterFactory fFactory;

	/**
	 * Constructor for FileAdapter.
	 */
	public FileAdapter(FileAdapter parent, File file, IFileAdapterFactory factory) {
		fFile = file;
		fParent = parent;
		fFactory = factory;
	}

	public boolean isManifest() {
		String fileName = fFile.getName();
		return (fileName.equals(ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR) || fileName.equals(ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR) || fileName.equalsIgnoreCase(ICoreConstants.MANIFEST_FILENAME));
	}

	public boolean isSchema() {
		String fileName = fFile.getName().toLowerCase(Locale.ENGLISH);
		return fileName.endsWith(".exsd"); //$NON-NLS-1$
	}

	public FileAdapter getParent() {
		return fParent;
	}

	public void setEditorId(String editorId) {
		this.fEditorId = editorId;
	}

	public String getEditorId() {
		return fEditorId;
	}

	public File getFile() {
		return fFile;
	}

	public boolean isDirectory() {
		return fFile.isDirectory();
	}

	public boolean hasChildren() {
		if (fFile.isDirectory() == false)
			return false;
		if (fChildren == null)
			createChildren();
		return fChildren.length > 0;
	}

	public Object[] getChildren() {
		if (fFile.isDirectory() && fChildren == null)
			createChildren();
		return fChildren != null ? fChildren : new Object[0];
	}

	private void createChildren() {
		File[] files = fFile.listFiles();
		fChildren = new Object[files.length];
		for (int i = 0; i < files.length; i++) {
			if (fFactory == null)
				fChildren[i] = new FileAdapter(this, files[i], null);
			else
				fChildren[i] = fFactory.createAdapterChild(this, files[i]);
		}
	}
}
