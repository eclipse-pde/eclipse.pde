package org.eclipse.pde.internal.pluginsview;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.Action; 
import org.eclipse.jface.viewers.*;
import java.util.*;
import org.eclipse.pde.internal.base.model.IModel;
import org.eclipse.pde.internal.base.model.plugin.IPluginModelBase;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.*;

public class EnableAction extends Action {
	private TreeViewer viewer;
	public EnableAction(TreeViewer viewer) {
		this.viewer = viewer;
	}
	
	public void run() {
		ISelection selection = viewer.getSelection();
		Vector wschanged = new Vector();
		Vector exchanged = new Vector();
		boolean enable = isChecked();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection)selection;
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase)obj;
					if (model.isEnabled()!=enable) {
						model.setEnabled(enable);
						if (model.getUnderlyingResource()!=null)
							wschanged.add(model);
						else
							exchanged.add(model);
					}
				}
			}
		}
		if (wschanged.size()>0) {
			IModel [] models = (IModel[])wschanged.toArray(new IModel[wschanged.size()]);
			PDEPlugin.getDefault().getWorkspaceModelManager().fireModelsChanged(models);
		}
		if (exchanged.size()>0) {
			IModel [] models = (IModel[])exchanged.toArray(new IModel[exchanged.size()]);
			ExternalModelManager manager = PDEPlugin.getDefault().getExternalModelManager();
			ModelProviderEvent e = new ModelProviderEvent(manager, IModelProviderEvent.MODELS_CHANGED, null, null, models);
			PDEPlugin.getDefault().getExternalModelManager().fireModelProviderEvent(e);
		}
	}
}
