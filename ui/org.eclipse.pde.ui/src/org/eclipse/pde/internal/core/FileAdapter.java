package org.eclipse.pde.internal.core;

import java.io.File;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class FileAdapter extends PlatformObject {
	private File file;
	private Object[] children;
	private FileAdapter parent;
	private String editorId;

	/**
	 * Constructor for FileAdapter.
	 */
	public FileAdapter(FileAdapter parent, File file) {
		this.file = file;
		String fileName = file.getName();
		if (fileName.equals("plugin.xml") ||
			fileName.equals("fragment.xml")) {
				editorId = PDEPlugin.MANIFEST_EDITOR_ID;
		}
		this.parent = parent;
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
