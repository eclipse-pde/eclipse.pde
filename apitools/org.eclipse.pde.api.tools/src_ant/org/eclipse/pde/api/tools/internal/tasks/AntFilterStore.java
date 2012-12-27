/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.eclipse.pde.api.tools.internal.FilterStore;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;

/**
 * This filter store is only used to filter problem using existing filters.
 * It doesn't add or remove any filters.
 */
public class AntFilterStore extends FilterStore {

	private boolean debug;
	String fComponentId = null;
	String fFiltersRoot = null;
	
	/**
	 * Constructor
	 * @param debug
	 * @param filtersRoot
	 * @param componentID
	 */
	public AntFilterStore(boolean debug, String filtersRoot, String componentID) {
		fComponentId = componentID;
		fFiltersRoot = filtersRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.FilterStore#initializeApiFilters()
	 */
	protected synchronized void initializeApiFilters() {
		if(fFilterMap != null) {
			return;
		}
		if(this.debug) {
			System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
		}
		fFilterMap = new HashMap(5);
		InputStream contents = null;
		try {
			File filterFileParent = new File(fFiltersRoot, fComponentId);
			if (!filterFileParent.exists()) {
				return;
			}
			contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
			readFilterFile(contents);
		}
		catch(IOException ioe) {
		}
		finally {
			if (contents != null) {
				try {
					contents.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}
	
}