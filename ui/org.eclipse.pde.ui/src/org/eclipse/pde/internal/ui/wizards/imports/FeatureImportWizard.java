/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org
	.eclipse
	.pde
	.internal
	.ui
	.wizards
	.imports
	.FeatureImportOperation
	.IReplaceQuery;

public class FeatureImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "FeatureImportWizard";
	private static final String KEY_WTITLE = "FeatureImportWizard.title";
	private static final String KEY_NO_TO_ALL_LABEL =
		"FeatureImportWizard.noToAll";
	private static final String KEY_MESSAGES_TITLE =
		"FeatureImportWizard.messages.title";
	private static final String KEY_MESSAGES_NO_FEATURES =
		"FeatureImportWizard.messages.noFeatures";
	private static final String KEY_MESSAGES_EXISTS =
		"FeatureImportWizard.messages.exists";

	private FeatureImportWizardFirstPage page1;
	private FeatureImportWizardDetailedPage page2;

	public FeatureImportWizard() {
		IDialogSettings masterSettings =
			PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_FEATURE_IMPORT_WIZ);
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

		page1 = new FeatureImportWizardFirstPage();
		addPage(page1);
		page2 = new FeatureImportWizardDetailedPage(page1);
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
		//long start;
		try {
			final IFeatureModel[] models = page2.getSelectedModels();
			if (models.length == 0) {
				MessageDialog.openInformation(
					getShell(),
					PDEPlugin.getResourceString(KEY_MESSAGES_TITLE),
					PDEPlugin.getResourceString(KEY_MESSAGES_NO_FEATURES));
				return false;
			}

			page1.storeSettings(true);
			page2.storeSettings(true);
			IPath targetPath = computeTargetPath();

			IRunnableWithProgress op = getImportOperation(getShell(), models, targetPath);
			//start = System.currentTimeMillis();
			getContainer().run(true, true, op);

		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return true; // exception handled
		}

		//long stop = System.currentTimeMillis();
		//System.out.println("Total time: "+(stop-start)+"ms");
		return true;
	}
	
	private IPath computeTargetPath() {
		IPath pluginsLocation = PDEPlugin.getWorkspace().getRoot().getLocation();
		return pluginsLocation.removeLastSegments(1).append("features");
	}

	public static IRunnableWithProgress getImportOperation(
		final Shell shell,
		final IFeatureModel[] models,
		final IPath targetPath) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
				try {
					IReplaceQuery query = new ReplaceQuery(shell);
					FeatureImportOperation op =
						new FeatureImportOperation(models, targetPath, query);
					PDEPlugin.getWorkspace().run(
						op,
						monitor);
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
				PDEPlugin.getFormattedMessage(
					KEY_MESSAGES_EXISTS,
					project.getName());
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