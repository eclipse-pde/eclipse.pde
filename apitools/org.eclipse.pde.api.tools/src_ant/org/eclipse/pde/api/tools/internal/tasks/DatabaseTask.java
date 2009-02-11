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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.apache.tools.ant.BuildException;

/**
 * Common attributes / methods for a task that connects to a database
 * 
 * @since 1.0.1
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class DatabaseTask extends UseTask {

	protected static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver"; //$NON-NLS-1$
	protected static final String MYSQL_DRIVER_NAME = "mysql"; //$NON-NLS-1$
	
	protected String dbName = null;
	protected String dbDriverName = null;
	protected String dbDomainName = null;
	protected int dbPort = 3306;
	protected String username = null;
	protected String password = null;
	
	/**
	 * Validates the current set of database parameters and throws a {@link org.apache.tools.ant.BuildException}
	 * if they are not valid.
	 */
	protected void validateDBConnectionParameters() throws BuildException {
		StringBuffer buffer = new StringBuffer();
		if(this.dbName == null) {
			buffer.append("[DB name] "); //$NON-NLS-1$
		}
		if(this.dbDriverName == null) {
			buffer.append("[DB driver name] "); //$NON-NLS-1$
		}
		if(this.dbDomainName == null) {
			buffer.append("[DB domain name] "); //$NON-NLS-1$
		}
		if(this.username == null) {
			buffer.append("[user name] "); //$NON-NLS-1$
		}
		if(this.password == null) {
			buffer.append("[password]"); //$NON-NLS-1$
		}
		if(buffer.length() > 0) {
			throw new BuildException(MessageFormat.format(Messages.DatabaseTask_missing_db_connect_arguments, new String[] {buffer.toString()}));
		}
		assertParameters();
	}
	
	/**
	 * Pre-formats the DB connection URL 
	 * @return the pre-formatted connection URL
	 */
	protected String getDBUrl() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("jdbc:"); //$NON-NLS-1$
		buffer.append(this.dbDriverName);
		buffer.append("://"); //$NON-NLS-1$
		buffer.append(this.dbDomainName);
		buffer.append(":"); //$NON-NLS-1$
		buffer.append(this.dbPort);
		buffer.append("/"); //$NON-NLS-1$
		buffer.append(this.dbName);
		buffer.append("?user="); //$NON-NLS-1$
		buffer.append(this.username);
		buffer.append("&password="); //$NON-NLS-1$
		buffer.append(this.password);
		return buffer.toString();
	}
	
	/**
	 * Returns the driver class that should be loaded when trying to connect
	 * @param drivername the name of the driver as it appears in a database connection URI:<br>
	 * <code>jdbc:[driver name]://[domain]:[port]/[dbname]?user=[username]&password=[password]</code>
	 * 
	 * @return the name of the class to load for the given driver name or <code>null</code>
	 */
	protected String getDriverClass(String drivername) {
		if(MYSQL_DRIVER_NAME.equals(drivername)) {
			return MYSQL_DRIVER;
		}
		return null;
	}
	
	/**
	 * Attempts a connection to the database specified via the database URI information given to the task.
	 * @param dburl the URL to connect to the database
	 * @return a connection to the DB or <code>null</code> if the driver class could not be loaded
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	protected Connection doConnection() {
		try {
			String clazz = getDriverClass(this.dbDriverName);
			if(clazz != null) {
				Class.forName(clazz).newInstance();
				return DriverManager.getConnection(getDBUrl());
			}
		}
		catch(SQLException sqle) {
			throw new BuildException(Messages.ApiUseDBTask_sql_connection_exception, sqle);
		}
		catch(InstantiationException ie) {
			throw new BuildException(Messages.ApiUseDBTask_driver_instantiation_exception, ie);
		}
		catch(IllegalAccessException iae) {
			throw new BuildException(Messages.ApiUseDBTask_illegal_access_loading_driver, iae);
		}
		catch(ClassNotFoundException cnfe) {
			throw new BuildException(Messages.ApiUseDBTask_driver_class_not_found, cnfe);
		}
		return null;
	}
}
