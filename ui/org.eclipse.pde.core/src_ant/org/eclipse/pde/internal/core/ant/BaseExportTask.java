/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.core.PDECoreMessages;

public abstract class BaseExportTask extends Task {

	protected String fDestination;
	protected String fZipFilename;
	protected boolean fToDirectory;
	protected boolean fUseJarFormat;
	protected boolean fExportSource;
	protected String fQualifier;
	protected boolean fAllowBinaryCycles;
	protected boolean fUseWorkspaceCompiledClasses;

	public BaseExportTask() {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if (fDestination == null)
			throw new BuildException("No destination is specified"); //$NON-NLS-1$

		if (!fToDirectory && fZipFilename == null)
			throw new BuildException("No zip file is specified"); //$NON-NLS-1$

		Job job = getExportJob(PDECoreMessages.BaseExportTask_pdeExport);

		// if running in ant runner, block until job is done.  Prevents Exiting before completed
		// blocking will cause errors if done when running in regular runtime.
		if (isAntRunner()) {
			try {
				job.schedule();
				job.join();
			} catch (InterruptedException e) {
			}
		} else
			job.schedule(2000);
	}

	public void setExportType(String type) {
		fToDirectory = !"zip".equals(type); //$NON-NLS-1$
	}

	public void setUseJARFormat(String useJarFormat) {
		fUseJarFormat = new Boolean(useJarFormat).booleanValue();
	}

	public void setExportSource(String doExportSource) {
		fExportSource = new Boolean(doExportSource).booleanValue();
	}

	public void setDestination(String destination) {
		fDestination = destination;
	}

	public void setFilename(String filename) {
		fZipFilename = filename;
	}

	public void setQualifier(String qualifier) {
		fQualifier = qualifier;
	}

	public void setAllowBinaryCycles(String allowBinaryCycles) {
		fAllowBinaryCycles = new Boolean(allowBinaryCycles).booleanValue();
	}

	public void setUseWorkspaceCompiledClasses(String useWorkspaceCompiledClasses) {
		fUseWorkspaceCompiledClasses = new Boolean(useWorkspaceCompiledClasses).booleanValue();
	}

	public boolean isAntRunner() {
		String args[] = Platform.getCommandLineArgs();
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-application")) //$NON-NLS-1$
				return args[i + 1].equals("org.eclipse.ant.core.antRunner"); //$NON-NLS-1$
		}
		return false;
	}

	protected abstract Job getExportJob(String jobName);
}
