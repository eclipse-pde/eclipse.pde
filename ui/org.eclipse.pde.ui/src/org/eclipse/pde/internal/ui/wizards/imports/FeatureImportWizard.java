/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.imports.FeatureImportOperation.IReplaceQuery;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class FeatureImportWizard extends Wizard implements IImportWizard {

	private static final String STORE_SECTION = "FeatureImportWizard"; //$NON-NLS-1$
	private FeatureImportWizardPage fPage;

	public FeatureImportWizard() {
		IDialogSettings masterSettings = PDEPlugin.getDefault().getDialogSettings();
		setDialogSettings(getSettingsSection(masterSettings));
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_FEATURE_IMPORT_WIZ);
		setWindowTitle(PDEUIMessages.FeatureImportWizard_title);
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
		setNeedsProgressMonitor(false);

		fPage = new FeatureImportWizardPage();
		addPage(fPage);
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
		IFeatureModel[] models = fPage.getSelectedModels();
		fPage.storeSettings(true);
		IPath targetPath = computeTargetPath();

		IReplaceQuery query = new ReplaceQuery(getShell());
		final FeatureImportOperation op = new FeatureImportOperation(models, fPage.isBinary(), targetPath, query);
		Job job = new Job(PDEUIMessages.FeatureImportWizard_title) {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PDEPlugin.getWorkspace().run(op, monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return true;
	}

	/**
	 * 
	 * @return IPath or null
	 */
	private IPath computeTargetPath() {
		IPath pluginsLocation = PDEPlugin.getWorkspace().getRoot().getLocation();
		if ("plugins".equals(pluginsLocation.lastSegment())) //$NON-NLS-1$
			return pluginsLocation.removeLastSegments(1).append("features"); //$NON-NLS-1$
		return null;
	}

	private static class ReplaceDialog extends MessageDialog {
		public ReplaceDialog(Shell parentShell, String dialogMessage) {
			super(parentShell, PDEUIMessages.FeatureImportWizard_messages_title, null, dialogMessage, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.YES_TO_ALL_LABEL, IDialogConstants.NO_LABEL, PDEUIMessages.FeatureImportWizard_noToAll, IDialogConstants.CANCEL_LABEL}, 0);
		}
	}

	public static class ReplaceQuery implements IReplaceQuery {
		public ReplaceQuery(Shell shell) {
		}

		private int yesToAll = 0;
		private int[] RETURNCODES = {IReplaceQuery.YES, IReplaceQuery.YES, IReplaceQuery.NO, IReplaceQuery.NO, IReplaceQuery.CANCEL};

		public int doQuery(IProject project) {
			if (yesToAll != 0)
				return yesToAll > 0 ? IReplaceQuery.YES : IReplaceQuery.NO;

			final String message = NLS.bind(PDEUIMessages.FeatureImportWizard_messages_exists, project.getName());
			final int[] result = {IReplaceQuery.CANCEL};

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					ReplaceDialog dialog = new ReplaceDialog(Display.getDefault().getActiveShell(), message);
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
