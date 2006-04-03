package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.schema.SchemaEditor;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public abstract class PDEOutlinePage extends ContentOutlinePage {

	public void makeContributions(
			IMenuManager menuManager, 
			IToolBarManager toolBarManager, 
			IStatusLineManager statusLineManager) {
		
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ISelection selection = getSelection();
				PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(manager);
				if (selection instanceof IStructuredSelection) {
					Object element = ((IStructuredSelection)selection).getFirstElement();
					if (element instanceof IPluginExtensionPoint) {
						final IPluginExtensionPoint point = (IPluginExtensionPoint) element;
						IResource res = point.getPluginModel().getUnderlyingResource();
						if (res != null) {
							IProject project = res.getProject();
							if (point.getSchema() != null) {
								final IFile schemaFile = project.getFile(point.getSchema());
								Action openSchemaAction = new Action(PDEUIMessages.ManifestEditor_DetailExtensionPointSection_openSchema) {
									public void run() {
										SchemaEditor.openSchema(schemaFile);
									}
								};
								manager.add(new Separator());
								manager.add(openSchemaAction);
							}
						} 
					} 
				}
			}
		};
		
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = getTreeViewer().getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
	}
	
}
