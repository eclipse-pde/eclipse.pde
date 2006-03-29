/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.build;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;

public class FeatureExportJob extends Job {
	
	class SchedulingRule implements ISchedulingRule {
		public boolean contains(ISchedulingRule rule) {
			return rule instanceof SchedulingRule || rule instanceof IResource;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return rule instanceof SchedulingRule;
		}
	}

	protected FeatureExportInfo fInfo;

	public FeatureExportJob(FeatureExportInfo info) {
		super(PDEUIMessages.FeatureExportJob_name); 
		fInfo = info;
		setRule(new SchedulingRule());
	}

	protected IStatus run(IProgressMonitor monitor) {
		String errorMessage = null;
		FeatureExportOperation op = createOperation();
		try {
			op.run(monitor);
		} catch (final CoreException e) {
			final Display display = getStandardDisplay();
			display.asyncExec(new Runnable() {
				public void run() {
					ErrorDialog.openError(display.getActiveShell(), PDEUIMessages.FeatureExportJob_error, PDEUIMessages.FeatureExportJob_problems, e.getStatus()); // 
					done(new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null)); //$NON-NLS-1$
				}
			});
			return Job.ASYNC_FINISH;
		}
		
		if (errorMessage == null && op.hasErrors()) 
			errorMessage = getLogFoundMessage();
		
		if (errorMessage != null) {
			final String em = errorMessage;
			getStandardDisplay().asyncExec(new Runnable() {
				public void run() {
					asyncNotifyExportException(em);
				}
			});
			return Job.ASYNC_FINISH;
		}
		return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null); //$NON-NLS-1$
	}
	
	protected FeatureExportOperation createOperation() {
		return new FeatureExportOperation(fInfo);
	}
	
	/**
	 * Returns the standard display to be used. The method first checks, if the
	 * thread calling this method has an associated disaply. If so, this display
	 * is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	private void asyncNotifyExportException(String errorMessage) {
		getStandardDisplay().beep();
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureExportJob_error, errorMessage); 
		done(new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", null)); //$NON-NLS-1$
	}

	protected String getLogFoundMessage() {
		return NLS.bind(PDEUIMessages.ExportJob_error_message, fInfo.destinationDirectory); 
	} 

}
