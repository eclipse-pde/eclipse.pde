package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.SelectionProviderAction;

public class OpenReferenceAction extends SelectionProviderAction {
	public static final String LABEL = "Actions.open.label";

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
		file = (IFile) pluginBase.getModel().getUnderlyingResource();
	}
	else if (obj instanceof IFeatureData) {
		IFeatureData data = (IFeatureData)obj;
		String id = data.getId();
		IProject project = (IProject)data.getModel().getUnderlyingResource().getProject();
		file = project.getFile(id);
	}
	if (file!=null && file.exists()) {
		IWorkbenchPage page = PDEPlugin.getDefault().getActivePage();
		try {
			page.openEditor(file);
		} catch (PartInitException e) {
		}
	}
}
public void selectionChanged(IStructuredSelection selection) {
	setEnabled(!selection.isEmpty());
}
}
