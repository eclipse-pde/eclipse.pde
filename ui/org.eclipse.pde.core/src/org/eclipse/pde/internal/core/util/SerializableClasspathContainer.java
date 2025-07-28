/*******************************************************************************
 *  Copyright (c) 2025 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.io.Serializable;
import java.util.Arrays;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;

class SerializableClasspathContainer implements IClasspathContainer, Serializable {

	private static final long serialVersionUID = 1L;
	private final IClasspathEntry[] entries;

	public SerializableClasspathContainer() {
		this(new IClasspathEntry[0]);
	}

	public SerializableClasspathContainer(IClasspathEntry[] entries) {
		this.entries = entries;
	}

	@Override
	public IClasspathEntry[] getClasspathEntries() {
		return entries.clone();
	}

	@Override
	public int getKind() {
		return K_APPLICATION;
	}

	@Override
	public IPath getPath() {
		return PDECore.REQUIRED_PLUGINS_CONTAINER_PATH;
	}

	@Override
	public String getDescription() {
		return PDECoreMessages.RequiredPluginsClasspathContainer_description;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(entries);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SerializableClasspathContainer other = (SerializableClasspathContainer) obj;
		return Arrays.equals(entries, other.entries);
	}

}
