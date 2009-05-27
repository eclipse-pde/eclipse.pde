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
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.IProgressConstants;

public class AddNewDependenciesAction extends Action {

	protected class AddDependenciesOperation extends AddNewDependenciesOperation {
		public AddDependenciesOperation(IProject project, IBundlePluginModelBase base) {
			super(project, base);
		}

		protected void handleNewDependencies(final Map additionalDeps, final boolean useRequireBundle, IProgressMonitor monitor) {
			if (!additionalDeps.isEmpty())
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						addDependencies(additionalDeps, useRequireBundle);
					}
				});
			monitor.done();
		}
	}

	private IProject fProject;
	private IBundlePluginModelBase fBase;

	public AddNewDependenciesAction(IProject project, IBundlePluginModelBase base) {
		fProject = project;
		fBase = base;
	}

	public void run() {
		Job job = new WorkspaceJob(PDEUIMessages.DependencyManagementSection_jobName) {

			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				try {
					AddNewDependenciesOperation op = getOperation();
					op.run(monitor);
					if (!op.foundNewDependencies())
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.AddNewDependenciesAction_title, PDEUIMessages.AddNewDependenciesAction_notFound);
							}
						});
				} catch (InvocationTargetException e) {
				} catch (InterruptedException e) {
				} finally {
					monitor.done();
				}
				return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
			}
		};
		job.setUser(true);
		job.setProperty(IProgressConstants.ICON_PROPERTY, PDEPluginImages.DESC_PSEARCH_OBJ.createImage());
		job.schedule();
	}

	protected AddNewDependenciesOperation getOperation() {
		return new AddDependenciesOperation(fProject, fBase);
	}

}
