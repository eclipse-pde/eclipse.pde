/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search.dependencies;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;

public class UnusedDependenciesJob extends Job {

	private IPluginModelBase fModel;
	private boolean fReadOnly;

	public UnusedDependenciesJob(String name, IPluginModelBase model, boolean readOnly) {
		super(name);
		fModel = model;
		fReadOnly = readOnly;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		try {
			GatherUnusedDependenciesOperation udo = new GatherUnusedDependenciesOperation(fModel);
			udo.run(monitor);
			// List can contain IPluginImports or ImportPackageObjects
			showResults(udo.getList().toArray());
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		} finally {
			monitor.done();
		}
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, PDEUIMessages.UnusedDependenciesJob_viewResults, null);
	}

	private Action getShowResultsAction(Object[] unused) {
		return new ShowResultsAction(fModel, unused, fReadOnly);
	}

	protected void showResults(final Object[] unused) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				getShowResultsAction(unused).run();
			}
		});
	}
}
