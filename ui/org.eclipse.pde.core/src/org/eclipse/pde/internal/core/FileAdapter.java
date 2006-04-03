/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.Locale;

import org.eclipse.core.runtime.PlatformObject;

public class FileAdapter extends PlatformObject {
	private File file;
	private Object[] children;
	private FileAdapter parent;
	private String editorId;
	private IFileAdapterFactory factory;

	/**
	 * Constructor for FileAdapter.
	 */
	public FileAdapter(FileAdapter parent, File file, IFileAdapterFactory factory) {
		this.file = file;
		this.parent = parent;
		this.factory = factory;
	}

	public boolean isManifest() {
		String fileName = file.getName();
		return (fileName.equals("plugin.xml") || fileName.equals("fragment.xml") || fileName.equalsIgnoreCase("manifest.mf")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	public boolean isSchema() {
		String fileName = file.getName().toLowerCase(Locale.ENGLISH);
		return fileName.endsWith(".mxsd") || fileName.endsWith(".exsd"); //$NON-NLS-1$ //$NON-NLS-2$
	}
			
	public FileAdapter getParent() {
		return parent;
	}

	public void setEditorId(String editorId) {
		this.editorId = editorId;
	}

	public String getEditorId() {
		return editorId;
	}

	public File getFile() {
		return file;
	}

	public boolean isDirectory() {
		return file.isDirectory();
	}

	public boolean hasChildren() {
		if (file.isDirectory() == false)
			return false;
		if (children == null)
			createChildren();
		return children.length > 0;
	}

	public Object[] getChildren() {
		if (file.isDirectory() && children == null)
			createChildren();
		return children != null ? children : new Object[0];
	}

	private void createChildren() {
		File[] files = file.listFiles();
		children = new Object[files.length];
		for (int i = 0; i < files.length; i++) {
			if (factory==null)	
				children[i] = new FileAdapter(this, files[i], null);
			else
				children[i] = factory.createAdapterChild(this, files[i]);
		}
	}
}
