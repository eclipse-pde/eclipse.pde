/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import java.util.Calendar;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.ibm.icu.text.DateFormat;

/**
 * Implementation of {@link IMetadata} for API Use Scan problem report
 */
public class MissingRefMetadata implements IMetadata {

	/**
	 * XML tag name for Profile
	 */
	public static final String PROFILE = "profile"; //$NON-NLS-1$
	/**
	 * XML tag name for the date the scan was run
	 */
	public static final String RUNATDATE = "runatdate"; //$NON-NLS-1$
	/**
	 * XML tag name for the report location
	 */
	public static final String REPORTLOCATION = "reportlocation"; //$NON-NLS-1$
	/**
	 * XML tag name for api use scan list
	 */
	public static final String APIUUSESCANS = "apiusescans"; //$NON-NLS-1$
	/**
	 * XML tag name for the value
	 */
	public static final String VALUE = "value"; //$NON-NLS-1$
	/**
	 * Root tag for the metadata file
	 */
	public static final String METADATA = "metadata"; //$NON-NLS-1$

	private String profile = null;
	private String runatdate = null;
	private String reportlocation = null;
	private String apiusescans = null;

	/**
	 * Constructor
	 */
	public MissingRefMetadata() {
		//create an empty metadata object
	}

	/**
	 * Constructor
	 * @param profile
	 * @param runatdate
	 * @param reportlocation
	 * @param scopepattern
	 * @param refpattern
	 * @param apiusescans
	 */
	public MissingRefMetadata(String profile, String reportlocation, String apiusescans) {
		this.profile = profile;
		this.runatdate = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
		this.reportlocation = reportlocation;
		this.apiusescans = apiusescans;
	}

	/**
	 * Returns the use metadata from meta.xml
	 * @return
	 * @throws Exception
	 */
	public static MissingRefMetadata getMetadata(File xmlFile) throws Exception {
		MissingRefMetadata metadata = new MissingRefMetadata();
		try {
			if (xmlFile.exists()) {
				String xmlstr = Util.getFileContentAsString(xmlFile);
				Element doc = Util.parseDocument(xmlstr.trim());
				Element element = null;
				String value = null, name = null;
				NodeList nodes = doc.getElementsByTagName("*"); //$NON-NLS-1$
				for (int i = 0; i < nodes.getLength(); i++) {
					element = (Element) nodes.item(i);
					value = element.getAttribute(MissingRefMetadata.VALUE);
					name = element.getNodeName();

					if (PROFILE.equals(name)) {
						metadata.setProfile(value);
						continue;
					}
					if (RUNATDATE.equals(name)) {
						metadata.setRunAtDate(value);
						continue;
					}
					if (REPORTLOCATION.equals(name)) {
						metadata.setReportLocation(value);
						continue;
					}
					if (APIUUSESCANS.equals(name)) {
						metadata.setApiUseScans(value);
						continue;
					}
				}
			}
		} catch (CoreException e) {
			throw new Exception(NLS.bind(SearchMessages.MissingRefMetadata_CoreExceptionInParsing, xmlFile.getAbsolutePath()));
		}
		return metadata;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IMetadata#serializeToFile(java.io.File)
	 */
	public void serializeToFile(File file) throws IOException, CoreException {
		BufferedWriter writer = null;
		try {
			Document doc = Util.newDocument();
			Element root = doc.createElement(MissingRefMetadata.METADATA);
			doc.appendChild(root);

			Element child = doc.createElement(MissingRefMetadata.PROFILE);
			root.appendChild(child);
			child.setAttribute(MissingRefMetadata.VALUE, profile);

			child = doc.createElement(MissingRefMetadata.RUNATDATE);
			root.appendChild(child);
			child.setAttribute(MissingRefMetadata.VALUE, runatdate);

			child = doc.createElement(MissingRefMetadata.REPORTLOCATION);
			root.appendChild(child);
			child.setAttribute(MissingRefMetadata.VALUE, reportlocation);

			child = doc.createElement(MissingRefMetadata.APIUUSESCANS);
			root.appendChild(child);
			child.setAttribute(MissingRefMetadata.VALUE, apiusescans);

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), IApiCoreConstants.UTF_8));
			writer.write(Util.serializeDocument(doc));
			writer.flush();
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Allows the profile to be set. This method accepts <code>null</code>
	 * @param profile the profile to set
	 */
	public void setProfile(String profile) {
		this.profile = profile;
	}

	/**
	 * Returns the profile set in this metadata or <code>null</code> if none.
	 * @return the profile
	 */
	public String getProfile() {
		return profile;
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
	 * Returns the API Use Scans set in this metadata or <code>null</code> if none.
	 * @return the apiusescans
	 */
	public String getApiUseScans() {
		return apiusescans;
	}

	/**
	 * Allows the API Use Scan to be set. This method accepts <code>null</code>
	 * @param apiusescans the apiusescans to set
	 */
	public void setApiUseScans(String apiusescans) {
		this.apiusescans = apiusescans;
	}
}
