package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.*;
import org.eclipse.ui.part.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.ui.actions.*;

public class OpenReferenceAction extends SelectionProviderAction {
	public static final String LABEL = "Actions.open.label";

public OpenReferenceAction(ISelectionProvider provider) {
	super(provider, PDEPlugin.getResourceString(LABEL));
}
public void run() {
	IStructuredSelection sel = (IStructuredSelection) getSelection();
	Object obj = sel.getFirstElement();
	if (obj instanceof PluginReference) {
		PluginReference reference = (PluginReference) obj;
		IPluginModelBase modelBase = reference.getModel();
		IFile file = (IFile) modelBase.getUnderlyingResource();
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
