/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.pde.internal.base.model.plugin.*;

import org.eclipse.pde.internal.*;
import org
	.eclipse
	.pde
	.internal
	.wizards
	.imports
	.PluginImportOperation
	.IReplaceQuery;

public class PluginImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "PluginImportWizard";

	private PluginImportWizardFirstPage page1;
	private PluginImportWizardDetailedPage page2;

	/**
	 * Constructor for DropPluginInportWizard.
	 */
	public PluginImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
	}

	/*
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	/*
	 * @see org.eclipse.jface.wizard.IWizard#addPages
	 */
	public void addPages() {
		setNeedsProgressMonitor(true);

		page1 = new PluginImportWizardFirstPage();
		addPage(page1);
		page2 = new PluginImportWizardDetailedPage(page1);
		addPage(page2);
	}

	private IDialogSettings getSettingsSection(IDialogSettings master) {
		IDialogSettings setting = master.getSection(STORE_SECTION);
		if (setting == null) {
			setting = master.addNewSection(STORE_SECTION);
		}
		return setting;
	}

	/*
	 * @see Wizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			IPluginModelBase[] models = page2.getSelectedModels();
			if (models.length == 0) {
				MessageDialog.openInformation(
					getShell(),
					"Plugin Import",
					"No plugins found. Check that the chosen directory points to the 'plugins' folder of a SDK drop.");
				return false;
			}

			page1.storeSettings(true);
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
					try {
						IReplaceQuery query = new ReplaceQuery();
						PluginImportOperation op =
							new PluginImportOperation(
								page2.getSelectedModels(),
								page1.doImportToWorkspace(),
								page1.doExtractPluginSource(),
								query);
						PDEPlugin.getWorkspace().run(op, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} catch (OperationCanceledException e) {
						throw new InterruptedException(e.getMessage());
					}
				}
			});
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return true; // exception handled
		}
		return true;
	}

	/*
	 * @see Wizard#performCancel()
	 */
	public boolean performCancel() {
		IDialogSettings setting = getDialogSettings();
		page1.storeSettings(false);
		return super.performCancel();
	}

	private class ReplaceDialog extends MessageDialog {

		private Button fForAllCheckBox;
		private boolean fIsForAll;

		public ReplaceDialog(Shell parentShell, String dialogMessage) {
			super(
				parentShell,
				"Plugin Import",
				null,
				dialogMessage,
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.CANCEL_LABEL },
				0);
			fIsForAll = false;
		}

		/*
		 * @see MessageDialog#createCustomArea(Composite)
		 */
		protected Control createCustomArea(Composite parent) {
			fForAllCheckBox = new Button(parent, SWT.CHECK);
			fForAllCheckBox.setText(
				"&Do not ask again for this import (Yes to all / No to all).");
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent = convertWidthInCharsToPixels(10);
			fForAllCheckBox.setLayoutData(gd);
			return fForAllCheckBox;
		}

		protected void buttonPressed(int buttonId) {
			fIsForAll = fForAllCheckBox.getSelection();
			super.buttonPressed(buttonId);
		}

		public boolean isForAll() {
			return fIsForAll;
		}
	}

	private class ReplaceQuery implements IReplaceQuery {

		private int fForAll = -1;
		private int[] RETURNCODES =
			{ IReplaceQuery.YES, IReplaceQuery.NO, IReplaceQuery.CANCEL };

		public int doQuery(IProject project) {
			if (fForAll != -1) {
				return fForAll;
			}
			final String message =
				"Project '" + project.getName() + "' already exists.\nOk to replace?";
			final int[] result = { IReplaceQuery.CANCEL };
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					ReplaceDialog dialog = new ReplaceDialog(getShell(), message);
					int retVal = dialog.open();
					if (retVal >= 0) {
						result[0] = RETURNCODES[retVal];
						if (dialog.isForAll()) {
							fForAll = result[0];
						}
					}
				}
			});
			return result[0];
		}
	}
}