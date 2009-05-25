/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     G&H Softwareentwicklung GmbH - internationalization implementation (bug 150933)
 *     Michael Seele -  remove offline-allowed  (bug 153403)
 *******************************************************************************/

package org.eclipse.pde.internal.build.tasks;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @since 3.1
 */
public class JNLPGenerator extends DefaultHandler {

	private SAXParser parser;
	private final File featureRoot;

	private final String codebase;
	private final String j2se;

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
	private String currentArch = null;
	private Locale locale = null;
	private PropertyResourceBundle nlsBundle = null;
	private final boolean generateOfflineAllowed;
	private Config[] configs;

	/**
	 * For testing purposes only.
	 */
	public static void main(String[] args) {
		JNLPGenerator generator = new JNLPGenerator(args[0], args[1], args[2], args[3]);
		generator.process();
	}

	/**
	 * Constructs a feature parser.
	 */
	public JNLPGenerator(String feature, String destination, String codebase, String j2se) {
		this(feature, destination, codebase, j2se, Locale.getDefault(), true, null);
	}

	/**
	 * Constructs a feature parser.
	 */
	public JNLPGenerator(String feature, String destination, String codebase, String j2se, Locale locale, boolean generateOfflineAllowed, String configs) {
		super();
		this.featureRoot = new File(feature);
		this.destination = destination;
		this.codebase = codebase;
		this.j2se = j2se;
		this.locale = locale;
		this.generateOfflineAllowed = generateOfflineAllowed;
		try {
			parserFactory.setNamespaceAware(true);
			parser = parserFactory.newSAXParser();
		} catch (ParserConfigurationException e) {
			System.out.println(e);
		} catch (SAXException e) {
			System.out.println(e);
		}
		setConfigInfo(configs);
	}

	/**
	 * Parses the specified url and constructs a feature
	 */
	public void process() {
		InputStream in = null;
		final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$

		try {
			ZipFile featureArchive = null;
			InputStream nlsStream = null;
			if (featureRoot.isFile()) {
				featureArchive = new ZipFile(featureRoot);
				nlsStream = getNLSStream(featureArchive);
				ZipEntry featureXML = featureArchive.getEntry(FEATURE_XML);
				in = featureArchive.getInputStream(featureXML);
			} else {
				nlsStream = getNLSStream(this.featureRoot);
				in = new BufferedInputStream(new FileInputStream(new File(featureRoot, FEATURE_XML)));
			}
			try {
				if (nlsStream != null) {
					nlsBundle = new PropertyResourceBundle(nlsStream);
					nlsStream.close();
				}
			} catch (IOException e) {
				// do nothing
			}
			try {
				parser.parse(new InputSource(in), this);
				writeResourceEpilogue();
				writeEpilogue();
			} catch (SAXException e) {
				//Ignore the exception
			} finally {
				in.close();
				if (out != null)
					out.close();
				if (featureArchive != null)
					featureArchive.close();
			}
		} catch (IOException e) {
			//Ignore the exception
		}
	}

	/**
	 * Search for nls properties files and return the stream if files are found.
	 * First try to load the default properties file, then one with the default
	 * locale settings and if nothing matches, return the stream of the first
	 * properties file found.
	 */
	private InputStream getNLSStream(File root) {
		String appendix = ".properties"; //$NON-NLS-1$
		String[] potentials = createNLSPotentials();

		Map validEntries = new HashMap();
		File[] files = root.listFiles();
		for (int i = 0; i < files.length; i++) {
			String filename = files[i].getName();
			if (filename.endsWith(appendix)) {
				validEntries.put(filename, files[i]);
			}
		}
		InputStream stream = null;
		if (validEntries.size() > 0) {
			for (int i = 0; i < potentials.length; i++) {
				File file = (File) validEntries.get(potentials[i]);
				if (file != null) {
					try {
						stream = new BufferedInputStream(new FileInputStream(file));
						break;
					} catch (IOException e) {
						// do nothing
					}
				}
			}
			if (stream == null) {
				File file = (File) validEntries.values().iterator().next();
				try {
					stream = new BufferedInputStream(new FileInputStream(file));
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return stream;
	}

	/**
	 * Search for nls properties files and return the stream if files are found.
	 * First try to load the default properties file, then one with the default
	 * locale settings and if nothing matches, return the stream of the first
	 * founded properties file.
	 */
	private InputStream getNLSStream(ZipFile featureArchive) {
		String appendix = ".properties"; //$NON-NLS-1$
		String[] potentials = createNLSPotentials();

		Map validEntries = new HashMap();
		for (Enumeration enumeration = featureArchive.entries(); enumeration.hasMoreElements();) {
			ZipEntry entry = (ZipEntry) enumeration.nextElement();
			String entryName = entry.getName();
			if (entryName.endsWith(appendix)) {
				validEntries.put(entryName, entry);
			}
		}
		InputStream stream = null;
		if (validEntries.size() > 0) {
			for (int i = 0; i < potentials.length; i++) {
				ZipEntry entry = (ZipEntry) validEntries.get(potentials[i]);
				if (entry != null) {
					try {
						stream = featureArchive.getInputStream(entry);
						break;
					} catch (IOException e) {
						// do nothing
					}
				}
			}
			if (stream == null) {
				ZipEntry entry = (ZipEntry) validEntries.values().iterator().next();
				try {
					stream = featureArchive.getInputStream(entry);
				} catch (IOException e) {
					// do nothing
				}
			}
		}
		return stream;
	}

	private String[] createNLSPotentials() {
		String suffix = "feature"; //$NON-NLS-1$
		String appendix = ".properties"; //$NON-NLS-1$

		String language = locale.getLanguage();
		String country = locale.getCountry();
		String variant = locale.getVariant();

		String potential1 = '_' + language + '_' + country + '_' + variant;
		String potential2 = '_' + language + '_' + country;
		String potential3 = '_' + language;
		String potential4 = ""; //$NON-NLS-1$

		String[] potentials = new String[] {potential1, potential2, potential3, potential4};
		for (int i = 0; i < potentials.length; i++) {
			potentials[i] = suffix + potentials[i] + appendix;
		}
		return potentials;
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
		String pluginId = attributes.getValue("id"); //$NON-NLS-1$
		String pluginVersion = attributes.getValue("version"); //$NON-NLS-1$
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		if (isValidEnvironment(os, ws, arch)) {
			writeResourcePrologue(os, ws, arch);
			out.println("\t\t<jar href=\"plugins/" + pluginId + "_" + pluginVersion + ".jar\"/>"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void writeResourceEpilogue() {
		if (!resourceWritten)
			return;
		out.println("\t</resources>"); //$NON-NLS-1$
		resourceWritten = false;
		currentOS = null;
	}

	private void writeResourcePrologue(String os, String ws, String arch) {
		if (os == null)
			os = ws;
		os = convertOS(os);
		arch = convertArch(arch);
		if (resourceWritten && osMatch(os) && archMatch(arch))
			return;
		if (resourceWritten)
			writeResourceEpilogue();
		out.println("\t<resources" + (os == null ? "" : " os=\"" + os + "\"") + (arch == null ? "" : " arch=\"" + arch + "\"") + ">"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$//$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$//$NON-NLS-7$ //$NON-NLS-8$
		resourceWritten = true;
		currentOS = os;
		currentArch = arch;
	}

	private String convertOS(String os) {
		if (os == null)
			return null;
		if ("win32".equalsIgnoreCase(os)) //$NON-NLS-1$
			return "Windows"; //$NON-NLS-1$
		if ("macosx".equalsIgnoreCase(os)) //$NON-NLS-1$
			return "Mac"; //$NON-NLS-1$
		if ("linux".equalsIgnoreCase(os)) //$NON-NLS-1$
			return "Linux"; //$NON-NLS-1$
		if ("solaris".equalsIgnoreCase(os)) //$NON-NLS-1$
			return "Solaris"; //$NON-NLS-1$
		if ("hpux".equalsIgnoreCase(os)) //$NON-NLS-1$
			return "HP-UX"; //$NON-NLS-1$
		if ("aix".equalsIgnoreCase(os)) //$NON-NLS-1$
			return "AIX"; //$NON-NLS-1$
		return os;
	}

	private boolean osMatch(String os) {
		if (os == currentOS)
			return true;
		if (os == null)
			return false;
		return os.equals(currentOS);
	}

	private String convertArch(String arch) {
		if (arch == null)
			return null;

		if ("x86".equals(arch)) //$NON-NLS-1$
			return "x86"; //$NON-NLS-1$

		if ("PA_RISC".equals(arch)) //$NON-NLS-1$
			return "PA_RISC"; //$NON-NLS-1$

		if ("ppc".equals(arch)) //$NON-NLS-1$
			return "ppc"; //$NON-NLS-1$

		if ("sparc".equals(arch)) //$NON-NLS-1$
			return "sparc"; //$NON-NLS-1$

		if ("x86_64".equals(arch))//$NON-NLS-1$
			return "x86_64"; //$NON-NLS-1$

		if ("ia64".equals(arch)) //$NON-NLS-1$
			return "ia64"; //$NON-NLS-1$

		if ("ia64_32".equals(arch)) //$NON-NLS-1$
			return "ia64_32"; //$NON-NLS-1$

		return arch;
	}

	private boolean archMatch(String arch) {
		if (arch == currentOS)
			return true;
		if (arch == null)
			return false;
		return arch.equals(currentArch);
	}

	private void processDescription(Attributes attributes) {
		// ignoring for now
	}

	private void processIncludes(Attributes attributes) throws IOException {
		writePrologue();
		String inclusionId = attributes.getValue("id"); //$NON-NLS-1$
		String inclusionVersion = attributes.getValue("version"); //$NON-NLS-1$
		String name = attributes.getValue("name"); //$NON-NLS-1$
		String os = attributes.getValue("os"); //$NON-NLS-1$
		String ws = attributes.getValue("ws"); //$NON-NLS-1$
		String arch = attributes.getValue("arch"); //$NON-NLS-1$
		if (isValidEnvironment(os, ws, arch)) {
			writeResourcePrologue(os, ws, arch);
			out.print("\t\t<extension ");//$NON-NLS-1$
			if (name != null)
				out.print("name=\"" + name + "\" "); //$NON-NLS-1$ //$NON-NLS-2$
			if (inclusionId != null) {
				out.print("href=\"features/" + inclusionId); //$NON-NLS-1$
				if (inclusionVersion != null)
					out.print('_' + inclusionVersion);
				out.print(".jnlp\" "); //$NON-NLS-1$
			}
			out.println("/>"); //$NON-NLS-1$
		}
	}

	private void processFeature(Attributes attributes) {
		id = attributes.getValue("id"); //$NON-NLS-1$
		version = attributes.getValue("version"); //$NON-NLS-1$
		label = processNLS(attributes.getValue("label")); //$NON-NLS-1$
		provider = processNLS(attributes.getValue("provider-name")); //$NON-NLS-1$
	}

	/**
	 * Search for a human readable string in the feature.properties file(s) if
	 * the given string is a translateable key.
	 *
	 * @param string a translateable key or a normal string(nothing is done)
	 *
	 * @return a translateabled string or the given string if it is not a
	 *         translateable key
	 */
	private String processNLS(String string) {
		if (string == null)
			return null;
		string = string.trim();
		if (!string.startsWith("%")) { //$NON-NLS-1$
			return string;
		}
		if (string.startsWith("%%")) { //$NON-NLS-1$
			return string.substring(1);
		}
		int index = string.indexOf(" "); //$NON-NLS-1$
		String key = index == -1 ? string : string.substring(0, index);
		String dflt = index == -1 ? string : string.substring(index + 1);
		if (nlsBundle == null) {
			return dflt;
		}
		try {
			return nlsBundle.getString(key.substring(1));
		} catch (MissingResourceException e) {
			return dflt;
		}
	}

	private void writePrologue() throws IOException {
		if (out != null)
			return;
		if (destination == null) {
			featureRoot.getParentFile();
			destination = featureRoot.getParent() + '/';
		}
		if (destination.endsWith("/") || destination.endsWith("\\")) //$NON-NLS-1$  //$NON-NLS-2$
			destination = new File(featureRoot.getParentFile(), id + "_" + version + ".jnlp").getAbsolutePath(); //$NON-NLS-1$ //$NON-NLS-2$
		out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(destination)));
		writePrologue();
		out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); //$NON-NLS-1$
		out.print("<jnlp spec=\"1.0+\" "); //$NON-NLS-1$
		if (codebase != null)
			out.print("codebase=\"" + codebase); //$NON-NLS-1$
		out.println("\">"); //$NON-NLS-1$
		out.println("\t<information>"); //$NON-NLS-1$
		if (label != null)
			out.println("\t\t<title>" + label + "</title>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (provider != null)
			out.println("\t\t<vendor>" + provider + "</vendor>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (description != null)
			out.println("\t\t<description>" + description + "</description>"); //$NON-NLS-1$ //$NON-NLS-2$
		if (generateOfflineAllowed)
			out.println("\t\t<offline-allowed/>"); //$NON-NLS-1$
		out.println("\t</information>"); //$NON-NLS-1$
		out.println("\t<security>"); //$NON-NLS-1$
		out.println("\t\t<all-permissions/>"); //$NON-NLS-1$
		out.println("\t</security>"); //$NON-NLS-1$
		out.println("\t<component-desc/>"); //$NON-NLS-1$
		out.println("\t<resources>"); //$NON-NLS-1$
		out.println("\t\t<j2se version=\"" + j2se + "\" />"); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("\t</resources>"); //$NON-NLS-1$
	}

	private void writeEpilogue() {
		out.println("</jnlp>"); //$NON-NLS-1$
	}

	private boolean isMatching(String candidateValues, String siteValues) {
		if (candidateValues == null)
			return true;
		if (siteValues == null)
			return false;
		if ("*".equals(candidateValues))return true; //$NON-NLS-1$
		if ("".equals(candidateValues))return true; //$NON-NLS-1$
		StringTokenizer siteTokens = new StringTokenizer(siteValues, ","); //$NON-NLS-1$
		//$NON-NLS-1$	
		while (siteTokens.hasMoreTokens()) {
			StringTokenizer candidateTokens = new StringTokenizer(candidateValues, ","); //$NON-NLS-1$
			String siteValue = siteTokens.nextToken();
			while (candidateTokens.hasMoreTokens()) {
				if (siteValue.equalsIgnoreCase(candidateTokens.nextToken()))
					return true;
			}
		}
		return false;
	}

	private boolean isValidEnvironment(String os, String ws, String arch) {
		if (configs.length == 0)
			return true;
		for (int i = 0; i < configs.length; i++) {
			if (isMatching(os, configs[i].getOs()) && isMatching(ws, configs[i].getWs()) && isMatching(arch, configs[i].getArch()))
				return true;
		}
		return false;
	}

	private void setConfigInfo(String spec) {
		if (spec != null && spec.startsWith("$")) { //$NON-NLS-1$
			configs = new Config[0];
			return;
		}
		if (spec == null) {
			configs = new Config[] {Config.genericConfig()};
			return;
		}
		StringTokenizer tokens = new StringTokenizer(spec, "&"); //$NON-NLS-1$
		int configNbr = tokens.countTokens();
		ArrayList configInfos = new ArrayList(configNbr);
		while (tokens.hasMoreElements()) {
			String aConfig = tokens.nextToken();
			StringTokenizer configTokens = new StringTokenizer(aConfig, ","); //$NON-NLS-1$
			if (configTokens.countTokens() == 3) {
				Config toAdd = new Config(configTokens.nextToken().trim(), configTokens.nextToken().trim(), configTokens.nextToken().trim());
				if (toAdd.equals(Config.genericConfig()))
					toAdd = Config.genericConfig();
				configInfos.add(toAdd);
			}
		}
		if (configInfos.size() == 0)
			configInfos.add(Config.genericConfig());
		configs = (Config[]) configInfos.toArray(new Config[configInfos.size()]);
	}
}
