/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.*;
import org.eclipse.update.internal.configurator.FeatureEntry;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * @since 3.1
 */
public class JNLPGenerator extends DefaultHandler {

	private SAXParser parser;
	private FeatureEntry feature;
	private URL url;

	private String codebase;
	private String j2se;

	/**
	 * id = ???
	 * version = jnlp.version
	 * label = information.title
	 * provider-name = information.vendor
	 * image = information.icon
	 * feature.description = information.description
	 * feature.includes = extension
	 * feature.plugin = jar
	 */
	private final static SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	private PrintWriter out;
	private String destination;
	private String provider;
	private String label;
	private String version;
	private String id;
	private String description;
	private boolean resourceWritten = false;
	private String currentOS = null;

	/**
	 * For testing purposes only.
	 */
	public static void main(String[] args) throws MalformedURLException {
		JNLPGenerator generator = new JNLPGenerator(new URL(args[0]), args[1], args[2], args[3]);
		generator.process();
	}

	/**
	 * Constructs a feature parser.
	 */
	public JNLPGenerator(URL featureURL, String destination, String codebase, String j2se) {
		super();
		this.url = featureURL;
		this.destination = destination;
		this.codebase = codebase;
		this.j2se = j2se;
		try {
			parserFactory.setNamespaceAware(true);
			parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
	}

	/**
	 * Parses the specified url and constructs a feature
	 */
	public void process() {
		InputStream in = null;
		try {
			in = url.openStream();
			try {
				parser.parse(new InputSource(in), this);
				writeResourceEpilogue();
				writeEpilogue();
			} catch (SAXException e) {
			} finally {
				in.close();
				if (out != null)
					out.close();
			}
		} catch (IOException e) {
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		try {
			if ("feature".equals(localName)) { //$NON-NLS-1$
				processFeature(attributes);
			} else if ("includes".equals(localName)) { //$NON-NLS-1$
				processIncludes(attributes);
			} else if ("description".equals(localName)) { //$NON-NLS-1$
				processDescription(attributes);
			} else if ("plugin".equals(localName)) { //$NON-NLS-1$
				processPlugin(attributes);
			}
		} catch (IOException e) {
			throw new SAXException(e);
		}
	}

	private void processPlugin(Attributes attributes) throws IOException {
		writePrologue();
		String id = attributes.getValue("id"); //$NON-NLS-1$
		String version = attributes.getValue("version"); //$NON-NLS-1$
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		writeResourcePrologue(os, ws);
		out.println("\t\t<jar href=\"../plugins/" + id + "_" + version + ".jnlp\"/>");
	}

	private void writeResourceEpilogue() {
		if (!resourceWritten)
			return;
		out.println("\t</resources>");
		resourceWritten = false;
		currentOS = null;
	}

	private void writeResourcePrologue(String os, String ws) {
		if (os == null)
			os = ws;
		os = convertOS(os);
		if (resourceWritten && osMatch(os))
			return;
		if (resourceWritten)
			writeResourceEpilogue();
		out.println("\t<resources" + (os == null ? "" : " os=\"" + os + "\"") + ">");
		resourceWritten = true;
		currentOS = os;
	}

	private String convertOS(String os) {
		if (os == null)
			return null;
		if (os.equals("win32"))
			return "Windows";
		return os;
	}

	private boolean osMatch(String os) {
		if (os == currentOS)
			return true;
		if (os == null)
			return false;
		return os.equals(currentOS);
	}

	private void processUpdate(Attributes attributes) {
		String href = attributes.getValue("url"); //$NON-NLS-1$
		if (codebase == null)
			codebase = href;
	}

	private void processDescription(Attributes attributes) {

	}

	private void processIncludes(Attributes attributes) throws IOException {
		writePrologue();
		String id = attributes.getValue("id"); //$NON-NLS-1$
//		String version = attributes.getValue("version"); //$NON-NLS-1$
		String name = attributes.getValue("name"); //$NON-NLS-1$
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		writeResourcePrologue(os, ws);
		out.print("\t\t<extension ");
		if (name != null)
			out.print("name=\"" + name + "\" ");
		if (id != null)
			out.print("href=\"features/" + id + ".jnlp\" ");
		out.println("/>");
	}

	private void processFeature(Attributes attributes) throws IOException {
		id = attributes.getValue("id"); //$NON-NLS-1$
		version = attributes.getValue("version"); //$NON-NLS-1$
		label = attributes.getValue("label"); //$NON-NLS-1$
		provider = attributes.getValue("provider-name"); //$NON-NLS-1$
//		String imageURL = attributes.getValue("image"); //$NON-NLS-1$
//		String os = attributes.getValue("os"); //$NON-NLS-1$
//		String ws = attributes.getValue("ws"); //$NON-NLS-1$
//		String nl = attributes.getValue("nl"); //$NON-NLS-1$
//		String arch = attributes.getValue("arch"); //$NON-NLS-1$
	}

	private void writePrologue() throws IOException {
		if (out != null)
			return;
		if (destination == null) {
			URL sourceURL = url;
			if (url.getProtocol().equalsIgnoreCase("jar"))
				sourceURL = new URL(url.getPath().replace('!', '/'));
			destination = new File(sourceURL.getPath()).getParentFile().getParent() + "/";
		}
		if (destination.endsWith("/") || destination.endsWith("\\"))
			destination = new File(destination, id + "_" + version + ".jnlp").getAbsolutePath();
		out = new PrintWriter(new FileOutputStream(destination));
		writePrologue();
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.print("<jnlp spec=\"1.0+\" ");
		if (codebase != null)
			out.print("codebase=\"" + codebase);
		out.println("\">");
		out.println("\t<information>");
		if (label != null)
			out.println("\t\t<title>" + label + "</title>");
		if (provider != null)
			out.println("\t\t<vendor>" + provider + "</vendor>");
		//		out.println("\t\t<homepage href=\"http://xxx\" />");
		if (description != null)
			out.println("\t\t<description>" + description + "</description>");
		out.println("\t</information>");
		out.println("\t<offline-allowed/>");
		out.println("\t<security>");
		out.println("\t\t<all-permissions/>");
		out.println("\t</security>");
		out.println("\t<component-desc/>");
		out.println("\t<resources>");
		out.println("\t\t<j2se version=\"" + j2se + "\" />");
		out.println("\t</resources>");
	}

	private void writeEpilogue() {
		out.println("</jnlp>");
	}
}
