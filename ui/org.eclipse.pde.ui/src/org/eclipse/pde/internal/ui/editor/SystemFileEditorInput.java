package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.ui.*;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class SystemFileEditorInput implements IStorageEditorInput, IPersistableElement {
	private SystemFileStorage storage;
	private static final String FACTORY_ID = PDEPlugin.getPluginId()+".systemFileEditorInputFactory";

	public SystemFileEditorInput(File file) {
		storage = new SystemFileStorage(file);
	}
	public boolean exists() {
		return storage.getFile().exists();
	}
	public Object getAdapter(Class adapter) {
		if (adapter.equals(File.class))
			return storage.getFile();
		return null;
	}
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
	public String getName() {
		return storage.getFile().getName();
	}
	public IPersistableElement getPersistable() {
		return this;
	}
	public void saveState(IMemento memento) {
		memento.putString("path", storage.getFile().getAbsolutePath());
	}
	public String getFactoryId() {
		return FACTORY_ID;
	}
	public IStorage getStorage() {
		return storage;
	}
	public String getToolTipText() {
		return storage.getFile().getAbsolutePath();
	}
	public boolean equals(Object object) {
		return object instanceof SystemFileEditorInput &&
		 getStorage().equals(((SystemFileEditorInput)object).getStorage());
	}
	
	public int hashCode() {
		return getStorage().hashCode();
	}
}