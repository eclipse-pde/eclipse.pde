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
package org.eclipse.pde.internal.ui.model.build;

import org.eclipse.pde.core.build.*;

public class BuildModelFactory implements IBuildModelFactory {
	
	private IBuildModel fModel;
	
	public BuildModelFactory(IBuildModel model) {
		fModel = model;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildModelFactory#createEntry(java.lang.String)
	 */
	public IBuildEntry createEntry(String name) {
		BuildEntry entry = new BuildEntry();
		entry.setName(name);
		entry.setModel(fModel);
		return entry;
	}
}
