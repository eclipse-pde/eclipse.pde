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
import org.eclipse.pde.model.plugin.*;

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
	private static final String KEY_WTITLE = "ImportWizard.title";
	private static final String KEY_MESSAGES_TITLE = "ImportWizard.messages.title";
	private static final String KEY_MESSAGES_NO_PLUGINS =
		"ImportWizard.messages.noPlugins";
	private static final String KEY_MESSAGES_DO_NOT_ASK =
		"ImportWizard.messages.doNotAsk";
	private static final String KEY_MESSAGES_EXISTS =
		"ImportWizard.messages.exists";
		

	private PluginImportWizardFirstPage page1;
	private PluginImportWizardDetailedPage page2;

	public PluginImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWEXPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
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
			final IPluginModelBase[] models = page2.getSelectedModels();
			if (models.length == 0) {
				MessageDialog.openInformation(
					getShell(),
					PDEPlugin.getResourceString(KEY_MESSAGES_TITLE),
					PDEPlugin.getResourceString(KEY_MESSAGES_NO_PLUGINS));
				return false;
			}

			page1.storeSettings(true);
			page2.storeSettings(true);
			final boolean doImportToWorkspace = page1.doImportToWorkspace();
			final boolean doExtractPluginSource = page1.doExtractPluginSource();
			getContainer().run(true, true, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
					try {
						IReplaceQuery query = new ReplaceQuery();
						PluginImportOperation op =
							new PluginImportOperation(
								models,
								doImportToWorkspace,
								doExtractPluginSource,
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
		page1.storeSettings(false);
		page2.storeSettings(false);
		return super.performCancel();
	}

	private class ReplaceDialog extends MessageDialog {
		public ReplaceDialog(Shell parentShell, String dialogMessage) {
			super(
				parentShell,
				PDEPlugin.getResourceString(KEY_MESSAGES_TITLE),
				null,
				dialogMessage,
				MessageDialog.QUESTION,
				new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.CANCEL_LABEL },
				0);
		}
	}

	private class ReplaceQuery implements IReplaceQuery {

		private boolean yesToAll;
		private int[] RETURNCODES =
			{ IReplaceQuery.YES, IReplaceQuery.YES, IReplaceQuery.NO, IReplaceQuery.CANCEL };

		public int doQuery(IProject project) {
			if (yesToAll) {
				return IReplaceQuery.YES;
			}

			final String message =
				PDEPlugin.getFormattedMessage(KEY_MESSAGES_EXISTS, project.getName());
			final int[] result = { IReplaceQuery.CANCEL };
			getShell().getDisplay().syncExec(new Runnable() {
				public void run() {
					ReplaceDialog dialog = new ReplaceDialog(getShell(), message);
					int retVal = dialog.open();
					if (retVal >= 0) {
						result[0] = RETURNCODES[retVal];
						if (retVal == 1) {
							yesToAll = true;
						}
					}
				}
			});
			return result[0];
		}
	}
}