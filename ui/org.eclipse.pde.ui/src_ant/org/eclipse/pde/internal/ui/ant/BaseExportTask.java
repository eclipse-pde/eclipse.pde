package org.eclipse.pde.internal.ui.ant;

import org.apache.tools.ant.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.pde.internal.ui.wizards.exports.*;

/**
 * @author melhem
 *
 */
public abstract class BaseExportTask extends Task {
	
	protected String fDestination;
	protected String fZipFilename;
	protected int fExportType;
	protected boolean fExportSource;
	/**
	 * 
	 */
	public BaseExportTask() {
		fExportType = FeatureExportJob.EXPORT_AS_ZIP;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if (fDestination == null)
			throw new BuildException("No destination is specified");
		
		if (fExportType == FeatureExportJob.EXPORT_AS_ZIP && fZipFilename == null)
			throw new BuildException("No zip file is specified");
		
		getExportJob().schedule();
	}
	
	public void setExportType(String type) {
		if ("update".equals(type)) {
			fExportType = FeatureExportJob.EXPORT_AS_UPDATE_JARS;
		} else if ("directory".equals(type)){
			fExportType = FeatureExportJob.EXPORT_AS_DIRECTORY;
		}
	}
	
	public void setExportSource(String doExportSource) {
		fExportSource = "true".equals(doExportSource);
	}
	
	public void setDestination(String destination) {
		fDestination = destination;
	}
	
	public void setFilename(String filename) {
		fZipFilename = filename;
	}
	
	protected abstract Job getExportJob();
}
