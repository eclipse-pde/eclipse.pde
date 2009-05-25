/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.site.compatibility;

public class URLEntry {
	private String annotation;
	private String url;

	public URLEntry() {
		this.annotation = ""; //$NON-NLS-1$
		this.url = ""; //$NON-NLS-1$
	}

	public URLEntry(String url, String annotation) {
		this.url = url;
		this.annotation = annotation;
	}

	public void setAnnotation(String annotation) {
		this.annotation = annotation;
	}

	public String getAnnotation() {
		return annotation;
	}

	public void setURL(String url) {
		this.url = url;
	}

	public String getURL() {
		return url;
	}
}
