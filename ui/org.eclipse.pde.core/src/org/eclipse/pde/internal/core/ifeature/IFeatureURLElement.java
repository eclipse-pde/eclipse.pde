/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ifeature;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;

/**
 * A URL element is a model object that represents
 * a single URL reference. The reference has a type
 * (UPDATE, DISCOVERY or INFO), and an optional
 * label that should be used to represent the URL
 * in the UI.
 */
public interface IFeatureURLElement extends IFeatureObject {
	/**
	 * Indicates that this is an update URL.
	 */
	public static final int UPDATE = 1;
	/**
	 * Indicates that this is a discovery URL.
	 */
	public static final int DISCOVERY = 2;
	/**
	 * Indicates that the site is a regular update site.
	 */
	public static final int UPDATE_SITE = 0;
	/**
	 * Indicates that the site is a Web site. 
	 */
	public static final int WEB_SITE = 1;
	/**
	 * This property name will be used to notify
	 * about changes in the "URL" field.
	 */
	public static final String P_URL = "url"; //$NON-NLS-1$
	/**
	 * This property name will be used to notify
	 * about changes in the 'siteType' field. 
	 */
	public static final String P_SITE_TYPE = "siteType"; //$NON-NLS-1$

	/**
	 * Returns the type of this URL element (UPDATE or DISCOVERY)
	 */
	public int getElementType();

	/**
	 * Returns the type of the site with the specified URL.
	 * @return UPDATE_SITE or WEB_SITE
	 */
	public int getSiteType();

	/**
	 * Returns the URL of this element.
	 *
	 * @return the URL
	 */
	public URL getURL();

	/**
	 * Sets the URL of this element.
	 * This method will throw a CoreException
	 * if the model is not editable.
	 *
	 * @param url the new URL 
	 */
	public void setURL(URL url) throws CoreException;

	/**
	 * Sets the type of the site with the specified URL.
	 * @param UPDATE_SITE or WEB_SITE
	 */
	public void setSiteType(int siteType) throws CoreException;
}
