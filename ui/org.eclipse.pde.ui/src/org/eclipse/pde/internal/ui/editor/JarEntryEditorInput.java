/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.io.File;
import org.eclipse.core.resources.IStorage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.*;

/**
 * An EditorInput for a JarEntryFile.
 */
public class JarEntryEditorInput implements IStorageEditorInput {

	private IStorage fJarEntryFile;

	public JarEntryEditorInput(IStorage jarEntryFile) {
		fJarEntryFile = jarEntryFile;
	}

	/*
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof JarEntryEditorInput))
			return false;
		JarEntryEditorInput other = (JarEntryEditorInput) obj;
		return fJarEntryFile.equals(other.fJarEntryFile);
	}

	@Override
	public int hashCode() {
		return fJarEntryFile.hashCode();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getName() {
		return fJarEntryFile.getName();
	}

	public String getFullPath() {
		return fJarEntryFile.getFullPath().toString();
	}

	public String getContentType() {
		return fJarEntryFile.getFullPath().getFileExtension();
	}

	@Override
	public String getToolTipText() {
		File file = fJarEntryFile.getAdapter(File.class);
		if (file != null)
			return file.getAbsolutePath();
		return fJarEntryFile.getFullPath().toString();
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
		return registry.getImageDescriptor(fJarEntryFile.getFullPath().getFileExtension());
	}

	@Override
	public boolean exists() {
		// JAR entries can't be deleted
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(File.class))
			return (T) fJarEntryFile.getAdapter(File.class);
		return null;
	}

	/*
	 * see IStorageEditorInput#getStorage()
	 */
	@Override
	public IStorage getStorage() {
		return fJarEntryFile;
	}
}
