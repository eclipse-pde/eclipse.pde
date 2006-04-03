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
package org.eclipse.pde.internal.ui.editor;

import java.io.File;

import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;

public class SystemFileEditorInput implements IStorageEditorInput, IPersistableElement {
	private SystemFileStorage storage;
	private static final String FACTORY_ID = PDEPlugin.getPluginId()+".systemFileEditorInputFactory"; //$NON-NLS-1$

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
		memento.putString("path", storage.getFile().getAbsolutePath()); //$NON-NLS-1$
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
