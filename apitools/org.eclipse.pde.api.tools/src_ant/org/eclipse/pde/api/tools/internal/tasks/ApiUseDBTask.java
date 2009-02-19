/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.sql.Connection;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.search.DBUseReporter;
import org.eclipse.pde.api.tools.internal.search.SkippedComponent;

/**
 * Api usage reporting task that reports to a database.
 * 
 * @since 1.0.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ApiUseDBTask extends DatabaseTask {
	
	/**
	 * Set the flag to indicate if the usage scan should try to proceed when an error is encountered
	 * or stop.
	 * <p>
	 * The default value is <code>false</code>.
	 * </p>
	 * @param proceed if the scan should try to continue in the face of errors. Valid values 
	 * are <code>true</code> or <code>false</code>.
	 */
	public void setProceedOnError(String proceed) {
		this.proceedonerror = Boolean.valueOf(proceed).booleanValue();
	}
	
	/**
	 * Set the flag to indicate if the usage search should include system libraries in the scope and baseline.
	 * <p>
	 * The default value is <code>false</code>.
	 * </p>
	 * @param include if system libraries should be considered in the search scope and baseline. Valid values 
	 * are <code>true</code> or <code>false</code>.
	 */
	public void setIncludeSystemLibraries(String include) {
		this.includesystemlibraries = Boolean.valueOf(include).booleanValue();
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
	 * Set the name of the database to connect to
	 *
	 * <p>For example <code>mydatabase</code>, as it would appear in the driver URL:<br><br>
	 * <code>jdbc:[driver name]://[domain]:[port]/mydatabase?user=[username]&password=[password]</code>
	 * 
	 * @param dbname the name of the database to connect to
	 */
	public void setDBName(String dbname) {
		this.dbName = dbname;
	}
	
	/**
	 * Set the kind of database to connect to as it would appear in the driver connection URL.
	 * 
	 * <p>For example <code>mysql</code>, as it would appear in the driver URL:<br><br>
	 * <code>jdbc:mysql://[domain]:[port]/[dbname]?user=[username]&password=[password]</code>
	 * 
	 * @param drivername the name of the driver to use to connect
	 */
	public void setDBDriverName(String drivername) {
		this.dbDriverName = drivername;
	}
	
	/**
	 * Sets the domain name of the database to connect to as it would appear in the driver connection URL.
	 * 
	 * <p>For example <code>my.domain.com</code>, as it would appear in the driver URL:<br><br>
	 * <code>jdbc:[driver name]://my.domain.com:[port]/[dbname]?user=[username]&password=[password]</code>
	 * 
	 * @param domain the domain hosting the database
	 */
	public void setDBDomainName(String domain) {
		this.dbDomainName = domain;	
	}
	
	/**
	 * Set the port number of the database to connect to as it would appear in the driver connection URL.
	 * If the given port number is not a valid integer the default port number is used. (i.e. the port number is 
	 * left off the driver connection URL)
	 * 
	 * <p>For example <code>3308</code>, as it would appear in the driver URL:<br><br>
	 * <code>jdbc:[driver name]://[domain]:3308/[dbname]?user=[username]&password=[password]</code>
	 * 
	 * @param port the port the database is available on
	 */
	public void setDBPort(String port) {
		try {
			this.dbPort = Integer.parseInt(port);
		}
		catch(NumberFormatException nfe) {}
	}
	
	/**
	 * Set the user name to use when connecting to the database as it would appear in the driver connection URL.
	 * 
	 * <p>For example <code>myusername</code>, as it would appear in the driver URL:<br><br>
	 * <code>jdbc:[driver name]://[domain]:[port]/[dbname]?user=myusername&password=[password]</code>
	 * 
	 * @param username the user name to use when connecting
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	
	/**
	 * Set the password to use when connecting to the database as it would appear in the driver connection URL.
	 * 
	 * <p>For example <code>mypassword</code>, as it would appear in the driver URL:<br><br>
	 * <code>jdbc:[driver name]://[domain]:[port]/[dbname]?user=[username]&password=mypassword</code>
	 * 
	 * @param password the password to use when connecting
	 */
	public void setPassword(String password) {
		this.password = password;
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
	 * Set the execution environment file to use.
	 * <p>By default, an execution environment file corresponding to a JavaSE-1.6 execution environment
	 * is used.</p>
	 * <p>The file is specified using an absolute path. This is optional.</p> 
	 *
	 * @param eeFileLocation the given execution environment file
	 */
	public void setEEFile(String eeFileLocation) {
		this.eeFileLocation = eeFileLocation;
	}
	
	/**
	 * Sets if references to API types should be considered in the search.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 * 
	 * @param considerapi the given value
	 */
	public void setConsiderAPI(String considerapi) {
		this.considerapi = Boolean.toString(true).equals(considerapi);
	}
	
	/**
	 * Sets if references to internal types should be considered in the search.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 * 
	 * @param considerapi the given value
	 */
	public void setConsiderInternal(String considerinternal) {
		this.considerinternal = Boolean.toString(true).equals(considerinternal);
	}
	
	/**
	 * Set the location of the current product or baseline that you want to search.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 *
	 * @param baselineLocation the given location for the baseline to analyze
	 */
	public void setBaseline(String baselineLocation) {
		this.currentBaselineLocation = baselineLocation;
	}
	
	/**
	 * Sets a flag to indicate if projects that have not been set up for API tooling should be allowed 
	 * in the search scope.
	 * 
	 * @param includenonapi if non- API enabled projects should be included in the search scope. Valid values 
	 * are <code>true</code> or <code>false</code>.
	 */
	public void setIncludeNonApiProjects(String includenonapi) {
		this.includenonapi = Boolean.valueOf(includenonapi).booleanValue();
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	public void execute() throws BuildException {
		validateDBConnectionParameters();
		writeDebugHeader();
		
		IApiBaseline baseline = getBaseline(CURRENT_BASELINE_NAME, this.currentBaselineLocation);
		IApiBaseline scope = getBaseline(SCOPE_BASELINE_NAME, this.scopeLocation);
		initializeExcludeSet(scope);
		try {
			Connection connection = doConnection();
			if(connection == null) {
				throw new BuildException(Messages.ApiUseDBTask_connection_could_not_be_established);
			}
			DBUseReporter reporter = new DBUseReporter(connection, this.debug);
			doSearch(baseline, scope, reporter);
			reporter.reportNotSearched((SkippedComponent[]) this.notsearched.toArray(new SkippedComponent[this.notsearched.size()]));
		}
		catch(CoreException ce) {
			throw new BuildException(Messages.ApiUseTask_search_engine_problem, ce);
		}
	}
}
