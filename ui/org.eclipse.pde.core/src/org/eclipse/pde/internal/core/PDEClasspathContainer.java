/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

/**
 *
 */
public abstract class PDEClasspathContainer implements IClasspathContainer {
	protected IClasspathEntry[] entries;

	public void reset() {
		entries = null;
	}

	protected IClasspathEntry[] verifyWithAttachmentManager(IClasspathEntry[] entries) {
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
