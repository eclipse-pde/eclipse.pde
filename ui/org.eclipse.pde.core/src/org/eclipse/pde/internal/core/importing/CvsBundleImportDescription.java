/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.importing;

import java.util.Map;
import org.eclipse.pde.core.importing.BundleImportDescription;

/**
 * Describes a bundle to be imported by CVS.
 * @since 3.6
 */
public class CvsBundleImportDescription extends BundleImportDescription {

	String tag;
	String server;
	String path;
	String module;
	String protocol;

	/**
	 * @param project
	 * @param manifest
	 */
	public CvsBundleImportDescription(String project, Map manifest, String protocol, String server, String path, String module, String tag) {
		super(project, manifest);
		this.protocol = protocol;
		this.server = server;
		this.path = path;
		this.module = module;
		this.tag = tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag() {
		return tag;
	}

	public String getServer() {
		return server;
	}

	public String getPath() {
		return path;
	}

	public String getModule() {
		return module;
	}

	public String getProtocol() {
		return protocol;
	}

}
