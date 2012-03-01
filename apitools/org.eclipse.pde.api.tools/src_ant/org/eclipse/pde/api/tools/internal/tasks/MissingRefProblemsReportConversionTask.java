/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.search.MissingRefReportConverter;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Default task for converting the XML output from the apitooling.apiuse ants to HTML
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class MissingRefProblemsReportConversionTask extends CommonUtilsTask {

	private String xmlReportsLocation = null;
	private String htmlReportsLocation = null;

	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue);
	}

	/**
	 * Set the location where the html reports are generated.
	 * 
	 * <p>This is optional. If not set, the html files are created in the same folder as the
	 * xml files.</p>
	 * <p>The location is set using an absolute path.</p>
	 * 
	 * @param htmlFilesLocation the given the location where the html reports are generated
	 */
	public void setHtmlFiles(String htmlFilesLocation) {
		this.htmlReportsLocation = htmlFilesLocation;
	}

	/**
	 * Set the location where the xml reports are retrieved.
	 * 
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param xmlFilesLocation the given location to retrieve the xml reports
	 */
	public void setXmlFiles(String xmlFilesLocation) {
		this.xmlReportsLocation = xmlFilesLocation;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if(this.xmlReportsLocation == null) {
			throw new BuildException(Messages.no_xml_location);
		}
		if(this.htmlReportsLocation == null) {
			throw new BuildException(Messages.no_html_location);
		}
		if (this.debug) {
			System.out.println("XML report location: " + this.xmlReportsLocation); //$NON-NLS-1$
			System.out.println("HTML report location: " + this.htmlReportsLocation); //$NON-NLS-1$
		}
		try {
			Util.delete(new File(this.htmlReportsLocation));
			MissingRefReportConverter converter = new MissingRefReportConverter(this.htmlReportsLocation, this.xmlReportsLocation);
			ApiPlugin.DEBUG_USE_REPORT_CONVERTER = this.debug;
			converter.convert(null, new NullProgressMonitor());
			File index = converter.getReportIndex();
			System.out.println(NLS.bind(Messages.ApiUseReportConversionTask_conversion_complete, index.getAbsolutePath()));
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}
}
