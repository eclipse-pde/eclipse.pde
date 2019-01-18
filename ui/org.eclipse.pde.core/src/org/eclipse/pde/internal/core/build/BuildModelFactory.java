/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.build;

import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.build.IBuildModelFactory;

public class BuildModelFactory implements IBuildModelFactory {
	private final IBuildModel model;

	public BuildModelFactory(IBuildModel model) {
		this.model = model;
	}

	@Override
	public IBuildEntry createEntry(String name) {
		BuildEntry entry = new BuildEntry(name);
		entry.setModel(model);
		return entry;
	}
}
