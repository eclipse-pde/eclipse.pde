/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RequiredPluginsClasspathContainer implements IClasspathContainer {
	private IPluginModelBase model;
	private IClasspathEntry[] entries;

	/**
	 * Constructor for RequiredPluginsClasspathContainer.
	 */
	public RequiredPluginsClasspathContainer(IPluginModelBase model) {
		this.model = model;
	}

	public void reset() {
		entries = null;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
	 */
	public IClasspathEntry[] getClasspathEntries() {
		if (model==null) return new IClasspathEntry[0];
		if (entries == null) {
			entries = ClasspathUtilCore.computePluginEntries(model);
			entries = verifyWithAttachmentManager(entries);
		}
		return entries;
	}

	private IClasspathEntry[] verifyWithAttachmentManager(IClasspathEntry[] entries) {
		SourceAttachmentManager manager =
			PDECore.getDefault().getSourceAttachmentManager();
		if (manager.isEmpty())
			return entries;
		IClasspathEntry[] newEntries = new IClasspathEntry[entries.length];
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			newEntries[i] = entry;
			if (entry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
				SourceAttachmentManager.SourceAttachmentEntry saentry =
					manager.findEntry(entry.getPath());
				if (saentry != null) {
					IClasspathEntry newEntry =
						JavaCore.newLibraryEntry(
							entry.getPath(),
							saentry.getAttachmentPath(),
							saentry.getAttachmentRootPath(),
							entry.isExported());
					newEntries[i] = newEntry;
				}
			}
		}
		return newEntries;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
	 */
	public String getDescription() {
		return PDECore.getResourceString("RequiredPluginsClasspathContainer.description"); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
	 */
	public int getKind() {
		return K_APPLICATION;
	}

	/**
	 * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
	 */
	public IPath getPath() {
		return new Path(PDECore.CLASSPATH_CONTAINER_ID);
	}

}
