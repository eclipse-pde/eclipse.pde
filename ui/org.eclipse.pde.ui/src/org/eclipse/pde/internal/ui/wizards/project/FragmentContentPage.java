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
package org.eclipse.pde.internal.ui.wizards.project;

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.pde.ui.*;
import org.eclipse.ui.actions.*;

public class FragmentContentPage extends FirstTemplateWizardPage implements IFirstWizardPage {
	private IProjectProvider projectProvider;
	private ProjectStructurePage structurePage;
	private boolean isInitialized = false;

	public FragmentContentPage(
		IProjectProvider projectProvider,
		ProjectStructurePage structurePage){
		super(projectProvider, null, true);
		this.projectProvider = projectProvider;
		this.structurePage = structurePage;
	}
	
	public boolean finish(){
		IPluginReference[] list = getDependencies(false);
		final ArrayList result = new ArrayList();
		addDependencies(list,result);
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor)
			throws InterruptedException {
				try {
					WorkspacePluginModelBase model =
						createPluginManifest(
								projectProvider.getProject(),
								createFieldData(),
								result,
								monitor);
					if (structurePage.getStructureData().getRuntimeLibraryName() != null) {
						setJavaSettings(model, new SubProgressMonitor(monitor, 1)); // one step
					}
					model.save();
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}
	
	private void setJavaSettings(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		ClasspathUtil.setClasspath(model, monitor);
	}
	
	private void addDependencies(IPluginReference[] list, ArrayList result) {
		for (int i = 0; i < list.length; i++) {
			IPluginReference reference = list[i];
			if (!result.contains(reference))
				result.add(reference);
		}
	}

	public void setVisible(boolean visible) {
		if (!isInitialized){
			setStructureData(structurePage.getStructureData());
			initializeFields();
			isInitialized = true;
		}
		super.setVisible(visible);
	}
}
