/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * InternationalizeOperation is responsible for populating a plug-in model table
 * containing the list of plug-ins (workspace and external) prior to running the
 * wizard. An instance of this class must be created before creating an
 * InternationlizeWizard instance.
 * 
 * @author Team Azure
 *
 */
public class InternationalizeOperation implements IRunnableWithProgress {

	private ISelection fSelection;
	private ArrayList fSelectedModels;
	private InternationalizeModelTable fModelPluginTable;
	private boolean fCanceled;

	/**
	 * 
	 * @param selection represents the preselected plug-in projects in the workbench
	 */
	public InternationalizeOperation(ISelection selection) {
		fSelection = selection;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

		if (fSelection instanceof IStructuredSelection) {

			Object[] elems = ((IStructuredSelection) fSelection).toArray();

			fSelectedModels = new ArrayList(elems.length);
			for (int i = 0; i < elems.length; i++) {
				//If a file was selected, get its parent project
				if (elems[i] instanceof IFile)
					elems[i] = ((IFile) elems[i]).getProject();

				//Add the project to the preselected model list
				if (elems[i] instanceof IProject && WorkspaceModelManager.isPluginProject((IProject) elems[i]) && !WorkspaceModelManager.isBinaryProject((IProject) elems[i]))
					fSelectedModels.add(elems[i]);
			}
		}

		//Get all models (workspace and external) excluding fragment models
		IPluginModelBase[] pluginModels = PluginRegistry.getAllModels(false);
		monitor.beginTask(PDEUIMessages.GetNonExternalizedStringsOperation_taskMessage, pluginModels.length);

		//Populate list to an InternationalizeModelTable
		fModelPluginTable = new InternationalizeModelTable();
		for (int i = 0; i < pluginModels.length; i++) {
			fModelPluginTable.addToModelTable(pluginModels[i], pluginModels[i].getUnderlyingResource() != null ? selected(pluginModels[i].getUnderlyingResource().getProject()) : false);
		}
	}

	/**
	 * 
	 * @return whether or not the operation was cancelled
	 */
	public boolean wasCanceled() {
		return fCanceled;
	}

	/**
	 * 
	 * @param project
	 * @return whether or not the project was preselected
	 */
	public boolean selected(IProject project) {
		return fSelectedModels.contains(project);
	}

	/**
	 * 
	 * @return the InternationalizeModelTable containing the plug-ins
	 */
	public InternationalizeModelTable getPluginTable() {
		return fModelPluginTable;
	}
}
