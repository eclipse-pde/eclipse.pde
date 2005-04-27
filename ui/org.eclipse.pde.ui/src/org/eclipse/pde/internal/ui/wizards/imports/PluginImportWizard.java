/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation.IReplaceQuery;

public class PluginImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "PluginImportWizard"; //$NON-NLS-1$
		
	private IStructuredSelection selection;	
	private PluginImportWizardFirstPage page1;
	private BaseImportWizardSecondPage page2;
	private BaseImportWizardSecondPage page3;

	public PluginImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_PLUGIN_IMPORT_WIZ);
		setWindowTitle(PDEUIMessages.ImportWizard_title); //$NON-NLS-1$
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	public void addPages() {
		setNeedsProgressMonitor(true);
		page1 = new PluginImportWizardFirstPage("first"); //$NON-NLS-1$
		addPage(page1);
		page2 = new PluginImportWizardExpressPage("express", page1, selection); //$NON-NLS-1$
		addPage(page2);
		page3 = new PluginImportWizardDetailedPage("detailed", page1); //$NON-NLS-1$
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
		
		try {
			final IPluginModelBase[] models = getModelsToImport();
			IRunnableWithProgress op =
				getImportOperation(
					getShell(),
					page1.getImportType(),
					models);
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
		final IPluginModelBase[] models) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				try {
					PluginImportOperation.IReplaceQuery query = new ReplaceQuery(shell);
					PluginImportOperation op =
						new PluginImportOperation(models, importType, query);
					PDEPlugin.getWorkspace().run(op, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException(e.getMessage());
				} finally {
					monitor.done();
				}
			}
		};
	}
	

	private static class ReplaceDialog extends MessageDialog {
		public ReplaceDialog(Shell parentShell, String dialogMessage) {
			super(
				parentShell,
				PDEUIMessages.ImportWizard_messages_title, //$NON-NLS-1$
				null,
				dialogMessage,
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL,
					IDialogConstants.NO_LABEL,
					PDEUIMessages.ImportWizard_noToAll, //$NON-NLS-1$
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
				NLS.bind(PDEUIMessages.ImportWizard_messages_exists, project.getName()); //$NON-NLS-1$
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
