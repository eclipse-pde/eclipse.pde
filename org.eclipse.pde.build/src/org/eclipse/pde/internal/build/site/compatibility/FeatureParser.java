/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site.compatibility;

import java.io.*;
import java.net.URL;
import javax.xml.parsers.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.IPDEBuildConstants;
import org.eclipse.pde.internal.build.Messages;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Default feature parser.
 * Parses the feature manifest file as defined by the platform.
 * 
 * @since 3.0
 */
public class FeatureParser extends DefaultHandler implements IPDEBuildConstants {

	private SAXParser parser;
	private Feature result;
	private URL url;
	private StringBuffer characters = null;
	private MultiStatus status = null;
	private boolean hasImports = false;

	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();

	public FeatureParser() {
		super();
		try {
			parserFactory.setNamespaceAware(true);
			this.parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
	}

	/**
	 * Parses the specified url and constructs a feature
	 */
	public Feature parse(URL featureURL) throws SAXException, IOException {
		result = null;
		InputStream in = null;
		try {
			url = featureURL;
			in = featureURL.openStream();
			parser.parse(new InputSource(in), this);
		} finally {
			if (in != null)
				in.close();
		}
		return result;
	}

	public MultiStatus getStatus() {
		return status;
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		//		Utils.debug("Start Element: uri:" + uri + " local Name:" + localName + " qName:" + qName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (PLUGIN.equals(localName)) {
			processPlugin(attributes);
		} else if ("description".equals(localName)) { //$NON-NLS-1$
			processDescription(attributes);
		} else if ("license".equals(localName)) { //$NON-NLS-1$
			processLicense(attributes);
		} else if ("copyright".equals(localName)) { //$NON-NLS-1$
			processCopyright(attributes);
		} else if (FEATURE.equals(localName)) {
			processFeature(attributes);
		} else if ("import".equals(localName)) { //$NON-NLS-1$
			processImport(attributes);
		} else if ("includes".equals(localName)) { //$NON-NLS-1$
			processIncludes(attributes);
		} else if ("install-handler".equals(localName)) { //$NON-NLS-1$
			processInstallHandler(attributes);
		} else if ("update".equals(localName)) { //$NON-NLS-1$
			processUpdateSite(attributes);
		} else if ("discovery".equals(localName)) { //$NON-NLS-1$
			processDiscoverySite(attributes);
		}
	}

	private void processImport(Attributes attributes) {
		String id = attributes.getValue(FEATURE);
		FeatureEntry entry = null;
		if (id != null) {
			entry = FeatureEntry.createRequires(id, attributes.getValue(VERSION), attributes.getValue("match"), attributes.getValue("filter"), false); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			id = attributes.getValue(PLUGIN);
			entry = FeatureEntry.createRequires(id, attributes.getValue(VERSION), attributes.getValue("match"), attributes.getValue("filter"), true); //$NON-NLS-1$ //$NON-NLS-2$ 
		}
		hasImports = true;
		result.addEntry(entry);
	}

	private void processIncludes(Attributes attributes) {
		FeatureEntry entry = new FeatureEntry(attributes.getValue(ID), attributes.getValue(VERSION), false);
		String flag = attributes.getValue("unpack"); //$NON-NLS-1$
		if (flag != null)
			entry.setUnpack(Boolean.valueOf(flag).booleanValue());
		flag = attributes.getValue("optional"); //$NON-NLS-1$
		if (flag != null)
			entry.setOptional(Boolean.valueOf(flag).booleanValue());
		setEnvironment(attributes, entry);
		result.addEntry(entry);
	}

	private void processInstallHandler(Attributes attributes) {
		result.setInstallHandler(attributes.getValue("handler")); //$NON-NLS-1$
		result.setInstallHandlerLibrary(attributes.getValue("library")); //$NON-NLS-1$
		result.setInstallHandlerURL(attributes.getValue("url")); //$NON-NLS-1$
	}

	private void processUpdateSite(Attributes attributes) {
		result.setUpdateSiteLabel(attributes.getValue("label")); //$NON-NLS-1$
		result.setUpdateSiteURL(attributes.getValue("url")); //$NON-NLS-1$
	}

	private void processDiscoverySite(Attributes attributes) {
		result.addDiscoverySite(attributes.getValue("label"), attributes.getValue("url")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void setEnvironment(Attributes attributes, FeatureEntry entry) {
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		String nl = attributes.getValue("nl"); //$NON-NLS-1$
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		entry.setEnvironment(os, ws, arch, nl);
	}

	protected Feature createFeature(String id, String version) {
		return new Feature(id, version);
	}

	protected void processFeature(Attributes attributes) {
		String id = attributes.getValue(ID);
		String ver = attributes.getValue(VERSION);

		if (id == null || id.trim().equals("") //$NON-NLS-1$
				|| ver == null || ver.trim().equals("")) { //$NON-NLS-1$
			error(NLS.bind(Messages.feature_parse_invalidIdOrVersion, (new String[] {id, ver})));
		} else {
			result = createFeature(id, ver);

			String os = attributes.getValue("os"); //$NON-NLS-1$
			String ws = attributes.getValue("ws"); //$NON-NLS-1$
			String nl = attributes.getValue("nl"); //$NON-NLS-1$
			String arch = attributes.getValue("arch"); //$NON-NLS-1$
			result.setEnvironment(os, ws, arch, nl);

			//TODO rootURLs
			if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
				File f = new File(url.getFile().replace('/', File.separatorChar));
				result.setURL("features" + "/" + f.getParentFile().getName() + "/");// + f.getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} else {
				// externalized URLs might be in relative form, ensure they are absolute				
				//				feature.setURL(Utils.makeAbsolute(Utils.getInstallURL(), url).toExternalForm());
			}

			result.setProviderName(attributes.getValue("provider-name")); //$NON-NLS-1$
			result.setLabel(attributes.getValue("label")); //$NON-NLS-1$
			result.setImage(attributes.getValue("image")); //$NON-NLS-1$
			result.setBrandingPlugin(attributes.getValue(PLUGIN));
			result.setLicenseFeature(attributes.getValue("license-feature")); //$NON-NLS-1$
			result.setLicenseFeatureVersion(attributes.getValue("license-feature-version")); //$NON-NLS-1$
			//			Utils.debug("End process DefaultFeature tag: id:" +id + " ver:" +ver + " url:" + feature.getURL()); 	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void processPlugin(Attributes attributes) {
		String id = attributes.getValue(ID);
		String version = attributes.getValue(VERSION);

		if (id == null || id.trim().equals("") || version == null || version.trim().equals("")) { //$NON-NLS-1$ //$NON-NLS-2$
			error(NLS.bind(Messages.feature_parse_invalidIdOrVersion, (new String[] {id, version})));
		} else {
			FeatureEntry plugin = new FeatureEntry(id, version, true);
			setEnvironment(attributes, plugin);
			String unpack = attributes.getValue("unpack"); //$NON-NLS-1$
			if (unpack != null)
				plugin.setUnpack(Boolean.valueOf(unpack).booleanValue());
			String fragment = attributes.getValue(FRAGMENT);
			if (fragment != null)
				plugin.setFragment(Boolean.valueOf(fragment).booleanValue());
			String filter = attributes.getValue("filter"); //$NON-NLS-1$
			if (filter != null)
				plugin.setFilter(filter);
			result.addEntry(plugin);

			//			Utils.debug("End process DefaultFeature tag: id:" + id + " ver:" + ver + " url:" + feature.getURL()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void processLicense(Attributes attributes) {
		result.setLicenseURL(attributes.getValue("url")); //$NON-NLS-1$
		characters = new StringBuffer();
	}

	private void processCopyright(Attributes attributes) {
		result.setCopyrightURL(attributes.getValue("url")); //$NON-NLS-1$
		characters = new StringBuffer();
	}

	private void processDescription(Attributes attributes) {
		result.setDescriptionURL(attributes.getValue("url")); //$NON-NLS-1$
		characters = new StringBuffer();
	}

	public void characters(char[] ch, int start, int length) {
		if (characters == null)
			return;
		characters.append(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName) {
		if ("requires".equals(localName) && !hasImports) { //$NON-NLS-1$
			error(Messages.feature_parse_emptyRequires);
		}
		if (characters == null)
			return;
		if ("description".equals(localName)) { //$NON-NLS-1$
			result.setDescription(characters.toString().trim());
		} else if ("license".equals(localName)) { //$NON-NLS-1$
			result.setLicense(characters.toString().trim());
		} else if ("copyright".equals(localName)) { //$NON-NLS-1$
			result.setCopyright(characters.toString().trim());
		}
		characters = null;
	}

	private void error(String message) {
		if (status == null) {
			String msg = NLS.bind(Messages.exception_featureParse, url.toExternalForm());
			status = new MultiStatus(PI_PDEBUILD, EXCEPTION_FEATURE_PARSE, msg, null);
		}
		status.add(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_PARSE, message, null));
	}
}
