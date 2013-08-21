/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.File;
import java.net.*;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;

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

	public static URL computeURL(ISchemaDescriptor parentDesc, String schemaLocation, List<IPath> additionalSearchLocations) throws MalformedURLException {
		URL parentURL = parentDesc == null ? null : parentDesc.getSchemaURL();
		if (schemaLocation.startsWith("schema://")) { //$NON-NLS-1$
			// extract plug-in ID
			IPath path = new Path(schemaLocation.substring(9));
			return getPluginRelativePath(path.segment(0), path.removeFirstSegments(1), parentURL, additionalSearchLocations);
		}

		if (parentURL == null)
			return null;

		// parent-relative location
		IPath path = new Path(parentURL.getPath());
		path = path.removeLastSegments(1).append(schemaLocation);
		return new URL(parentURL.getProtocol(), parentURL.getHost(), path.toString());
	}

	/**
	 * Creates a URL describing the location of a included schema.  First looks up
	 * the plug-in in the schema registry, then tries additional source locations,
	 * then looks for a co-located plug-in, then looks in the additional search
	 * path locations.
	 * 
	 * @param pluginID ID of the plug-in owning the schema
	 * @param path the path to the schema inside the plug-in
	 * @param parentURL url of the parent schema file
	 * @param additionalSearchPath list of additional locations to search; only used with the <code>pde.convertSchemaToHTML</code> Ant task
	 * @return a url location of the included schema or <code>null</code>
	 */
	private static URL getPluginRelativePath(String pluginID, IPath path, URL parentURL, List<IPath> additionalSearchPath) {
		// Start by looking in the schema registry
		URL url = SchemaRegistry.getSchemaURL(pluginID, path.toString());

		// Next search source locations
		if (url == null) {
			IPluginModelBase model = PluginRegistry.findModel(pluginID);
			if (model != null)
				url = SchemaRegistry.getSchemaFromSourceExtension(model.getPluginBase(), path);
		}

		File parentFile = null;
		if (url == null && parentURL != null) {
			try {
				parentFile = URIUtil.toFile(URIUtil.toURI(parentURL));
			} catch (URISyntaxException e) {
			}
		}

		// If we are running the ant task, see if another project co-located with the parent contains the schema file
		// The project folder must have the plug-in ID as its file name
		if (url == null && parentFile != null) {
			try {
				// assuming schemas are located in: pluginId/schema/schemaFile.exsd (need to go up to parent of plug-in directory)
				File pluginFile = new File(parentFile + "/../../../" + pluginID); //$NON-NLS-1$
				if (pluginFile.isDirectory()) {
					File schemaFile = new File(pluginFile, path.toOSString());
					if (schemaFile.exists()) {
						url = schemaFile.toURL();
					}
					// This is how we would extract the schema from a jar, but in practice this will never be the case
					// because when a bundle is built, the schema files are moved to the source bundle, not the bundle we are checking
//					} else if (CoreUtility.jarContainsResource(pluginFile, path.toPortableString(), false)) {
//						url = new URL("jar:file:" + pluginFile.getAbsolutePath() + "!/" + path); //$NON-NLS-1$ //$NON-NLS-2$
				}
			} catch (MalformedURLException e) {
			}
		}

		// If we are running the ant task, additional search locations may be provided
		// The project folder must have the plug-in ID as its file name
		if (url == null && additionalSearchPath != null) {
			for (Iterator<IPath> iterator = additionalSearchPath.iterator(); iterator.hasNext();) {
				IPath currentSearch = iterator.next();
				File pluginFile = null;
				if (currentSearch.isAbsolute()) {
					// Append plug-in id directly to absolute paths
					pluginFile = new File(currentSearch.toFile(), pluginID);
				} else if (parentFile != null) {
					// Append relative path to parent file location
					File file = new File(parentFile, currentSearch.toOSString());
					pluginFile = new File(file, pluginID);
				}

				if (pluginFile != null && pluginFile.isDirectory()) {
					try {
						File schemaFile = new File(pluginFile, path.toOSString());
						if (schemaFile.exists()) {
							url = schemaFile.toURL();
							break;
						}
					} catch (MalformedURLException e) {
					}
				}
			}
		}
		return url;
	}

	public boolean isEnabled() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.ischema.ISchemaDescriptor#getPointId()
	 */
	public String getPointId() {
		int dotLoc = fSchemaLocation.lastIndexOf('.');
		if (dotLoc != -1) {
			return fSchemaLocation.substring(0, dotLoc);
		}
		return null;
	}

	/* (non-Javadoc)
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
