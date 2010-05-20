/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchRequestor;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of {@link IMetadata} for API use scans
 * 
 * @since 1.0.1
 */
public class UseMetadata implements IMetadata {

	/**
	 * XML tag name for the date the scan was run
	 */
	public static final String RUNATDATE = "runatdate"; //$NON-NLS-1$
	/**
	 * XML tag name for the search flags
	 */
	public static final String FLAGS = "flags"; //$NON-NLS-1$
	/**
	 * XML tag name for the baseline location
	 */
	public static final String BASELINELOCATION = "baselinelocation"; //$NON-NLS-1$
	/**
	 * XML tag name for the report location
	 */
	public static final String REPORTLOCATION = "reportlocation"; //$NON-NLS-1$
	/**
	 * XML tag name for a scope pattern
	 */
	public static final String SCOPEPATTERN = "scopepattern"; //$NON-NLS-1$
	/**
	 * XML tag name reference pattern
	 */
	public static final String REFERENCEPATTERN = "referencepattern"; //$NON-NLS-1$
	/**
	 * XML tag name for API patterns
	 */
	public static final String APIPATTERNS = "apipatterns"; //$NON-NLS-1$
	/**
	 * XML tag name for internal patterns
	 */
	public static final String INTERNALPATTERNS = "internalpatterns"; //$NON-NLS-1$
	/**
	 * XML tag name for archive patterns
	 */
	public static final String ARCHIVEPATTERNS = "archivepatterns"; //$NON-NLS-1$
	/**
	 * XML tag name for a pattern
	 */
	public static final String PATTERN = "pattern"; //$NON-NLS-1$
	/**
	 * XML tag name for the value
	 */
	public static final String VALUE = "value"; //$NON-NLS-1$
	/**
	 * Root tag for the metadata file
	 */
	public static final String METADATA = "metadata"; //$NON-NLS-1$
	/**
	 * XML tag name for the description field
	 */
	public static final String DESCRIPTION = "description"; //$NON-NLS-1$
	
	int searchflags = 0;
	String[] apipatterns = null, intpatterns = null, archivepatterns = null;
	String baselinelocation = null, 
			reportlocation = null, 
			scopepattern = null, 
			refpattern = null, 
			runatdate = null,
			description = null;
	
	/**
	 * Constructor
	 */
	public UseMetadata() {
		//create an empty metadata object
	}
	
	/**
	 * Constructor
	 * @param searchflags
	 * @param scopepattern
	 * @param refpattern
	 * @param baselinelocation
	 * @param reportlocation
	 * @param apipatterns
	 * @param internalpatterns
	 * @param archivepatterns
	 * @param runatdate
	 * @param description
	 */
	public UseMetadata(int searchflags, String scopepattern, String refpattern, 
			String baselinelocation, String reportlocation, String[] apipatterns, String[] internalpatterns, 
			String[] archivepatterns, String runatdate, String description) {
		this.searchflags = searchflags;
		this.scopepattern = scopepattern;
		this.refpattern = refpattern;
		this.baselinelocation = baselinelocation;
		this.reportlocation = reportlocation;
		this.apipatterns = apipatterns;
		this.intpatterns = internalpatterns;
		this.archivepatterns = archivepatterns;
		this.runatdate = runatdate;
		this.description = description;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IMetadata#serializeToFile(java.io.File)
	 */
	public void serializeToFile(File file) throws IOException, CoreException {
		BufferedWriter writer = null;
		try {
			Document doc = Util.newDocument();
			Element root = doc.createElement(METADATA);
			doc.appendChild(root);
			Element child = doc.createElement(FLAGS);
			root.appendChild(child);
			child.setAttribute(VALUE, Integer.toString(this.searchflags));
			child = doc.createElement(RUNATDATE);
			root.appendChild(child);
			child.setAttribute(VALUE, this.runatdate);
			child = doc.createElement(DESCRIPTION);
			root.appendChild(child);
			child.setAttribute(VALUE, this.description);
			child = doc.createElement(BASELINELOCATION);
			root.appendChild(child);
			child.setAttribute(VALUE, this.baselinelocation);
			child = doc.createElement(REPORTLOCATION);
			root.appendChild(child);
			child.setAttribute(VALUE, this.reportlocation);
			child = doc.createElement(SCOPEPATTERN);
			root.appendChild(child);
			child.setAttribute(VALUE, this.scopepattern);
			child = doc.createElement(REFERENCEPATTERN);
			root.appendChild(child);
			child.setAttribute(VALUE, this.refpattern);
			child = doc.createElement(APIPATTERNS);
			root.appendChild(child);
			Element sub = null;
			if(this.apipatterns != null) {
				for (int i = 0; i < this.apipatterns.length; i++) {
					sub = doc.createElement(PATTERN);
					child.appendChild(sub);
					sub.setAttribute(VALUE, apipatterns[i]);
				}
			}
			child = doc.createElement(INTERNALPATTERNS);
			root.appendChild(child);
			if(this.intpatterns != null) {
				for (int i = 0; i < this.intpatterns.length; i++) {
					sub = doc.createElement(PATTERN);
					child.appendChild(sub);
					sub.setAttribute(VALUE, intpatterns[i]);
				}
			}
			child = doc.createElement(ARCHIVEPATTERNS);
			root.appendChild(child);
			if(this.archivepatterns != null) {
				for (int i = 0; i < this.archivepatterns.length; i++) {
					sub = doc.createElement(PATTERN);
					child.appendChild(sub);
					sub.setAttribute(VALUE, archivepatterns[i]);
				}
			}
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), IApiCoreConstants.UTF_8));
			writer.write(Util.serializeDocument(doc));
			writer.flush();
		}
		finally {
			if(writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * @return true if the search8 flags include searching for API references,
	 * false otherwise
	 */
	public boolean includesAPI() {
		return (this.searchflags & IApiSearchRequestor.INCLUDE_API) != 0;
	}
	
	/**
	 * @return true if the search flags include searching for internal references, 
	 * false otherwise
	 */
	public boolean includesInternal() {
		return (this.searchflags & IApiSearchRequestor.INCLUDE_INTERNAL) != 0;
	}
	
	/**
	 * @return true if the search flags include searching for illegal use, 
	 * false otherwise
	 */
	public boolean includesIllegalUse() {
		return (this.searchflags & IApiSearchRequestor.INCLUDE_ILLEGAL_USE) != 0;
	}
	
	/**
	 * Allows the run-at date to be set. This method accepts <code>null</code>
	 * @param date the date to set
	 */
	public void setRunAtDate(String date) {
		this.runatdate = date;
	}
	
	/**
	 * Returns the run-at date set in this metadata or <code>null</code> if none.
	 * @return the run-at date or <code>null</code>
	 */
	public String getRunAtDate() {
		return this.runatdate;
	}
	
	/**
	 * Returns the human-readable description of the scan
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Allows the human-readable description to be set. This method accepts <code>null</code>
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Allows the combined search flags to be set.
	 * @param flags the search flags to set
	 */
	public void setSearchflags(int flags) {
		this.searchflags = flags;
	}

	/**
	 * Returns the collection of API patterns set in this metadata or <code>null</code> if none.
	 * @return the API patterns or <code>null</code>
	 */
	public String[] getApiPatterns() {
		return apipatterns;
	}
	
	/**
	 * Allows the API patterns to be set. This method accepts <code>null</code>
	 * @param patterns the patterns to set
	 */
	public void setApiPatterns(String[] patterns) {
		this.apipatterns = patterns;
	}

	/**
	 * Returns the collection of internal patterns set in this metadata or <code>null</code> if none.
	 * @return the internal patterns or <code>null</code>
	 */
	public String[] getInternalPatterns() {
		return this.intpatterns;
	}

	/**
	 * Allows the internal patterns to be set. This method accepts <code>null</code>.
	 * @param patterns the internal patterns to set
	 */
	public void setInternalPatterns(String[] patterns) {
		this.intpatterns = patterns;
	}

	/**
	 * Returns the collection of archive patterns set in this metadata or <code>null</code> if none.
	 * @return the archive patterns or <code>null</code>
	 */
	public String[] getArchivePatterns() {
		return this.archivepatterns;
	}

	/**
	 * Allows the set of archive patterns to be set. This method accepts <code>null</code>
	 * @param patterns the archive patterns to set
	 */
	public void setArchivePatterns(String[] patterns) {
		this.archivepatterns = patterns;
	}

	/**
	 * Returns the baseline location set in this metadata or <code>null</code> if none.
	 * @return the baseline location or <code>null</code>
	 */
	public String getBaselineLocation() {
		return this.baselinelocation;
	}

	/**
	 * Allows the baseline location to be set. This method accepts <code>null</code>.
	 * @param location the new location
	 */
	public void setBaselineLocation(String location) {
		this.baselinelocation = location;
	}

	/**
	 * Returns the report location set in this metadata or <code>null</code> if none.
	 * @return the report location or <code>null</code>
	 */
	public String getReportLocation() {
		return this.reportlocation;
	}

	/**
	 * Allows the report location to be set. This method accepts <code>null</code>.
	 * @param location the new report location
	 */
	public void setReportLocation(String location) {
		this.reportlocation = location;
	}

	/**
	 * Allows the reference pattern to be set. This method accepts <code>null</code>
	 * @param pattern the new pattern
	 */
	public void setReferencePattern(String pattern) {
		this.refpattern = pattern;
	}
	
	/**
	 * Returns the reference pattern set in this metadata or <code>null</code> if none.
	 * @return the reference pattern or <code>null</code>
	 */
	public String getReferencePattern() {
		return this.refpattern;
	}
	
	/**
	 * Allows the scope pattern to be set. This method accepts <code>null</code>
	 * @param pattern the new pattern
	 */
	public void setScopePattern(String pattern) {
		this.scopepattern = pattern;
	}
	
	/**
	 * Returns the scope pattern set in this metadata or <code>null</code> if none.
	 * @return the scope pattern or <code>null</code>
	 */
	public String getScopePattern() {
		return this.scopepattern;
	}
}
