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
package org.eclipse.pde.api.tools.internal.search;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.tasks.CommonUtilsTask;

/**
 * Reporter that reports results to a database
 * 
 * @since 1.0.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class DBUseReporter implements IApiSearchReporter {

	private Connection connection = null;
	private boolean debug = false;
	
	/**
	 * Constructor
	 * @param connection the database connection to report results to
	 * @param debug if statement execution results should be reported
	 */
	public DBUseReporter(Connection connection, boolean debug) {
		this.connection = connection;
		this.debug = debug;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter#reportResults(org.eclipse.pde.api.tools.internal.provisional.model.IApiElement, org.eclipse.pde.api.tools.internal.provisional.builder.IReference[])
	 */
	public void reportResults(IApiElement element, IReference[] references) {
		if(this.connection == null) {
			return;
		}
		executeStatement(getInsertQuery(element, references));
	}

	/**
	 * Executes a given query
	 * @param query
	 */
	private void executeStatement(String query) {
		if(query == null) {
			if(this.debug) {
				System.out.println("A null query was attempted to be executed"); //$NON-NLS-1$
			}
			return;
		}
		try {
			if(this.connection.isClosed()) {
				return;
			}
			Statement statement = this.connection.createStatement();
			statement.execute(query);
			int updatecount = statement.getUpdateCount();
			if(this.debug) {
				if(updatecount < 1) {
					System.out.println("The executed statement: "+statement.toString()+"did not cause any updates"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				SQLWarning warning = statement.getWarnings();
				if(warning != null) {
					System.out.println("The executed statement: "+statement.toString()+" had the following warning(s): "+warning); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		catch(SQLException sqe) {}
	}
	
	/**
	 * Creates the INSERT query to add the results to the given database connection
	 * @param element
	 * @param references
	 * @return the complete INSERT query string
	 */
	protected String getInsertQuery(IApiElement element, IReference[] references) {
		if(references.length > 0) {
			
		}
		return null;
	}
	
	/**
	 * Reports that a component with the given id has not bee searched, with status on why not.
	 * @param components the array of skipped API components
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public void reportNotSearched(CommonUtilsTask.SkippedComponent[] components) {
		executeStatement(getNotSearchedInsertQuery(components));
	}
	
	/**
	 * Returns the query for updating the not_searched table 
	 * @param components
	 * @return the query to execute or <code>null</code>
	 */
	protected String getNotSearchedInsertQuery(CommonUtilsTask.SkippedComponent[] components) {
		if(components != null && components.length > 0) {
			
		}
		return null;
	}
}
