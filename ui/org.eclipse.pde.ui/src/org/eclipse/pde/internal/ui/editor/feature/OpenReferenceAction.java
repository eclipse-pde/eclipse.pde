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

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.ide.*;

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
	else if (obj instanceof ISiteBuildFeature) {
		ISiteBuildFeature sfeature = (ISiteBuildFeature)obj;
		IFeature feature = sfeature.getReferencedFeature();
		if (feature!=null) {
			file = (IFile)feature.getModel().getUnderlyingResource();
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
