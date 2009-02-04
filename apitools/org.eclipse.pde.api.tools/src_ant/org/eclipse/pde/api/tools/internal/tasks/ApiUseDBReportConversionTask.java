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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.search.DBUseReporter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Ant task that can take an XML generated API use report and 'convert' it to be in DB 
 * of choice.
 * 
 * @since 1.0.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ApiUseDBReportConversionTask extends DatabaseTask {

	private String xmlReportsLocation = null;
	
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
		validateDBConnectionParameters();
		if (this.xmlReportsLocation == null) {
			throw new BuildException(Messages.missing_xml_files_location);
		}
		try {
			Connection connection = doConnection();
			if(connection == null) {
				throw new BuildException(Messages.ApiUseDBTask_connection_could_not_be_established);
			}
			File reportroot = new File(this.xmlReportsLocation);
			if (!reportroot.exists() || !reportroot.isDirectory()) {
				throw new BuildException(Messages.bind(Messages.invalid_directory_name, this.xmlReportsLocation));
			}
			long start = 0;
			DBUseReporter reporter = new DBUseReporter(connection, this.debug);
			if(this.debug) {
				System.out.println("Reporting components that were not searched..."); //$NON-NLS-1$
				start = System.currentTimeMillis();
			}
			reportNotSearched(reporter, reportroot);
			if(this.debug) {
				System.out.println("done in: "+(System.currentTimeMillis()-start)+" ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		catch(ClassNotFoundException cnf) {
			throw new BuildException(Messages.ApiUseDBTask_driver_class_not_found, cnf);
		}
		catch (IllegalAccessException iae) {
			throw new BuildException(Messages.ApiUseDBTask_illegal_access_loading_driver, iae);
		}
		catch (SQLException sqle) {
			throw new BuildException(Messages.ApiUseDBTask_sql_connection_exception, sqle);
		} 
		catch (InstantiationException ie) {
			throw new BuildException(Messages.ApiUseDBTask_driver_instantiation_exception, ie);
		}
	}
	
	/**
	 * Reports components that were not searched and their reasons why to the database
	 * @param reporter the {@link DBUseReporter} to report to
	 */
	private void reportNotSearched(DBUseReporter reporter, File reportroot) {
		File file = new File(reportroot, "not_searched.xml"); //$NON-NLS-1$
		if(!file.exists()) {
			if(this.debug) {
				System.out.println("no skipped component information was found"); //$NON-NLS-1$
			}
			return;
		}
		try {
			Element root = Util.parseDocument(Util.getFileContentAsString(file));
			NodeList components = root.getElementsByTagName(IApiXmlConstants.ELEMENT_COMPONENT);
			Element component = null;
			String id = null, nodesc = null, excluded = null;
			SkippedComponent[] skipped = new SkippedComponent[components.getLength()];
			for (int i = 0; i < components.getLength(); i++) {
				component = (Element) components.item(i);
				id = component.getAttribute(IApiXmlConstants.ATTR_ID);
				nodesc = component.getAttribute(ApiUseTask.NO_API_DESCRIPTION);
				excluded = component.getAttribute(ApiUseTask.EXCLUDED);
				skipped[i] = new SkippedComponent(id, Boolean.valueOf(nodesc).booleanValue(), Boolean.valueOf(excluded).booleanValue());
			}
			reporter.reportNotSearched(skipped);
		}
		catch(CoreException ce) {}
	}
}
