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

import org.apache.tools.ant.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.SourceDOMParser;
import org.eclipse.pde.internal.core.ischema.ISchemaDescriptor;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.schema.Schema;
import org.xml.sax.*;

public class ConvertSchemaToHTML extends Task {

	private SourceDOMParser parser = new SourceDOMParser();
	private SchemaTransformer transformer = new SchemaTransformer();
	private String manifest;
	private String destination;

	public void execute() throws BuildException {
		if (!validateDestination())
			return;

		IPluginModelBase model = readManifestFile();
		if (model == null)
			return;

		IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < extPoints.length; i++) {
			String schemaLocation = extPoints[i].getSchema();
			FileInputStream is = null;
			PrintWriter out = null;

			if (schemaLocation == null || schemaLocation.equals(""))
				continue;

			try {
				File schemaFile =
					new File(
						model.getInstallLocation() + Path.SEPARATOR + schemaLocation);
				is = new FileInputStream(schemaFile);
				parser.parse(new InputSource(is));

				URL url = null;
				try {
					url = new URL("file:" + schemaFile.getAbsolutePath());
				} catch (MalformedURLException e) {
				}
				Schema schema = new Schema((ISchemaDescriptor) null, url);
				schema.traverseDocumentTree(
					parser.getDocument().getDocumentElement(),
					parser.getLineTable());
					
				File directory =
					new Path(destination).isAbsolute()
						? new File(destination)
						: new File(getProject().getBaseDir(), destination);
				if (!directory.exists() || !directory.isDirectory())
					if (!directory.mkdirs())
						return;

				File file =
					new File(
						directory,
						extPoints[i].getFullId().replace('.', '_') + ".html");
				out = new PrintWriter(new FileWriter(file), true);
				transformer.transform(out, schema);
			} catch (Exception e) {
				if (e.getMessage() != null)
					System.out.println(e.getMessage());
			} finally {
				try {
					if (out != null)
						out.close();
					if (is != null)
						is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public void setManifest(String manifest) {
		this.manifest = manifest;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}
	

	private IPluginModelBase readManifestFile() {
		if (manifest == null) {
			System.out.println(
				PDE.getFormattedMessage("Builders.Convert.missingAttribute", "manifest"));
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
			if (file.getName().toLowerCase().equals("fragment.xml"))
				model = new ExternalFragmentModel();
			else if (file.getName().toLowerCase().equals("plugin.xml"))
				model = new ExternalPluginModel();
			else {
				System.out.println(
					PDE.getFormattedMessage("Builders.Convert.illegalValue", "manifest"));
				return null;
			}

			String parentPath = file.getParentFile().getAbsolutePath();
			model.setInstallLocation(parentPath);
			model.load(stream, false);
			stream.close();
		} catch (Exception e) {
			if (e.getMessage() != null)
				System.out.println(e.getMessage());
		} finally {
			return model;
		}
	}

	private boolean validateDestination() {
		boolean valid = true;
		if (destination == null) {
			System.out.println(
				PDE.getFormattedMessage(
					"Builders.Convert.missingAttribute",
					"destination"));
			valid = false;
		} else if (!new Path(destination).isValidPath(destination)) {
			System.out.println(
				PDE.getFormattedMessage("Builders.Convert.illegalValue", "destination"));
			valid = false;
		}
		return valid;
	}
	
}
