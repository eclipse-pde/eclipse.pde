/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class UpdateClasspathJob extends Job {
	private static final String KEY_JOB_TITLE = "UpdateClasspathJob.title"; //$NON-NLS-1$

	private static final String KEY_TITLE = "UpdateClasspathJob.error.title"; //$NON-NLS-1$

	private static final String KEY_MESSAGE = "UpdateClasspathJob.error.message"; //$NON-NLS-1$

	private static final String KEY_UPDATE = "UpdateClasspathJob.task"; //$NON-NLS-1$

	IPluginModelBase[] fModels;

	/**
	 * @param name
	 */
	public UpdateClasspathJob(IPluginModelBase[] models) {
		super(PDEPlugin.getResourceString(KEY_JOB_TITLE));
		setPriority(Job.LONG);
		fModels = models;
	}
	/*
	 * return canceled
	 */
	public boolean doUpdateClasspath(IProgressMonitor monitor,
			IPluginModelBase[] models) throws CoreException {
		monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATE),
				models.length);
		try {
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				monitor.subTask(models[i].getPluginBase().getId());
				// no reason to compile classpath for a non-Java model
				IProject project = model.getUnderlyingResource().getProject();
				if (!project.hasNature(JavaCore.NATURE_ID)) {
					monitor.worked(1);
					continue;
				}
				ClasspathUtilCore.setClasspath(model, new SubProgressMonitor(
						monitor, 1));
				if (monitor.isCanceled())
					return false;
			}
		} finally {
			monitor.done();
		}
		return true;
	}

	class UpdateClasspathWorkspaceRunnable implements IWorkspaceRunnable {
		boolean fCanceled = false;
		public void run(IProgressMonitor monitor)
				throws CoreException {
			fCanceled = doUpdateClasspath(monitor, fModels);
		}
		public boolean isCanceled(){
			return fCanceled;
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.internal.jobs.InternalJob#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		try {
			UpdateClasspathWorkspaceRunnable runnable = new UpdateClasspathWorkspaceRunnable();
			PDEPlugin.getWorkspace().run(runnable, monitor);
			if(runnable.isCanceled()){
				return new Status(IStatus.CANCEL, IPDEUIConstants.PLUGIN_ID, IStatus.CANCEL, "",null);
			}

		} catch (CoreException e) {
			String title = PDEPlugin.getResourceString(KEY_TITLE);
			String message = PDEPlugin.getResourceString(KEY_MESSAGE);
			PDEPlugin.logException(e, title, message);
			return new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, IStatus.OK, message, e);
		}
		return new Status(IStatus.OK, IPDEUIConstants.PLUGIN_ID, IStatus.OK, "",null);
	}

}
