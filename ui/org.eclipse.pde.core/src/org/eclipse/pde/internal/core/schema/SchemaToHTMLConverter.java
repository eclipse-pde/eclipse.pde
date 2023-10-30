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
 *     Christoph Läubrich - extract into reusable class
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.XMLDefaultHandler;
import org.eclipse.pde.internal.core.builders.SchemaTransformer;
import org.eclipse.pde.internal.core.ischema.ISchemaInclude;
import org.eclipse.pde.internal.core.plugin.ExternalFragmentModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.core.util.HeaderMap;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.xml.sax.SAXException;

/**
 * Takes a plug-in and created HTML reference documents for all schema (.exsd)
 * files.
 *
 */
public class SchemaToHTMLConverter {

	private final SchemaTransformer fTransformer = new SchemaTransformer();
	private File baseDir;
	private URL cssURL;

	public SchemaToHTMLConverter(File baseDir, URL cssURL) {
		this.baseDir = baseDir;
		this.cssURL = cssURL;
	}

	@SuppressWarnings("restriction")
	public void generate(String manifest, String destination, SchemaProvider schemaProvider,
			Consumer<String> errorConsumer) throws IOException, CoreException {
		Objects.requireNonNull(manifest, "manifest can't be null"); //$NON-NLS-1$
		Objects.requireNonNull(destination, "destination can't be null"); //$NON-NLS-1$
		IPluginModelBase model = readManifestFile(manifest);
		if (model == null) {
			return;
		}

		String pluginID = model.getPluginBase().getId();
		if (pluginID == null) {
			pluginID = getPluginID(manifest);
		}

		IPluginExtensionPoint[] extPoints = model.getPluginBase().getExtensionPoints();
		for (IPluginExtensionPoint extPoint : extPoints) {
			String schemaLocation = extPoint.getSchema();

			if (schemaLocation == null || schemaLocation.equals("")) { //$NON-NLS-1$
				continue;
			}
			Schema schema = null;
			try {
				File schemaFile = new File(model.getInstallLocation(), schemaLocation);
				XMLDefaultHandler handler = new XMLDefaultHandler();
				try {
					org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE()
							.parse(schemaFile, handler);
				} catch (SAXException | ParserConfigurationException e) {
					throw new IOException("XML Parser error", e); //$NON-NLS-1$
				}
				@SuppressWarnings("deprecation")
				URL url = schemaFile.toURL();
				SchemaDescriptor desc = new SchemaDescriptor(extPoint.getFullId(), url, schemaProvider);
				schema = (Schema) desc.getSchema(false);

				// Check that all included schemas are available
				ISchemaInclude[] includes = schema.getIncludes();
				for (ISchemaInclude include : includes) {
					if (include.getIncludedSchema() == null) {
						errorConsumer.accept(NLS.bind(PDECoreMessages.ConvertSchemaToHTML_CannotFindIncludedSchema,
								include.getLocation(), schemaFile));
					}
				}

				File directory = IPath.fromOSString(destination).isAbsolute() ? new File(destination)
						: new File(baseDir, destination);
				if (!directory.exists() || !directory.isDirectory()) {
					if (!directory.mkdirs()) {
						schema.dispose();
						return;
					}
				}

				String id = extPoint.getId();
				if (id.indexOf('.') == -1) {
					id = pluginID + "." + id; //$NON-NLS-1$
				}
				File file = new File(directory, id.replace('.', '_') + ".html"); //$NON-NLS-1$
				try (PrintWriter out = new PrintWriter(
						new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), true)) {
					fTransformer.transform(schema, out, cssURL, SchemaTransformer.BUILD);
				}
			} finally {
				if (schema != null) {
					schema.dispose();
				}
			}
		}
	}

	protected String getPluginID(String manifest) throws IOException {
		File file = IPath.fromOSString(manifest).isAbsolute() ? new File(manifest) : new File(baseDir, manifest);
		File OSGiFile = new File(file.getParentFile(), ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);

		if (OSGiFile.exists()) {
			try (FileInputStream manifestStream = new FileInputStream(OSGiFile)) {
				Map<String, String> headers = ManifestElement.parseBundleManifest(manifestStream, new HeaderMap<>());
				String value = headers.get(Constants.BUNDLE_SYMBOLICNAME);
				if (value == null) {
					return null;
				}
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, value);
				if (elements.length > 0) {
					return elements[0].getValue();
				}
			} catch (BundleException bundleException) {
				throw new IOException("Invalid Manifest " + OSGiFile, bundleException); //$NON-NLS-1$
			}
		}
		return null;
	}

	protected IPluginModelBase readManifestFile(String manifest) throws IOException, CoreException {
		File file = IPath.fromOSString(manifest).isAbsolute() ? new File(manifest) : new File(baseDir, manifest);
		try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			ExternalPluginModelBase model = null;
			switch (file.getName().toLowerCase(Locale.ENGLISH)) {
			case ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR:
				model = new ExternalFragmentModel();
				break;
			case ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR:
				model = new ExternalPluginModel();
				break;
			default:
				stream.close();
				throw new IOException(NLS.bind(PDECoreMessages.Builders_Convert_illegalValue, "manifest")); //$NON-NLS-1$
			}
			String parentPath = file.getParentFile().getAbsolutePath();
			model.setInstallLocation(parentPath);
			model.load(stream, false);
			stream.close();
			return model;
		}
	}

}
