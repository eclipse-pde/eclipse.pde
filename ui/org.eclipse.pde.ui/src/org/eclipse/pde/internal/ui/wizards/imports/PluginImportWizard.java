/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
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
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.wizards.imports.PluginImportOperation.IReplaceQuery;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class PluginImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "PluginImportWizard";
	private static final String KEY_WTITLE = "ImportWizard.title";
	private static final String KEY_NO_TO_ALL_LABEL = "ImportWizard.noToAll";
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
			IRunnableWithProgress op =
				getImportOperation(
					getShell(),
					doImportToWorkspace,
					doExtractPluginSource,
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
		final boolean doImport,
		final boolean doExtract,
		final IPluginModelBase[] models) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				try {
					IReplaceQuery query = new ReplaceQuery(shell);
					PluginImportOperation op =
						new PluginImportOperation(models, doImport, doExtract, query);
					PDEPlugin.getWorkspace().run(op, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException(e.getMessage());
				}
			}
		};
	}

	/*
	 * @see Wizard#performCancel()
	 */
	public boolean performCancel() {
		page1.storeSettings(false);
		page2.storeSettings(false);
		return super.performCancel();
	}

	private static class ReplaceDialog extends MessageDialog {
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
					PDEPlugin.getResourceString(KEY_NO_TO_ALL_LABEL),
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
				PDEPlugin.getFormattedMessage(KEY_MESSAGES_EXISTS, project.getName());
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
}