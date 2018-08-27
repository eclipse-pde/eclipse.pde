/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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

	@Override
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
		Display.getDefault().asyncExec(() -> getShowResultsAction(unused).run());
	}
}
