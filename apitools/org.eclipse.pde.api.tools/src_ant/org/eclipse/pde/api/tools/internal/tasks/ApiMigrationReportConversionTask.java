/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.search.MigrationReportConvertor;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Task to convert a report generated from {@link ApiMigrationTask} to HTML
 * 
 * @since 1.0.1
 * @see ApiMigrationTask
 */
public class ApiMigrationReportConversionTask extends CommonUtilsTask {

	private String xmlReportsLocation = null;
	private String htmlReportsLocation = null;
	private String xsltFileLocation = null;
	private String[] filterPatterns = null;
	private String[] toPatterns = null;
	
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
	
	/**
	 * Set the group of {@link String} patterns to use as heuristics to filter
	 * references to names matching any of the given patterns during the report conversion
	 * @param patterns
	 */
	public void setToFilterPatterns(String patterns) {
		this.toPatterns = parsePatterns(patterns);
	}
	
	/**
	 * Set the group of {@link String} patterns to use as heuristics to filter
	 * references from names matching any of the given patterns during the report conversion
	 * @param patterns
	 */
	public void setFilterPatterns(String patterns) {
		this.filterPatterns = parsePatterns(patterns);	
	}
	
	/**
	 * Sets the location of the XSLT file to use in the conversion of the XML
	 * the HTML.
	 * 
	 * <p>This is optional. If none is specified, then a default one is used.</p>
	 * 
	 * <p>The location is an absolute path.</p>
	 * 
	 * @param xsltFileLocation
	 */
	public void setXSLTFile(String xsltFileLocation) {
		this.xsltFileLocation = xsltFileLocation;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		if (this.debug) {
			System.out.println("XML report location: " + this.xmlReportsLocation); //$NON-NLS-1$
			System.out.println("HTML report location: " + this.htmlReportsLocation); //$NON-NLS-1$
			if (this.xsltFileLocation == null) {
				System.out.println("No XSLT file specified: using default"); //$NON-NLS-1$}
			} else {
				System.out.println("XSLT file location: " + this.xsltFileLocation); //$NON-NLS-1$}
			}
		}
		try {
			Util.delete(new File(this.htmlReportsLocation));
			MigrationReportConvertor converter = new MigrationReportConvertor(this.htmlReportsLocation, this.xmlReportsLocation, this.toPatterns, this.filterPatterns);
			ApiPlugin.DEBUG_USE_REPORT_CONVERTER = this.debug;
			converter.convert(this.xsltFileLocation, null);
			File index = converter.getReportIndex();
			System.out.println(NLS.bind(
					Messages.ApiUseReportConversionTask_conversion_complete,
					index.getAbsolutePath()));
		}
		catch(Exception e) {
			throw new BuildException(e);
		}
	}
}
