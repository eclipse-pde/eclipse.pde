/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;

/**
 * A helper class to open an InternationalizeWizard dialog.
 * 
 * @author Team Azure
 */
public class InternationalizeWizardOpenOperation {

	private InternationalizeWizard fWizard;

	public InternationalizeWizardOpenOperation(InternationalizeWizard wizard) {
		Assert.isNotNull(wizard);
		fWizard = wizard;
	}

	public int run(final Shell parent, final String dialogTitle) throws InterruptedException {
		Assert.isNotNull(dialogTitle);
		final IJobManager manager = Job.getJobManager();
		final int[] result = new int[1];
		final InterruptedException[] canceled = new InterruptedException[1];

		Runnable r = new Runnable() {
			public void run() {
				try {
					manager.beginRule(ResourcesPlugin.getWorkspace().getRoot(), null);

					Dialog dialog = new WizardDialog(parent, fWizard);
					dialog.create();

					IWizardContainer wizardContainer = (IWizardContainer) dialog;
					if (wizardContainer.getCurrentPage() == null) {
						//Close the dialog if there are no pages
						result[0] = Window.CANCEL;
					} else {
						//Open the wizard dialog
						result[0] = dialog.open();
					}

				} catch (OperationCanceledException e) {
					canceled[0] = new InterruptedException(e.getMessage());
				} finally {
					manager.endRule(ResourcesPlugin.getWorkspace().getRoot());
				}
			}
		};
		BusyIndicator.showWhile(parent.getDisplay(), r);
		if (canceled[0] != null)
			throw canceled[0];
		return result[0];
	}
}
