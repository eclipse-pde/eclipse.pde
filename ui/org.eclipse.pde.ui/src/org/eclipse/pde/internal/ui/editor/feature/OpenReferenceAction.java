/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.ide.IDE;

public class OpenReferenceAction extends SelectionProviderAction {
	public static final String LABEL = "Actions.open.label"; //$NON-NLS-1$

public OpenReferenceAction(ISelectionProvider provider) {
	super(provider, PDEPlugin.getResourceString(LABEL));
}
public void run() {
	IStructuredSelection sel = (IStructuredSelection) getSelection();
	Object obj = sel.getFirstElement();
	IFile file = null;
	if (obj instanceof FeaturePlugin) {
		FeaturePlugin reference = (FeaturePlugin) obj;
		IPluginBase pluginBase = reference.getPluginBase();
		if (pluginBase!=null)
			file = (IFile) pluginBase.getModel().getUnderlyingResource();
	}
	else if (obj instanceof IFeatureData) {
		IFeatureData data = (IFeatureData)obj;
		String id = data.getId();
		IResource resource = data.getModel().getUnderlyingResource();
		if (resource!=null) {
			IProject project = resource.getProject();
			file = project.getFile(id);
		}
	}
	else if (obj instanceof IFeatureChild) {
			IFeatureChild included = (IFeatureChild) obj;
			IFeature feature = ((FeatureChild) included).getReferencedFeature();
			if (feature != null) {
				IResource resource = feature.getModel().getUnderlyingResource();
				if (resource != null) {
					file = (IFile) resource;
				}
			}
		}
	if (file!=null && file.exists()) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		try {
			IDE.openEditor(page, file, true);
		} catch (PartInitException e) {
		}
	}
}
public void selectionChanged(IStructuredSelection selection) {
	setEnabled(!selection.isEmpty());
}
}
