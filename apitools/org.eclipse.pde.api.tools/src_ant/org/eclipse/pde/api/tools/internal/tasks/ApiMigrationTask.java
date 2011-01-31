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

package org.eclipse.pde.api.tools.internal.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.search.ReferenceLookupVisitor;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;
import org.eclipse.pde.api.tools.internal.search.UseScanParser;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Ant task for performing analysis of an API use scan against an alterante target (migration candidate)
 */
public final class ApiMigrationTask extends CommonUtilsTask {
	
	/**
	 * Set of project names that were not searched
	 */
	private TreeSet notsearched = null;
	/**
	 * The regex pattern to use to compose the scope
	 */
	private String scopepattern = null;
	/**
	 * The regex pattern to use to compose the reference set of component ids
	 */
	private String referencepattern = null;
	
	/**
	 * handle to the baseline install dir to delete after the scan completes
	 */
	private File baselinedir = null;
	
	/**
	 * Location of the API use scan to resolve in the migration candidate.
	 * This can be the root directory containing an 'xml' sub directory or the 'xml'
	 * directory itself.
	 */
	private String scanLocation = null;
	
	/**
	 * Set the location of the product you want to use as the migration candidate.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param location the location for the migration candidate to consider
	 */
	public void setCandidate(String location) {
		this.currentBaselineLocation = location;
	}

	/**
	 * Set the regular expression pattern used to build the scope of elements to search for 
	 * references from in the product location.
	 * 
	 * <p>
	 * The pattern must be a well-formatted regular expression as
	 * defined here: http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html 
	 * </p>
	 * @param scopepattern
	 */
	public void setScopePattern(String scopepattern) {
		this.scopepattern = scopepattern;
	}
	
	/**
	 * Set the regular expression pattern used to build the scope of elements to search for 
	 * references to in the product location.
	 * 
	 * <p>
	 * The pattern must be a well-formatted regular expression as
	 * defined here: http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.html 
	 * </p>
	 * @param referencepattern
	 */
	public void setReferencePattern(String referencepattern) {
		this.referencepattern = referencepattern;
	}
	
	/**
	 * Set the output location where the reports will be generated.
	 * 
	 * <p>Once the task is completed, reports are available in this directory using a structure
	 * similar to the filter root. A sub-folder is created for each component that has problems
	 * to be reported. Each sub-folder contains a file called "report.xml". </p>
	 * 
	 * <p>A special folder called "allNonApiBundles" is also created in this folder that contains a xml file called
	 * "report.xml". This file lists all the bundles that are not using the API Tools nature.</p>
	 * 
	 * @param reportlocation the given location for the reference baseline to analyze
	 */
	public void setReport(String reportlocation) {
		this.reportLocation = reportlocation;
	}
	
	/**
	 * Set the location of an existing API use scan containing references to re-resolve in the
	 * migration candidate. This can be the root directory containing 'xml' and 'html' subdirectories
	 * or the 'xml' directory itself.
	 * 
	 * @param scanLocation the location of an existing API use scan
	 */
	public void setUseScan(String scanLocation) {
		this.scanLocation = scanLocation;
	}
	
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
	 * @see org.eclipse.pde.api.tools.internal.tasks.UseTask#assertParameters()
	 */
	protected void assertParameters() throws BuildException {
		if (this.reportLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(
					Messages.ApiUseTask_missing_report_location, 
					new String[] {this.reportLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (this.currentBaselineLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(
					Messages.ApiUseTask_missing_baseline_argument, 
					new String[] {this.currentBaselineLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (this.scanLocation == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(
					Messages.ApiMigrationTask_missing_scan_location, 
					new String[] {this.scanLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}		
		String xmlLocation = scanLocation;
		File file = new File(xmlLocation);
		if (!file.exists()) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(
					Messages.ApiMigrationTask_scan_location_not_exist, 
					new String[] {this.scanLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (!file.isDirectory()) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(
					Messages.ApiMigrationTask_scan_location_not_dir, 
					new String[] {this.scanLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}		
		if (this.reportLocation.equals(scanLocation)) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(
					Messages.ApiMigrationTask_scan_locatoin_same_as_report_location, 
					new String[] {this.scanLocation}));
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}

	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		assertParameters();
		writeDebugHeader();
		cleanReportLocation();
		
		IApiBaseline baseline = getBaseline(CURRENT_BASELINE_NAME, this.currentBaselineLocation);
		try {
			String xmlLocation = scanLocation;
			File file = new File(xmlLocation);
			File nested = new File(file, "xml"); //$NON-NLS-1$
			if (nested.exists() && nested.isDirectory()) {
				file = nested;
			}
			ReferenceLookupVisitor lookup = new ReferenceLookupVisitor(baseline, this.reportLocation);
			lookup.setAnalysisScope(scopepattern);
			lookup.setTargetScope(referencepattern);
			
			FilteredElements excludedElements = CommonUtilsTask.initializeFilteredElements(this.excludeListLocation, baseline, this.debug);
			if (this.debug) {
				System.out.println("===================================================================================="); //$NON-NLS-1$
				System.out.println("Excluded elements list:"); //$NON-NLS-1$
				System.out.println(excludedElements);
			}
			lookup.setExcludedElements(excludedElements);

			FilteredElements includedElements = CommonUtilsTask.initializeFilteredElements(this.includeListLocation, baseline, this.debug);
			if (this.debug) {
				System.out.println("===================================================================================="); //$NON-NLS-1$
				System.out.println("Included elements list:"); //$NON-NLS-1$
				System.out.println(includedElements);
			}
			lookup.setIncludedElements(includedElements);

			UseScanParser parser = new UseScanParser();
			parser.parse(file.getAbsolutePath(), new NullProgressMonitor(), lookup);
		}
		catch(CoreException ce) {
			throw new BuildException(ce.getStatus().getMessage(), ce);
		}
		catch (Exception e) {
			throw new BuildException(e.getMessage(), e);
		}
		finally {
			if(baseline != null) {
				baseline.dispose();
				deleteBaseline(this.currentBaselineLocation, this.baselinedir);
			}
		}
	}
	
	/**
	 * Returns if we should add the given component to our search scope
	 * @param component
	 * @param pattern
	 * @param allowresolve
	 * @return true if the given component should be considered, false otherwise
	 * @throws CoreException
	 */
	boolean acceptComponent(IApiComponent component, Pattern pattern, boolean allowresolve) throws CoreException {
		if(!allowresolve) {
			ResolverError[] errors = component.getErrors();
			if(errors != null) {
				this.notsearched.add(new SkippedComponent(component.getSymbolicName(), component.getVersion(), errors)); 
				return false;
			}
		}
		if(component.isSystemComponent()) {
			return false;
		}
		if(pattern != null) {
			return pattern.matcher(component.getSymbolicName()).matches();
		}
		return true;
	}
	
	/**
	 * Prepares and creates and new baseline with the given name from the given location. The
	 * returned {@link IApiBaseline} is not checked for resolution errors or consistency. If <code>null</code>
	 * is passed in as a location <code>null</code> is returned.
	 * 
	 * @param name the name to give to the baseline
	 * @param location the location the baseline should be prepared from. If <code>null</code> is passed in, <code>null</code>
	 * is returned
	 * @return a new {@link IApiBaseline} with the given name from the given location or <code>null</code> if the given location
	 * is <code>null</code>
	 */
	protected IApiBaseline getBaseline(String name, String location) {
		if(location == null) {
			return null;
		}
		//extract the baseline to examine
		long time = 0;
		if (this.debug) {
			time = System.currentTimeMillis();
			System.out.println("Preparing '"+name+"' baseline installation..."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		File installdir = extractSDK(name, location);
		if (this.debug) {
			System.out.println("done in: " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
			time = System.currentTimeMillis();
		}
		//create the baseline to examine
		if(this.debug) {
			time = System.currentTimeMillis();
			System.out.println("Creating '"+name+"' baseline..."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		IApiBaseline baseline = createBaseline(name, installdir.getAbsolutePath(), this.eeFileLocation);
		if (this.debug) {
			System.out.println("done in: " + (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.baselinedir = installdir;
		return baseline;
	}
	
	/**
	 * Cleans the report location specified by the parameter {@link CommonUtilsTask#reportLocation}
	 */
	protected void cleanReportLocation() {
		if(this.reportLocation == null) {
			return;
		}
		long time = 0;
		if(this.debug) {
			time = System.currentTimeMillis();
			System.out.println("Cleaning report location..."); //$NON-NLS-1$
		}
		File file = new File(this.reportLocation);
		if(file.exists()) {
			Util.delete(file);
		}
		if(this.debug) {
			System.out.println("done in: "+ (System.currentTimeMillis() - time) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * Writes a general header of debug information iff the debug flag is set to true
	 */
	protected void writeDebugHeader() {
		if (this.debug) {
			System.out.println("Migration candidate to analyze : " + this.currentBaselineLocation); //$NON-NLS-1$
			System.out.println("Report location : " + this.reportLocation); //$NON-NLS-1$
			System.out.println("Scan location : " + this.scanLocation); //$NON-NLS-1$
			if(this.scopepattern == null) {
				System.out.println("No scope pattern defined - searching all bundles"); //$NON-NLS-1$
			}
			else {
				System.out.println("Scope pattern : " + this.scopepattern); //$NON-NLS-1$
			}
			if(this.referencepattern == null) {
				System.out.println("No baseline pattern defined - reporting references to all bundles"); //$NON-NLS-1$
			}
			else {
				System.out.println("Baseline pattern : " + this.referencepattern); //$NON-NLS-1$
			}
			System.out.println("-----------------------------------------------------------------------------------------------------"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Set the exclude list location.
	 * 
	 * <p>The exclude list is used to know what bundles should excluded from the xml report generated by the task
	 * execution. Lines starting with '#' are ignored from the excluded elements.</p>
	 * <p>The format of the exclude list file looks like this:</p>
	 * <pre>
	 * # DOC BUNDLES
	 * org.eclipse.jdt.doc.isv
	 * org.eclipse.jdt.doc.user
	 * org.eclipse.pde.doc.user
	 * org.eclipse.platform.doc.isv
	 * org.eclipse.platform.doc.user
	 * # NON-ECLIPSE BUNDLES
	 * com.ibm.icu
	 * com.jcraft.jsch
	 * javax.servlet
	 * javax.servlet.jsp
	 * ...
	 * </pre>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param excludeListLocation the given location for the excluded list file
	 */
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
	
	/**
	 * Set the include list location.
	 * 
	 * <p>The include list is used to know what bundles should included from the xml report generated by the task
	 * execution. Lines starting with '#' are ignored from the included elements.</p>
	 * <p>The format of the include list file looks like this:</p>
	 * <pre>
	 * # DOC BUNDLES
	 * org.eclipse.jdt.doc.isv
	 * org.eclipse.jdt.doc.user
	 * org.eclipse.pde.doc.user
	 * org.eclipse.platform.doc.isv
	 * org.eclipse.platform.doc.user
	 * # NON-ECLIPSE BUNDLES
	 * com.ibm.icu
	 * com.jcraft.jsch
	 * javax.servlet
	 * javax.servlet.jsp
	 * ...
	 * </pre>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param includeListLocation the given location for the included list file
	 */
	public void setIncludeList(String includeListLocation) {
		this.includeListLocation = includeListLocation;
	}
}
