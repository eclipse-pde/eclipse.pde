/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import org.eclipse.core.runtime.jobs.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.progress.*;

public class UnusedDependenciesAction extends Action {
	
	private IPluginModelBase fModel;

	private boolean fReadOnly;

	public UnusedDependenciesAction(IPluginModelBase model, boolean readOnly) {
		fModel = model;
		setText(PDEUIMessages.UnusedDependencies_action); 
		fReadOnly = readOnly;
	}

	public void run() {
		Job job = new UnusedDependenciesJob(PDEUIMessages.UnusedDependenciesAction_jobName, fModel, fReadOnly); 
		job.setUser(true);
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PSEARCH_OBJ.createImage());
		job.schedule();
	}

}
