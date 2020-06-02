/*******************************************************************************
 *  Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.pde.internal.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

// The update manager is deprecated, but still supported in PDE launching
// The implementation is only called from TargetPlatform.createPlatform
// and once that API has been removed then this class can be deleted.
// Code from this class was copied from the org.eclipse.update.configurator bundle,
// specifically the IPlatformConfiguration and PlatformConfiguration classes.
public class UpdateManagerHelperDeprecated {

	@Deprecated
	private static class LocalSite {
		private final ArrayList<IPluginModelBase> fPlugins;
		private IPath fPath;

		LocalSite(IPath path) {
			if (path.getDevice() != null) {
				fPath = path.setDevice(path.getDevice().toUpperCase(Locale.ENGLISH));
			} else {
				fPath = path;
			}
			fPlugins = new ArrayList<>();
		}

		IPath getPath() {
			return fPath;
		}

		URL getURL() throws MalformedURLException {
			return new URL("file:" + fPath.removeTrailingSeparator()); //$NON-NLS-1$
		}

		void add(IPluginModelBase model) {
			fPlugins.add(model);
		}

		private static String relative(IPluginModelBase model) {
			IPath location = new Path(model.getInstallLocation());
			// defect 37319
			if (location.segmentCount() > 2) {
				location = location.removeFirstSegments(location.segmentCount() - 2);
			}
			// 31489 - entry must be relative
			return location.setDevice(null).makeRelative().toString();
		}

		void write(Writer writer) throws IOException {
			writer.write("<site enabled=\"true\" list=\""); //$NON-NLS-1$
			boolean comma = false;
			for (IPluginModelBase model : fPlugins) {
				if (comma) {
					writer.write(',');
				}
				comma = true;
				writer.write(relative(model));
			}
			writer.write("\" policy=\"USER-INCLUDE\" updateable=\"true\" url=\""); //$NON-NLS-1$
			writer.write(getURL().toString());
			writer.write("/\">\n"); //$NON-NLS-1$
			writer.write("</site>\n"); //$NON-NLS-1$
		}
	}

	@Deprecated
	public static void createPlatformConfiguration(File configLocation, IPluginModelBase[] models,
			IPluginModelBase brandingPlugin) throws CoreException {
		try {
			if (!configLocation.exists()) {
				return;
			}
			File platform_xml = new File(configLocation, "org.eclipse.update/platform.xml"); //$NON-NLS-1$
			platform_xml.getParentFile().mkdirs();
			// Compute local sites
			ArrayList<LocalSite> sites = new ArrayList<>();
			for (IPluginModelBase model : models) {
				IPath path = new Path(model.getInstallLocation()).removeLastSegments(2);
				addToSite(path, model, sites);
			}
			if (brandingPlugin != null) {
				IPath path = new Path(brandingPlugin.getInstallLocation()).removeLastSegments(2);
				addToSite(path, brandingPlugin, sites);
			}

			try (BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(new FileOutputStream(platform_xml), StandardCharsets.UTF_8))) {
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
				writer.write("<config date=\"" //$NON-NLS-1$
						+ new Date().getTime() + "\" transient=\"true\" version=\"3.0\">\n"); //$NON-NLS-1$
				for (LocalSite site : sites) {
					site.write(writer);
					writeFeatures(writer, site.fPath.toFile());
				}
				writer.write("</config>"); //$NON-NLS-1$
			}


		} catch (Exception e) {
			// Wrap everything else in a core exception.
			String message = e.getMessage();
			if (message == null || message.length() == 0) {
				message = PDECoreMessages.TargetPlatform_exceptionThrown;
			}
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, message, e));
		}
	}

	private static void addToSite(IPath path, IPluginModelBase model, ArrayList<LocalSite> sites) {
		if (path.getDevice() != null) {
			path = path.setDevice(path.getDevice().toUpperCase(Locale.ENGLISH));
		}
		for (int i = 0; i < sites.size(); i++) {
			LocalSite localSite = sites.get(i);
			if (localSite.getPath().equals(path)) {
				localSite.add(model);
				return;
			}
		}
		// First time - add site
		LocalSite localSite = new LocalSite(path);
		localSite.add(model);
		sites.add(localSite);
	}

	static class FeatureEntry {
		static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		final String id;
		final String version;
		final String url;
		final String brandingPlugin;

		FeatureEntry(File xml) throws SAXException, IOException, ParserConfigurationException {
			Document document = factory.newDocumentBuilder().parse(xml);
			NamedNodeMap attributes = document.getFirstChild().getAttributes();
			String name = xml.getParentFile().getName();
			url = FEATURES + "/" + name + "/"; //$NON-NLS-1$ //$NON-NLS-2$
			id = get("id", attributes); //$NON-NLS-1$
			version = get("version", attributes); //$NON-NLS-1$
			brandingPlugin = get("plugin", attributes); //$NON-NLS-1$
		}

		String get(String name, NamedNodeMap attribues) {
			Node node = attribues.getNamedItem(name);
			return node == null ? null : node.getTextContent();
		}
	}

	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$

	private static void writeFeatures(Writer writer, File location) {
		File featuresDir = new File(location, FEATURES);
		if (featuresDir.exists()) {
			// handle the installed features under the features directory
			File[] dirs = featuresDir.listFiles((FileFilter) f -> {
				return f.isDirectory() && (new File(f, FEATURE_XML).exists());
			});

			for (File dir : dirs) {
				File featureXML = new File(dir, FEATURE_XML);
				try {
					FeatureEntry feature = new FeatureEntry(featureXML);
					writeFeatureEntry(writer, feature);
				} catch (Exception e) {
					// Ignore
				}
			}
		}
	}

	private static void writeFeatureEntry(Writer writer, FeatureEntry feature) throws IOException {
		writer.write("<feature id=\""); //$NON-NLS-1$
		writer.write(feature.id);
		if (feature.brandingPlugin != null) {
			writer.write("\" plugin-identifier=\""); //$NON-NLS-1$
			writer.write(feature.brandingPlugin);
		}
		writer.write("\" url=\""); //$NON-NLS-1$
		writer.write(feature.url);
		writer.write("\" version=\""); //$NON-NLS-1$
		writer.write(feature.version);
		writer.write("\">\n</feature>\n"); //$NON-NLS-1$
	}

}
