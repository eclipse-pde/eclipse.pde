/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.net.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.ischema.*;

public class IncludedSchemaDescriptor extends AbstractSchemaDescriptor {
	private URL url;
	private String schemaLocation;
	private ISchemaDescriptor parent;

	public IncludedSchemaDescriptor(ISchemaDescriptor parent, String schemaLocation) {
		this.parent = parent;
		this.schemaLocation = schemaLocation;

		try {
			url = computeURL(parent, parent.getSchemaURL(), schemaLocation);
		}
		catch (MalformedURLException e) {
		}
	}
	
	public IFile getFile() {
		if (parent instanceof FileSchemaDescriptor) {
			FileSchemaDescriptor fparent = (FileSchemaDescriptor)parent;
			IFile parentFile = fparent.getFile();
			if (parentFile==null) return null;
			IPath parentPath = parentFile.getProjectRelativePath();
			IPath childPath = parentPath.removeLastSegments(1).append(schemaLocation);
			return parentFile.getProject().getFile(childPath);
		}
		return null;
	}
	
	public static URL computeURL(IPluginLocationProvider locationProvider, URL parentURL, String schemaLocation) throws MalformedURLException {
		if (schemaLocation.startsWith("schema://")) { //$NON-NLS-1$
			// plugin-relative location
			String rem = schemaLocation.substring(9);
			// extract plug-in ID
			IPath path = new Path(rem);
			String pluginId = path.segment(0);
			path = path.removeFirstSegments(1);
			// the resulting path is relative to the plug-in.
			// Use location provider to find the referenced plug-in
			// location.
			if (locationProvider!=null) {
				IPath includedLocation = locationProvider.getPluginRelativePath(pluginId, path);
				if (includedLocation==null) return null;
				return new URL(parentURL.getProtocol(), parentURL.getHost(), includedLocation.toString());
			}
			return null;
		}
		// parent-relative location
		IPath path = new Path(parentURL.getPath());
		path = path.removeLastSegments(1).append(schemaLocation);
		return new URL(parentURL.getProtocol(), parentURL.getHost(), path.toString());	
	}

	/**
	 * @see org.eclipse.pde.internal.core.schema.AbstractSchemaDescriptor#isEnabled()
	 */
	public boolean isEnabled() {
		return true;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getPointId()
	 */
	public String getPointId() {
		int dotLoc = schemaLocation.lastIndexOf('.');
		if (dotLoc!= -1) {
			return schemaLocation.substring(0, dotLoc);
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchemaURL()
	 */
	public URL getSchemaURL() {
		return url;
	}
}
