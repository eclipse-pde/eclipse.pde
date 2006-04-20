package org.eclipse.pde.internal.ui.view;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class ImportActionGroup extends ActionGroup {

	class ImportAction extends Action {
		IStructuredSelection fSel;
		int fImportType;
		ImportAction(int importType, IStructuredSelection selection) {
			fSel = selection;
			fImportType = importType;
			switch (fImportType) {
			case PluginImportOperation.IMPORT_BINARY:
				setText(PDEUIMessages.PluginsView_asBinaryProject);
				break;
			case PluginImportOperation.IMPORT_BINARY_WITH_LINKS:
				setText(PDEUIMessages.ImportActionGroup_binaryWithLinkedContent);
				break;
			case PluginImportOperation.IMPORT_WITH_SOURCE:
				setText(PDEUIMessages.PluginsView_asSourceProject);
				break;
			}
		}
		public void run() {
			handleImport(fImportType, fSel);
		}
	}
	
	public void fillContextMenu(IMenuManager menu) {
		ActionContext context = getContext();
		ISelection selection = context.getSelection();
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			String menuName = null;
			if (sSelection.getFirstElement() instanceof IPluginExtension || 
					sSelection.getFirstElement() instanceof IPluginExtensionPoint)
				menuName = PDEUIMessages.ImportActionGroup_importContributingPlugin;
			else
				menuName = PDEUIMessages.PluginsView_import;
			MenuManager importMenu = new MenuManager(menuName); 
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_BINARY, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_BINARY_WITH_LINKS, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_WITH_SOURCE, sSelection));
			menu.add(importMenu);
		}
	}
	
	private void handleImport(int importType, IStructuredSelection selection) {
		ArrayList externalModels = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object next = iter.next();
			IPluginModelBase model = null;
			if (next instanceof ModelEntry)
				model = ((ModelEntry) next).getActiveModel();
			else if (next instanceof IPluginBase)
				model = ((IPluginBase)next).getPluginModel();
			else if (next instanceof IPluginExtension)
				model = ((IPluginExtension)next).getPluginModel();
			else if (next instanceof IPluginExtensionPoint)
				model = ((IPluginExtensionPoint)next).getPluginModel();
			
			if (model != null && model.getUnderlyingResource() == null)
				externalModels.add(model);
		}
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		IPluginModelBase[] models =
			(IPluginModelBase[]) externalModels.toArray(
				new IPluginModelBase[externalModels.size()]);
		try {		
			IRunnableWithProgress op =
				PluginImportWizard.getImportOperation(display.getActiveShell(), importType, models, false);
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (Exception e) {
		}
	}
	
	public static boolean canImport(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object obj = iter.next();
			if (obj instanceof ModelEntry) {
				ModelEntry entry = (ModelEntry) obj;
				if (entry.getWorkspaceModel() == null)
					return true;
			} else if (obj instanceof IPluginBase) {
				IPluginBase base = (IPluginBase)obj;
				if (base.getPluginModel().getUnderlyingResource() == null)
					return true;
			} else if (obj instanceof IPluginExtension) {
				IPluginExtension ext = (IPluginExtension)obj;
				if (ext.getPluginModel().getUnderlyingResource() == null)
					return true;
			} else if (obj instanceof IPluginExtensionPoint) {
				IPluginExtensionPoint exp = (IPluginExtensionPoint)obj;
				if (exp.getPluginModel().getUnderlyingResource() == null)
					return true;
			}
		}
		return false;
	}
	
}
