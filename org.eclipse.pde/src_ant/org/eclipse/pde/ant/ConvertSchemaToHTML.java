/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ant;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

import javax.xml.parsers.*;

import org.apache.tools.ant.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.builders.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.core.schema.*;
import org.osgi.framework.*;

public class ConvertSchemaToHTML extends Task {

	private SAXParser fParser;
	private SchemaTransformer fTransformer = new SchemaTransformer();
	private String manifest;
	private String destination;
	private URL cssURL;
	
	/*
     * The standalone descriptor is responsible for creating the schema
     * and computing the location of the included schemas that span
     * the plug-ins by computing the referenced plug-in location.
     * The assumption is that all the plug-ins in the build
     * are extracted into one folder, hence they are all colocated.
	 */
	private static class StandaloneSchemaDescriptor implements ISchemaDescriptor {
		private IPluginExtensionPoint point;
		private URL url;
		private ISchema schema;
		
		public StandaloneSchemaDescriptor(IPluginExtensionPoint point, URL url) {
			this.point = point;
			this.url = url;
			schema = new Schema(this, url);
		}
		public IPath getPluginLocation(String id) {
			IPluginModelBase model = (IPluginModelBase)point.getModel();
			String location = model.getInstallLocation();
			IPath path = new Path(location);
			// Go one level up from the model location,
			// and append the plug-in ID
			// During the build, all the plug-ins are 
			// colocated.
			return path.removeLastSegments(1).append(id);			
		}
		public URL getSchemaURL() {
			return url;
			
		}
		public String getPointId() {
			return point.getFullId();
			
		}
		public ISchema getSchema() {
			return schema;
		}
		public boolean isStandalone() {
			return true;
		}
	}

	public ConvertSchemaToHTML(){
		cssURL = null;
	}

	public void execute() throws BuildException {
		if (!validateDestination())
			return;

		IPluginModelBase model = readManifestFile();
		if (model == null)
			return;
		
		String pluginID = model.getPluginBase().getId();
		if (pluginID == null) {
			pluginID = getPluginID();
		}

		IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			String schemaLocation = extPoints[i].getSchema();
			PrintWriter out = null;

			if (schemaLocation == null || schemaLocation.equals("")) //$NON-NLS-1$
				continue;
			Schema schema=null;
			try {
				if (fParser == null) {
					SAXParserFactory factory = SAXParserFactory.newInstance();
					fParser = factory.newSAXParser();
				}
				File schemaFile = new File(model.getInstallLocation(), schemaLocation);
				XMLDefaultHandler handler = new XMLDefaultHandler();
				fParser.setProperty("http://xml.org/sax/properties/lexical-handler", handler); //$NON-NLS-1$
				fParser.parse(schemaFile, handler);

				URL url = null;
				try {
					url = new URL("file:" + schemaFile.getAbsolutePath()); //$NON-NLS-1$
				} catch (MalformedURLException e) {
				}
				StandaloneSchemaDescriptor desc = new StandaloneSchemaDescriptor(extPoints[i], url);
				schema = (Schema)desc.getSchema();
				schema.traverseDocumentTree(
					handler.getDocumentElement(),
					handler.getLineTable());
					
				File directory =
					new Path(destination).isAbsolute()
						? new File(destination)
						: new File(getProject().getBaseDir(), destination);
				if (!directory.exists() || !directory.isDirectory())
					if (!directory.mkdirs()) {
						schema.dispose();
						return;
					}

				File file =
					new File(
						directory,
						(pluginID + "." + extPoints[i].getId()).replace('.', '_') + ".html"); //$NON-NLS-1$ //$NON-NLS-2$
				
				out = new PrintWriter(new FileWriter(file), true);
				fTransformer.transform(out, schema, cssURL, SchemaTransformer.BUILD);
			} catch (Exception e) {
				if (e.getMessage() != null)
					System.out.println(e.getMessage());
			} finally {
				if (out != null)
					out.close();
				if (schema!=null)
					schema.dispose();
			}
		}
	}
	
	private String getPluginID() {
		File file =
			new Path(manifest).isAbsolute()
				? new File(manifest)
				: new File(getProject().getBaseDir(), manifest);
		File OSGiFile = new File(file.getParentFile(), "META-INF/MANIFEST.MF"); //$NON-NLS-1$

		if (OSGiFile.exists()) {
			try {
				Manifest OSGiManifest = new Manifest(new FileInputStream(OSGiFile));
				Dictionary headers = manifestToProperties(OSGiManifest.getMainAttributes());
				String value = headers.get(Constants.BUNDLE_SYMBOLICNAME).toString();
				if (value == null)
					return null;
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, value);
					if (elements.length > 0)
						return elements[0].getValue();
			} catch (Exception e1) {
				System.out.print(e1.getMessage());
			}
		}
		return null;
	}

	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}
	
	public URL getCSSURL(){
		return cssURL;
	}
	
	public void setCSSURL(String url){
		try {
			cssURL = new URL(url);
		} catch (MalformedURLException e) {
			PDE.logException(e);
		}
	}
	
	public void setCSSURL(URL url){
		cssURL = url;
	}

	private IPluginModelBase readManifestFile() {
		if (manifest == null) {
			System.out.println(
				PDE.getFormattedMessage("Builders.Convert.missingAttribute", "manifest")); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		
		File file =
			new Path(manifest).isAbsolute()
				? new File(manifest)
				: new File(getProject().getBaseDir(), manifest);
		InputStream stream = null;
		try {
			stream = new FileInputStream(file);
		} catch (Exception e) {
			if (e.getMessage() != null)
				System.out.println(e.getMessage());
			return null;
		}

		ExternalPluginModelBase model = null;
		try {
			if (file.getName().toLowerCase().equals("fragment.xml")) //$NON-NLS-1$
				model = new ExternalFragmentModel();
			else if (file.getName().toLowerCase().equals("plugin.xml")) //$NON-NLS-1$
				model = new ExternalPluginModel();
			else {
				System.out.println(
					PDE.getFormattedMessage("Builders.Convert.illegalValue", "manifest")); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}

			String parentPath = file.getParentFile().getAbsolutePath();
			model.setInstallLocation(parentPath);
			model.load(stream, false);
			stream.close();
		} catch (Exception e) {
			if (e.getMessage() != null)
				System.out.println(e.getMessage());
		} 
		
		return model;
	}
	
	private Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}


	private boolean validateDestination() {
		boolean valid = true;
		if (destination == null) {
			System.out.println(
				PDE.getFormattedMessage(
					"Builders.Convert.missingAttribute", //$NON-NLS-1$
					"destination")); //$NON-NLS-1$
			valid = false;
		} else if (!new Path(destination).isValidPath(destination)) {
			System.out.println(
				PDE.getFormattedMessage("Builders.Convert.illegalValue", "destination")); //$NON-NLS-1$ //$NON-NLS-2$
			valid = false;
		}
		return valid;
	}
	
}
