/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

public class PluginImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "PluginImportWizard";
		
	private IStructuredSelection selection;	
	private PluginImportWizardFirstPage page1;
	private BaseImportWizardSecondPage page2;
	private BaseImportWizardSecondPage page3;

	public PluginImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(PDEPlugin.getResourceString("ImportWizard.title"));
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public void addPages() {
		setNeedsProgressMonitor(true);
		page1 = new PluginImportWizardFirstPage("first");
		addPage(page1);
		page2 = new PluginImportWizardExpressPage("express", page1, selection);
		addPage(page2);
		page3 = new PluginImportWizardDetailedPage("detailed", page1);
		addPage(page3);
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	private IPluginModelBase[] getModelsToImport() {
		if (page1.getNextPage().equals(page2))
			return page2.getModelsToImport();
		return page3.getModelsToImport();
	}

	public boolean performFinish() {
		page1.storeSettings();
		((BaseImportWizardSecondPage)page1.getNextPage()).storeSettings();
		
		final ArrayList modelIds = new ArrayList();
		try {
			final IPluginModelBase[] models = getModelsToImport();
			IRunnableWithProgress op =
				getImportOperation(
					getShell(),
					page1.getImportType(),
					models,
					modelIds);
			getContainer().run(true, true, op);

		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return true; // exception handled
		}		
		return true;
	}
	
	public static IRunnableWithProgress getImportOperation(
		final Shell shell,
		final int importType,
		final IPluginModelBase[] models,
		final ArrayList modelIds) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				boolean isAutoBuilding = PDEPlugin.getWorkspace().isAutoBuilding();
				try {
					if (isAutoBuilding) {
						IWorkspace workspace = PDEPlugin.getWorkspace();
						IWorkspaceDescription description = workspace.getDescription();
						description.setAutoBuilding(false);
						workspace.setDescription(description);
					}
					monitor.beginTask("", 10);
					IReplaceQuery query = new ReplaceQuery(shell);
					PluginImportOperation op =
						new PluginImportOperation(models, modelIds, importType, query);
					PDEPlugin.getWorkspace().run(op, new SubProgressMonitor(monitor, 8));
					Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException(e.getMessage());
				} finally {
					try {
						PDEPlugin.getWorkspace().run(
							getUpdateClasspathOperation(modelIds),
							new SubProgressMonitor(monitor, 1));
						if (isAutoBuilding) {
							PDEPlugin.getWorkspace().run(
								getActivateAutoBuildOperation(),
								new SubProgressMonitor(monitor, 1));
						} else {
							monitor.worked(1);
						}
					} catch (CoreException e) {
					}
					monitor.done();
				}
			}
		};
	}
	
	private static IWorkspaceRunnable getActivateAutoBuildOperation() {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				IWorkspace workspace = PDEPlugin.getWorkspace();
				IWorkspaceDescription description = workspace.getDescription();
				description.setAutoBuilding(true);
				workspace.setDescription(description);
			}
		};
	}
	
	private static IWorkspaceRunnable getUpdateClasspathOperation(final ArrayList modelIds) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				/*UpdateClasspathAction.doUpdateClasspath(
					monitor,
					getWorkspaceCounterparts(modelIds),
					new UpdateClasspathAction.MissingPluginConfirmation());*/
				UpdateClasspathAction.doUpdateClasspath(
						monitor,
						getWorkspaceCounterparts(modelIds),
						null);
			}
		};
	}

	private static class ReplaceDialog extends MessageDialog {
		public ReplaceDialog(Shell parentShell, String dialogMessage) {
			super(
				parentShell,
				PDEPlugin.getResourceString("ImportWizard.messages.title"),
				null,
				dialogMessage,
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL,
					IDialogConstants.NO_LABEL,
					PDEPlugin.getResourceString("ImportWizard.noToAll"),
					IDialogConstants.CANCEL_LABEL },
				0);
		}
	}

	private static class ReplaceQuery implements IReplaceQuery {
		private Shell shell;
		public ReplaceQuery(Shell shell) {
			this.shell = shell;
		}

		private int yesToAll = 0;
		private int[] RETURNCODES =
			{
				IReplaceQuery.YES,
				IReplaceQuery.YES,
				IReplaceQuery.NO,
				IReplaceQuery.NO,
				IReplaceQuery.CANCEL };

		public int doQuery(IProject project) {
			if (yesToAll != 0) {
				return yesToAll > 0 ? IReplaceQuery.YES : IReplaceQuery.NO;
			}

			final String message =
				PDEPlugin.getFormattedMessage("ImportWizard.messages.exists", project.getName());
			final int[] result = { IReplaceQuery.CANCEL };
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					ReplaceDialog dialog = new ReplaceDialog(shell, message);
					int retVal = dialog.open();
					if (retVal >= 0) {
						result[0] = RETURNCODES[retVal];
						if (retVal == 1) {
							yesToAll = 1;
						} else if (retVal == 3) {
							yesToAll = -1;
						}
					}
				}
			});
			return result[0];
		}
	}
	
	private static IPluginModelBase[] getWorkspaceCounterparts(ArrayList modelIds) {
		ArrayList desiredModels = new ArrayList();
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		for (int i = 0; i < modelIds.size(); i++) {
			ModelEntry entry = manager.findEntry(modelIds.get(i).toString(), null);
			if (entry != null && entry.getActiveModel() instanceof WorkspacePluginModelBase) {
				desiredModels.add(entry.getActiveModel());
			}
		}
		return (IPluginModelBase[])desiredModels.toArray(new IPluginModelBase[desiredModels.size()]);
	}
	
	public IWizardPage getNextPage(IWizardPage page) {
		if (page.equals(page1)) {
			if (page1.getScanAllPlugins()) {
				return page3;
			}
			return page2;			
		}
		return null;
	}
	
	public IWizardPage getPreviousPage(IWizardPage page) {
		return page.equals(page1) ? null : page1;
	}
	
	public boolean canFinish() {
		return page1.getNextPage().isPageComplete();
	}
}
