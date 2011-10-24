/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;

import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.ITargetDefinition;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.SearchablePluginsManager;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * Adds/Removes the target bundles to/from Java search
 * 
 * @since 3.6
 */
public class AddToJavaSearchJob extends WorkspaceJob {

	private static final String JOB_FAMILY_ID = "AddToJavaSearchJob"; //$NON-NLS-1$

	private IPluginModelBase[] fBundles;
	private boolean fAdd;
	private ITargetDefinition fTargetDefinition;

	/**
	 * Adds/Removes the target bundles to/from Java search
	 * 
	 * @param target	The target definition whose bundles are to be added/removed.
	 */
	public static void synchWithTarget(ITargetDefinition target) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		AddToJavaSearchJob job = new AddToJavaSearchJob(target);
		job.schedule();
	}

	/**
	 * Removes all bundles from Java search
	 */
	public static void clearAll() {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		AddToJavaSearchJob job = new AddToJavaSearchJob(null, false);
		job.schedule();
	}

	/**
	 * Adds or removes a set of bundles from Java search
	 * 
	 * @param bundles	bundles that are to be added/removed.
	 * @param add		<code>true</code> to add, <code>false></code> to remove
	 */
	public static void changeBundles(IPluginModelBase[] bundles, boolean add) {
		Job.getJobManager().cancel(JOB_FAMILY_ID);
		AddToJavaSearchJob job = new AddToJavaSearchJob(bundles, add);
		job.schedule();
	}

	/**
	 * Updates the contents of the java search scope setting its contents to the
	 * contents of the given target definition.
	 * 
	 * @param target target to update search scope with
	 */
	private AddToJavaSearchJob(ITargetDefinition target) {
		super(PDEUIMessages.AddToJavaSearchJob_0);
		fTargetDefinition = target;
		fAdd = true;
		fBundles = null;
	}

	/**
	 * Updates the contents of the java search scope with the given set of bundles.
	 * Adds them to the scope if add to <code>true</code> otherwise they are removed.
	 * Calling this method with bundles being null and add being <code>false</code>
	 * will clear the java search scope.
	 * 
	 * @param bundles set of bundles to add or remove
	 * @param add whether to add or remove the bundles
	 */
	private AddToJavaSearchJob(IPluginModelBase[] bundles, boolean add) {
		super(PDEUIMessages.AddToJavaSearchJob_0);
		fTargetDefinition = null;
		fBundles = bundles;
		fAdd = add;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.WorkspaceJob#runInWorkspace(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		int ticks = fTargetDefinition != null ? 100 : 25;
		SubMonitor subMon = SubMonitor.convert(monitor, ticks);
		try {
			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			SearchablePluginsManager manager = PDECore.getDefault().getSearchablePluginsManager();

			// If synching with a target, clear the project and check that the target is resolved
			if (fTargetDefinition != null) {

				manager.removeAllFromJavaSearch();

				if (!fTargetDefinition.isResolved()) {
					IStatus status = fTargetDefinition.resolve(subMon.newChild(50));
					if (!status.isOK()) {
						return status;
					}
					subMon.subTask(""); //$NON-NLS-1$
				} else {
					subMon.worked(50);
				}

				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				TargetBundle[] bundles = fTargetDefinition.getBundles();
				fAdd = true;
				List models = new ArrayList(bundles.length);
				for (int index = 0; index < bundles.length; index++) {
					IPluginModelBase model = PluginRegistry.findModel(bundles[index].getBundleInfo().getSymbolicName());
					if (model != null) {
						models.add(model);
					}
				}
				subMon.worked(25);
				fBundles = (IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
			}

			if (subMon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}

			if (fAdd) {
				manager.addToJavaSearch(fBundles);
			} else {
				if (fBundles != null) {
					manager.removeFromJavaSearch(fBundles);
				} else {
					manager.removeAllFromJavaSearch();
				}
			}
			subMon.worked(25);

			return Status.OK_STATUS;

		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#belongsTo(java.lang.Object)
	 */
	public boolean belongsTo(Object family) {
		return JOB_FAMILY_ID.equals(family);
	}

}
