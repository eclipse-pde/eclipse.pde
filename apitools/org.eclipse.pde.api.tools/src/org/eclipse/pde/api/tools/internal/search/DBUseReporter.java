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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;

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
		executeStatement(getBundleTableInsertQuery(element.getApiComponent()));
	}

	/**
	 * Executes a collection of queries in one transaction
	 * @param queries
	 */
	protected void executeBatchStatement(String[] queries) {
		if(queries != null && queries.length > 0) {
			try {
				if(this.connection.isClosed()) {
					return;
				}
				Statement statement = this.connection.createStatement();
				for (int i = 0; i < queries.length; i++) {
					statement.addBatch(queries[i]);
				}
				int[] values = statement.executeBatch();
				if(this.debug) {
					for (int i = 0; i < values.length; i++) {
						if(values[i] < 1) {
							System.out.println("The executed statement: "+queries[i]+"did not cause any updates"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					SQLWarning warning = statement.getWarnings();
					if(warning != null) {
						System.out.println("The executed statement: "+statement.toString()+" had the following warning(s): "+warning); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
			catch (SQLException sqle) {
				ApiPlugin.log(sqle);
			}
		}
	}
	
	/**
	 * Executes a given query
	 * @param query
	 */
	protected void executeStatement(String query) {
		if(query == null) {
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
		catch(SQLException sqe) {
			ApiPlugin.log(sqe);
		}
	}
	
	/**
	 * Returns the INSERT query to add new entries to the BUILDS table
	 * @param version
	 * @return the INSERT query for the BUILDS table
	 */
	protected String getBuildVersionInsertQuery(String version) {
		return "INSERT INTO BUILDS (VERSION_ID) VALUES ('"+version+"')"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * Returns the query to use to insert into the BUNDLES table
	 * @param element
	 * @return the query to run or <code>null</code> if the given element is <code>null</code> or an exception occurs getting
	 * the {@link IApiComponent} information from the given {@link IApiElement}
	 * @throws CoreException
	 */
	protected String getBundleTableInsertQuery(IApiElement element) {
		if(element == null) {
			return null;
		}
		IApiComponent component = element.getApiComponent();
		if(component == null) {
			return null;
		}
		try {
			return "INSERT INTO BUNDLES (BUNDLE_ID, VERSION) VALUES('"+component.getId()+"', '"+component.getVersion()+"')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		return null;
	}
	
	/**
	 * Reports that a component with the given id has not bee searched, with status on why not.
	 * @param components the array of skipped API components
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 */
	public void reportNotSearched(SkippedComponent[] components) {
		executeStatement(getNotSearchedInsertQuery(components));
	}
	
	/**
	 * Returns the query for updating the not_searched table 
	 * @param components
	 * @return the query to execute or <code>null</code>
	 */
	protected String getNotSearchedInsertQuery(SkippedComponent[] components) {
		if(components != null && components.length > 0) {
			
		}
		return null;
	}
}
