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

	/*
	 * @see IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/*
	 * @see IEditorInput#getName()
	 */
	@Override
	public String getName() {
		return fJarEntryFile.getName();
	}

	/*
	 * @see IEditorInput#getFullPath()
	 */
	public String getFullPath() {
		return fJarEntryFile.getFullPath().toString();
	}

	/*
	 * @see IEditorInput#getContentType()
	 */
	public String getContentType() {
		return fJarEntryFile.getFullPath().getFileExtension();
	}

	/*
	 * @see IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		File file = fJarEntryFile.getAdapter(File.class);
		if (file != null)
			return file.getAbsolutePath();
		return fJarEntryFile.getFullPath().toString();
	}

	/*
	 * @see IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
		return registry.getImageDescriptor(fJarEntryFile.getFullPath().getFileExtension());
	}

	/*
	 * @see IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		// JAR entries can't be deleted
		return true;
	}

	/*
	 * @see IAdaptable#getAdapter(Class)
	 */
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
