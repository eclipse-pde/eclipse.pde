/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.plugins;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.project.BundleProjectService;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.imports.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
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
				case PluginImportOperation.IMPORT_BINARY :
					setText(PDEUIMessages.PluginsView_asBinaryProject);
					break;
				case PluginImportOperation.IMPORT_BINARY_WITH_LINKS :
					setText(PDEUIMessages.ImportActionGroup_binaryWithLinkedContent);
					break;
				case PluginImportOperation.IMPORT_WITH_SOURCE :
					setText(PDEUIMessages.PluginsView_asSourceProject);
					break;
				case PluginImportOperation.IMPORT_FROM_REPOSITORY :
					setText(PDEUIMessages.ImportActionGroup_Repository_project);
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
			if (sSelection.getFirstElement() instanceof IPluginExtension || sSelection.getFirstElement() instanceof IPluginExtensionPoint)
				menuName = PDEUIMessages.ImportActionGroup_importContributingPlugin;
			else
				menuName = PDEUIMessages.PluginsView_import;
			MenuManager importMenu = new MenuManager(menuName);
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_BINARY, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_BINARY_WITH_LINKS, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_WITH_SOURCE, sSelection));
			importMenu.add(new ImportAction(PluginImportOperation.IMPORT_FROM_REPOSITORY, sSelection));
			menu.add(importMenu);
		}
	}

	static void handleImport(int importType, IStructuredSelection selection) {
		ArrayList externalModels = new ArrayList();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IPluginModelBase model = getModel(iter.next());
			if (model != null && model.getUnderlyingResource() == null)
				externalModels.add(model);
		}
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		IPluginModelBase[] models = (IPluginModelBase[]) externalModels.toArray(new IPluginModelBase[externalModels.size()]);
		if (importType == PluginImportOperation.IMPORT_FROM_REPOSITORY) {
			Map importMap = getImportDescriptions(display.getActiveShell(), models);
			if (importMap != null) {
				RepositoryImportWizard wizard = new RepositoryImportWizard(importMap);
				WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
				dialog.open();
			}
		} else {
			PluginImportWizard.doImportOperation(display.getActiveShell(), importType, models, false);
		}
	}

	/**
	 * Return a map of {@link IBundleImporter} > Array of {@link ScmUrlImportDescription} to be imported.
	 * 
	 * @param shell shell to open message dialogs on, if required
	 * @param models candidate models
	 * @return  map of importer to import descriptions
	 */
	private static Map getImportDescriptions(Shell shell, IPluginModelBase[] models) {
		BundleProjectService service = (BundleProjectService) BundleProjectService.getDefault();
		try {
			Map descriptions = service.getImportDescriptions(models); // all possible descriptions
			if (!descriptions.isEmpty()) {
				return descriptions;
			}
			// no applicable importers for selected models
			MessageDialog.openInformation(shell, PDEUIMessages.ImportWizard_title, PDEUIMessages.ImportActionGroup_cannot_import);
		} catch (CoreException e) {
			PDEPlugin.log(e);
			MessageDialog.openError(shell, PDEUIMessages.ImportWizard_title, e.getMessage());
		}
		return null;
	}

	public static boolean canImport(IStructuredSelection selection) {
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IPluginModelBase model = getModel(iter.next());
			if (model != null && model.getUnderlyingResource() == null)
				return true;
		}
		return false;
	}

	private static IPluginModelBase getModel(Object next) {
		IPluginModelBase model = null;
		if (next instanceof IPluginModelBase) {
			model = (IPluginModelBase) next;
		} else if (next instanceof IPluginBase) {
			model = ((IPluginBase) next).getPluginModel();
		} else if (next instanceof IPluginExtension) {
			model = ((IPluginExtension) next).getPluginModel();
		} else if (next instanceof IPluginExtensionPoint) {
			model = ((IPluginExtensionPoint) next).getPluginModel();
		} else if (next instanceof BundleDescription) {
			model = PDECore.getDefault().getModelManager().findModel((BundleDescription) next);
		} else if (next instanceof BundleSpecification) {
			// Required for contents of Target Platform State View
			BundleDescription desc = (BundleDescription) ((BundleSpecification) next).getSupplier();
			if (desc != null) {
				model = PDECore.getDefault().getModelManager().findModel(desc);
			}
		} else if (next instanceof IPackageFragmentRoot) {
			// Required for context menu on PDE classpath container entries
			IPackageFragmentRoot root = (IPackageFragmentRoot) next;
			if (root.isExternal()) {
				String path = root.getPath().toOSString();
				IPluginModelBase[] externalModels = PDECore.getDefault().getModelManager().getExternalModels();
				for (int i = 0; i < externalModels.length; i++) {
					if (path.equals(externalModels[i].getInstallLocation())) {
						return externalModels[i];
					}
				}
			}
		}
		return model;

	}
}
