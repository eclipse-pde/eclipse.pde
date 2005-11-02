/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;

public class IncludedSchemaDescriptor implements ISchemaDescriptor {
	private URL fSchemaURL;
	private String fSchemaLocation;
	private Schema fSchema;
	private long fLastModified;

	public IncludedSchemaDescriptor(URL schemaURL) {
		fSchemaURL = schemaURL;
		File file = new File(fSchemaURL.getFile());
		if (file.exists())
			fLastModified = file.lastModified();
	}
	
	public static URL computeURL(ISchemaDescriptor parentDesc, String schemaLocation) throws MalformedURLException {
		if (schemaLocation.startsWith("schema://")) { //$NON-NLS-1$
			// extract plug-in ID
			IPath path = new Path( schemaLocation.substring(9));
			return getPluginRelativePath(path.segment(0), path.removeFirstSegments(1), parentDesc.getSchemaURL());
		}
		// parent-relative location
		URL parentURL = parentDesc.getSchemaURL();
		IPath path = new Path(parentURL.getPath());
		path = path.removeLastSegments(1).append(schemaLocation);
		return new URL(parentURL.getProtocol(), parentURL.getHost(), path.toString());	
	}
	
	private static URL getPluginRelativePath(String pluginID, IPath path, URL parentURL) {		
		URL url = SchemaRegistry.getSchemaURL(pluginID, path.toString());
		if (url == null) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginID);
			if (model != null)
				url = SchemaRegistry.getSchemaFromSourceExtension(model.getPluginBase(), path);		
		}
		try {
			if (url == null && parentURL != null){
				File file = new File(parentURL.getFile(), "../../../" + pluginID + "/" + path.toString()); //$NON-NLS-1$ //$NON-NLS-2$
				if (file.exists() && file.isFile())
					url = file.toURL();
			}
		} catch (MalformedURLException e) {
		}
		return url;
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
		int dotLoc = fSchemaLocation.lastIndexOf('.');
		if (dotLoc!= -1) {
			return fSchemaLocation.substring(0, dotLoc);
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchemaURL()
	 */
	public URL getSchemaURL() {
		return fSchemaURL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getSchema(boolean)
	 */
	public ISchema getSchema(boolean abbreviated) {
		if (fSchema == null && fSchemaURL != null) {
			fSchema = new Schema(this, fSchemaURL, abbreviated);
			fSchema.load();
		}
		return fSchema;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#isStandalone()
	 */
	public boolean isStandalone() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getLastModified()
	 */
	public long getLastModified() {
		return fLastModified;
	}

}
