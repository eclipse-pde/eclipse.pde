package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.ui.actions.*;

public class OpenReferenceAction extends SelectionProviderAction {
	public static final String LABEL = "Actions.open.label";

public OpenReferenceAction(ISelectionProvider provider) {
	super(provider, PDEPlugin.getResourceString(LABEL));
}
public void run() {
	IStructuredSelection sel = (IStructuredSelection) getSelection();
	Object obj = sel.getFirstElement();
	IFile file = null;
	if (obj instanceof PluginReference) {
		PluginReference reference = (PluginReference) obj;
		IPluginModelBase modelBase = reference.getModel();
		file = (IFile) modelBase.getUnderlyingResource();
	}
	else if (obj instanceof IFeatureData) {
		IFeatureData data = (IFeatureData)obj;
		String id = data.getId();
		IFolder folder = (IFolder)data.getModel().getUnderlyingResource().getParent();
		file = folder.getFile(id);
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
