/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.osgi.framework.Constants;

public class ConvertSchemaToHTML extends Task {

	private SAXParser fParser;
	private SchemaTransformer fTransformer = new SchemaTransformer();
	private String manifest;
	private String destination;
	private URL cssURL;
	
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
				fParser.parse(schemaFile, handler);

				URL url =  schemaFile.toURL(); 
				SchemaDescriptor desc = new SchemaDescriptor(extPoints[i].getFullId(), url);
				schema = (Schema)desc.getSchema(false);
					
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
				fTransformer.transform(schema, out, cssURL, SchemaTransformer.BUILD);
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
			PDECore.logException(e);
		}
	}
	
	public void setCSSURL(URL url){
		cssURL = url;
	}

	private IPluginModelBase readManifestFile() {
		if (manifest == null) {
			System.out.println(
				NLS.bind(PDECoreMessages.Builders_Convert_missingAttribute, "manifest")); //$NON-NLS-1$ 
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
			if (file.getName().toLowerCase(Locale.ENGLISH).equals("fragment.xml")) //$NON-NLS-1$
				model = new ExternalFragmentModel();
			else if (file.getName().toLowerCase(Locale.ENGLISH).equals("plugin.xml")) //$NON-NLS-1$
				model = new ExternalPluginModel();
			else {
				System.out.println(
						NLS.bind(PDECoreMessages.Builders_Convert_illegalValue, "manifest")); //$NON-NLS-1$ 
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
					NLS.bind(PDECoreMessages.Builders_Convert_missingAttribute,
					"destination")); //$NON-NLS-1$
			valid = false;
		} else if (!new Path(destination).isValidPath(destination)) {
			System.out.println(
					NLS.bind(PDECoreMessages.Builders_Convert_illegalValue, "destination")); //$NON-NLS-1$ 
			valid = false;
		}
		return valid;
	}
	
}
