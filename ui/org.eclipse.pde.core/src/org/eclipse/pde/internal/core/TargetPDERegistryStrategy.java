/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.io.File;

public class TargetPDERegistryStrategy extends PDERegistryStrategy {

	public TargetPDERegistryStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object key, PDEExtensionRegistry registry) {
		super(storageDirs, cacheReadOnly, key, registry);
	}

	@Override
	protected void init() {
		// don't attach listeners to ModelManager since we don't need to listen for changes
	}

}
