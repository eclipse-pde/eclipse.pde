/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.schema.PathSchemaProvider;
import org.eclipse.pde.internal.core.schema.SchemaToHTMLConverter;

/**
 * Ant task that takes a plug-in and created HTML reference documents for all schema (.exsd) files.
 *
 */
public class ConvertSchemaToHTML extends Task {

	private String manifest;
	private String destination;
	private URL cssURL;
	private String additionalSearchPaths;

	@Override
	public void execute() throws BuildException {
		if (destination == null) {
			throw new BuildException(NLS.bind(PDECoreMessages.Builders_Convert_missingAttribute, "destination")); //$NON-NLS-1$
		}
		if (manifest == null) {
			throw new BuildException(NLS.bind(PDECoreMessages.Builders_Convert_missingAttribute, "manifest")); //$NON-NLS-1$
		}
		if (!IPath.fromOSString(destination).isValidPath(destination)) {
			throw new BuildException(NLS.bind(PDECoreMessages.Builders_Convert_illegalValue, "destination")); //$NON-NLS-1$
		}
		SchemaToHTMLConverter converter = new SchemaToHTMLConverter(getProject().getBaseDir(), cssURL);
		try {
			converter.generate(manifest, destination, new PathSchemaProvider(getSearchPaths()),
					err -> log(err, Project.MSG_ERR));
		} catch (Exception e) {
			throw new BuildException(e);
		}
	}

	/**
	 * Required attribute describing the location of the plugin.xml file
	 * for the plug-in to create schema html docs for.  May be an absolute
	 * file path or a path relative to the ant base directory <code>${basedir}</code>
	 *
	 * @param manifest string file path to plugin.xml for the plug-in to convert
	 */
	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	/**
	 * Required attribute describing the location to output the HTML.
	 *
	 * @param destination string file path to output html to
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}

	/**
	 * Optional attribute providing a comma <code>','</code> delimited
	 * list of file paths to search for plug-ins that provide schema
	 * files included by the schema files being converted.
	 * <p>
	 * When a schema file includes another, the html will include the
	 * element definitions from the included schema if it is available.
	 * If the schema does not exist in the same plug-in, the task will
	 * assume the schema url is of the form
	 * <code>schema://<pluginID>/<schemaPath>. It will extract the plug-in
	 * ID and look for a folder of that name in the same directory as the
	 * parent schema's host plug-in. If the plug-ins are not all in the same
	 * directory, this attribute can be used to locate them.
	 * </p><p>
	 * The paths can be absolute file paths or paths relative to the ant
	 * base directory <code>${basedir}</code>.
	 * </p>
	 *
	 * @param additionalSearchPaths comma delimited list of search paths
	 */
	public void setAdditionalSearchPaths(String additionalSearchPaths) {
		this.additionalSearchPaths = additionalSearchPaths;
	}

	public URL getCSSURL() {
		return cssURL;
	}

	/**
	 * Sets a url location to lookup a CSS file to use during
	 * the schema transformation.  If not set, the task will search
	 * for a default CSS in the product plug-in.
	 *
	 * @param url string form of url pointing to a CSS file
	 */
	public void setCSSURL(String url) {
		try {
			cssURL = new URL(url);
		} catch (MalformedURLException e) {
			PDECore.logException(e);
		}
	}

	public void setCSSURL(URL url) {
		cssURL = url;
	}

	/**
	 * @return user specified search paths or <code>null</code>
	 */
	private List<IPath> getSearchPaths() {
		if (this.additionalSearchPaths == null) {
			return null;
		}
		String[] paths = this.additionalSearchPaths.split(","); //$NON-NLS-1$
		List<IPath> result = new ArrayList<>(paths.length);
		for (String pathString : paths) {
			IPath path = IPath.fromOSString(pathString);
			if (path.isValidPath(pathString)) {
				if (!path.isAbsolute()) {
					File baseDir = getProject().getBaseDir();
					path = IPath.fromOSString(baseDir.getPath()).append(path);
				}
				result.add(path);
			} else {
				System.out.println(NLS.bind(PDECoreMessages.ConvertSchemaToHTML_InvalidAdditionalSearchPath, pathString));
			}
		}
		return result;
	}

}
