/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.jobs.Job;

public abstract class BaseExportTask extends Task {
	
	protected String fDestination;
	protected String fZipFilename;
	protected boolean fToDirectory;
	protected boolean fUseJarFormat;
	protected boolean fExportSource;
	protected String fJavacTarget;
	protected String fJavacSource;

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
		
		getExportJob().schedule(2000);
	}
	
	public void setExportType(String type) {
		fToDirectory = !"zip".equals(type); //$NON-NLS-1$
	}
	
	public void setUseJARFormat(String useJarFormat) {
		fUseJarFormat = "true".equals(useJarFormat); //$NON-NLS-1$
	}
	
	public void setExportSource(String doExportSource) {
		fExportSource = "true".equals(doExportSource); //$NON-NLS-1$
	}
	
	public void setDestination(String destination) {
		fDestination = destination;
	}
	
	public void setFilename(String filename) {
		fZipFilename = filename;
	}
	
	public void setTarget(String target) {
		fJavacTarget = target;
	}
	
	public void setSource(String source) {
		fJavacSource = source;
	}
	
	protected abstract Job getExportJob();
}
