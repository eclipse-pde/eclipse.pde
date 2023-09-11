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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.swt.widgets.Display;

public class UnusedDependenciesJob extends Job {

	private final IPluginModelBase fModel;
	private final boolean fReadOnly;

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
		} catch (InvocationTargetException | InterruptedException e) {
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private Action getShowResultsAction(Object[] unused) {
		return new ShowResultsAction(fModel, unused, fReadOnly);
	}

	protected void showResults(final Object[] unused) {
		Display.getDefault().asyncExec(() -> getShowResultsAction(unused).run());
	}
}
