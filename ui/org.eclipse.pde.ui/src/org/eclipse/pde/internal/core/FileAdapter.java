package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.*;
import java.io.*;
import org.eclipse.pde.model.plugin.IPluginModelBase;

public class FileAdapter implements IAdaptable {
	private File file;
	private Object[] children;
	private FileAdapter parent;

	/**
	 * Constructor for FileAdapter.
	 */
	public FileAdapter(FileAdapter parent, File file) {
		this.file = file;
	}
	
	public FileAdapter getParent() {
		return parent;
	}
	
	public File getFile() {
		return file;
	}
	
	public boolean isDirectory() {
		return file.isDirectory();
	}
	
	/**
	 * @see IAdapterFactory#getAdapter(Object, Class)
	 */
	public Object getAdapter(Class key) {
		return null;
	}
	
	public boolean hasChildren() {
		if (file.isDirectory()==false) return false;
		if (children==null) createChildren();
		return children.length>0;
	}

	public Object[] getChildren() {
		if (file.isDirectory() && children==null)
			createChildren();
		return children!=null?children:new Object[0];
	}
	
	private void createChildren() {
		File [] files = file.listFiles();
		children = new Object[files.length];
		for (int i=0; i<files.length; i++) {
			children[i] = new FileAdapter(this, files[i]);
		}
	}
}
