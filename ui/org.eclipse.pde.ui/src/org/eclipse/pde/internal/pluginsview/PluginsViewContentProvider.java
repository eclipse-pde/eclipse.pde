/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.pluginsview;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.base.model.plugin.IPluginModelBase;

class PluginsViewContentProvider
	extends DefaultContentProvider
	implements ITreeContentProvider {
	private PluginsView view;

	private ParentElement[] rootElements;

	public PluginsViewContentProvider(PluginsView view) {
		this.view = view;
		rootElements = new ParentElement[3];
		rootElements[0] = new ParentElement(ParentElement.PLUGIN_PROFILES);
		rootElements[1] = new ParentElement(ParentElement.WORKSPACE_PLUGINS);
		rootElements[2] = new ParentElement(ParentElement.EXTERNAL_PLUGINS);
	}

	public Object[] getChildren(Object parent) {
		if (parent instanceof ParentElement) {
			ParentElement pe = (ParentElement) parent;
			return getParentElementChildren(pe.getId());
		}
		return new Object[0];
	}
	public boolean hasChildren(Object parent) {
		if (parent instanceof ParentElement)
			return true;
		return false;
	}
	public Object getParent(Object child) {
		return null;
	}
	public Object[] getElements(Object parent) {
		return rootElements;
	}

	private Object[] getParentElementChildren(int id) {
		switch (id) {
			case ParentElement.WORKSPACE_PLUGINS :
				return PDEPlugin
					.getDefault()
					.getWorkspaceModelManager()
					.getWorkspacePluginModels();

			case ParentElement.EXTERNAL_PLUGINS :
				Display display = PDEPlugin.getActiveWorkbenchShell().getDisplay();
				return PDEPlugin.getDefault().getExternalModelManager().getModels();
		}
		return new Object[0];
	}

	public void modelsChanged(final IModelProviderEvent e) {
		view.getTreeViewer().getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				boolean workspaceChange =
					e.getEventSource() == PDEPlugin.getDefault().getWorkspaceModelManager();
				ParentElement parent = workspaceChange ? rootElements[1] : rootElements[2];
				IModel[] added = e.getAddedModels();
				TreeViewer viewer = view.getTreeViewer();
				if (added != null && added.length > 0) {
					viewer.add(parent, added);
				}
				IModel[] removed = e.getRemovedModels();
				if (removed != null && removed.length > 0) {
					viewer.remove(removed);
				}
				IModel[] changed = e.getChangedModels();
				if (changed != null && changed.length > 0) {
					viewer.update(changed, null);
				}
			}
		});
	}
}