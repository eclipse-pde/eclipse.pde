/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.pde.internal.core.ant;

import java.io.File;
import org.apache.tools.ant.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.tasks.TaskHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.target.ExportTargetJob;
import org.eclipse.pde.internal.core.target.TargetPlatformService;

/**
 * Exports the bundles and plug-ins of a target definition to a directory
 */
public class TargetPlatformProvisionTask extends Task {

	private File targetFile;
	private File destinationDirectory;
	private boolean clearDestination;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {

		try {
			BundleHelper.getDefault().setLog(this);
			run();
		} catch (CoreException e) {
			throw new BuildException(TaskHelper.statusToString(e.getStatus(), null).toString());
		} finally {
			BundleHelper.getDefault().setLog(null);
		}

	}

	private void export(final ITargetDefinition targetDefinition) throws CoreException {
		// export using Job to allow progress reporting when run inside IDE
		ExportTargetJob exportTargetJob = new ExportTargetJob(targetDefinition, destinationDirectory.toURI(), clearDestination);
		exportTargetJob.schedule();
		try {
			exportTargetJob.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CoreException(Status.CANCEL_STATUS);
		}
	}

	private IStatus resolve(final ITargetDefinition targetDefinition) throws CoreException {
		// resolve using Job to allow progress reporting when run inside IDE
		final IStatus[] status = new IStatus[1];
		Job resolveJob = new Job(NLS.bind("Resolving {0}", null != targetDefinition.getName() && targetDefinition.getName().length() > 0 ? targetDefinition.getName() : targetFile.getName())) {
			protected IStatus run(IProgressMonitor monitor) {
				status[0] = targetDefinition.resolve(monitor);
				return Status.OK_STATUS;
			}
		};
		resolveJob.setPriority(Job.LONG);
		resolveJob.schedule();
		try {
			resolveJob.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CoreException(Status.CANCEL_STATUS);
		}
		return status[0];
	}

	private void run() throws CoreException {
		if (null == targetFile)
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.TargetPlatformProvisionTask_ErrorDefinitionNotSet));
		if (!targetFile.isFile() || !targetFile.canRead())
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(PDECoreMessages.TargetPlatformProvisionTask_ErrorDefinitionNotFoundAtSpecifiedLocation, targetFile)));
		if (null == destinationDirectory)
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, PDECoreMessages.TargetPlatformProvisionTask_ErrorDestinationNotSet));

		final ITargetDefinition targetDefinition = TargetPlatformService.getDefault().getTarget(targetFile.toURI()).getTargetDefinition();

		log("Resolving target definition...");
		IStatus status = resolve(targetDefinition);
		if (status.matches(IStatus.ERROR | IStatus.CANCEL))
			throw new CoreException(status);
		else if (!status.isOK()) {
			log(TaskHelper.statusToString(status, null).toString(), Project.MSG_WARN);
		}

		log("Exporting target definition...");
		export(targetDefinition);
	}

	/**
	 * Set whether the destination should be cleared prior to provisioning.
	 * @param clearDestination
	 */
	public void setClearDestination(boolean clearDestination) {
		this.clearDestination = clearDestination;
	}

	/**
	 * Set the folder in which the target will be provisioned.
	 * @param destinationDirectory
	 */
	public void setDestinationDirectory(File destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}

	/**
	 * Set the target file to provision
	 * @param targetFile
	 */
	public void setTargetFile(File targetFile) {
		this.targetFile = targetFile;
	}

}
