/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Provides static methods that will serialize and deserialize xml representing a target definition
 * 
 * @see ITargetDefinition
 */
public class TargetDefinitionPersistenceHelper {

	/**
	 * Constants for XML element names and attributes
	 */
	private static final String ROOT = "target"; //$NON-NLS-1$
	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String LOCATIONS = "locations"; //$NON-NLS-1$
	private static final String LOCATION = "location"; //$NON-NLS-1$
	private static final String ATTR_LOCATION_PATH = "path"; //$NON-NLS-1$
	private static final String ATTR_LOCATION_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_USE_DEFAULT = "useDefault"; //$NON-NLS-1$
	private static final String INCLUDE_BUNDLES = "includeBundles"; //$NON-NLS-1$
	private static final String OPTIONAL_BUNDLES = "optionalBundles"; //$NON-NLS-1$
	private static final String ENVIRONMENT = "environment"; //$NON-NLS-1$
	private static final String OS = "os"; //$NON-NLS-1$
	private static final String WS = "ws"; //$NON-NLS-1$
	private static final String ARCH = "arch"; //$NON-NLS-1$
	private static final String NL = "nl"; //$NON-NLS-1$
	private static final String TARGET_JRE = "targetJRE"; //$NON-NLS-1$
	private static final String EXEC_ENV = "execEnv"; //$NON-NLS-1$
	private static final String JRE_NAME = "jreName"; //$NON-NLS-1$
	private static final String ARGUMENTS = "launcherArgs"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	private static final String VM_ARGS = "vmArgs"; //$NON-NLS-1$
	private static final String IMPLICIT = "implicitDependencies"; //$NON-NLS-1$
	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
	private static final String PDE_INSTRUCTION = "pde"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String REPOSITORIES = "repositories"; //$NON-NLS-1$
	private static final String REPOSITORY = "repository"; //$NON-NLS-1$
	private static final String ATTR_OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String INSTALLABLE_UNIT = "unit"; //$NON-NLS-1$
	private static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	private static final String ATTR_CONFIGURATION = "configuration"; //$NON-NLS-1$
	private static final String CONTENT = "content"; //$NON-NLS-1$
	private static final String ATTR_USE_ALL = "useAllPlugins"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String EXTRA_LOCATIONS = "extraLocations"; //$NON-NLS-1$

	private static final String TYPE_DIRECTORY = "Directory"; //$NON-NLS-1$
	private static final String TYPE_INSTALLATION = "Profile"; //$NON-NLS-1$
	private static final String TYPE_REPOSITORY = "InstallableUnit"; //$NON-NLS-1$
	private static final String TYPE_FEATURE = "Feature"; //$NON-NLS-1$

	private static ITargetPlatformService fTargetService;

	/* 
	** Example Legacy < 3.5 XML **
	
	<?xml version="1.0" encoding="UTF-8"?>
	<?pde version="3.2"?>

	<target>

	<targetJRE>
	  <execEnv>CDC-1.0/Foundation-1.0</execEnv>
	</targetJRE>

	<environment>
		<os>hpux</os>
		<ws>gtk</ws>
		<arch>ia64</arch>
		<nl>ar</nl>
	</environment>

	<launcherArgs>
		<vmArgs>vm</vmArgs>
		<programArgs>program</programArgs>
	</launcherArgs>

	<location path="d:\targets\provisioning-base"/>

	<content>
	  <plugins>
	     <plugin id="org.eclipse.core.jobs"/>
	     <plugin id="org.eclipse.equinox.app"/>
	     <plugin id="org.eclipse.osgi"/>
	     <plugin id="org.eclipse.osgi.services"/>
	     <plugin id="org.junit"/>
	  </plugins>
	  <features>
	  </features>
	  <extraLocations>
	     <location path="D:\targets\equinox\eclipse"/>
	  </extraLocations>
	</content>
	
	<implicitDependencies>
	  <plugin id="javax.servlet"/>
	  <plugin id="com.jcraft.jsch"/>
	  <plugin id="ie.wombat.jbdiff"/>
	  <plugin id="javax.servlet.jsp"/>
	  <plugin id="ie.wombat.jbdiff.test"/>
	</implicitDependencies>
	
	</target>

	** Example 3.5 XML **

	<?xml version="1.0" encoding="UTF-8" standalone="no"?>
	<?pde version="3.5"?>

	<target name="A name">
	<locations>
	<location path="d:\targets\provisioning-base" type="Profile">
	<includeBundles>
	<plugin id="org.eclipse.core.jobs"/>
	<plugin id="org.eclipse.equinox.app"/>
	<plugin id="org.eclipse.osgi"/>
	<plugin id="org.eclipse.osgi.services"/>
	<plugin id="org.junit"/>
	</includeBundles>
	</location>
	<location path="D:\targets\equinox\eclipse" type="Directory">
	<includeBundles>
	<plugin id="org.eclipse.core.jobs"/>
	<plugin id="org.eclipse.equinox.app"/>
	<plugin id="org.eclipse.osgi"/>
	<plugin id="org.eclipse.osgi.services"/>
	<plugin id="org.junit"/>
	</includeBundles>
	</location>
	</locations>
	<environment>
	<os>hpux</os>
	<ws>gtk</ws>
	<arch>ia64</arch>
	<nl>ar</nl>
	</environment>
	<targetJRE path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/CDC-1.0%Foundation-1.0"/>
	<launcherArgs>
	<vmArgs>vm</vmArgs>
	<programArgs>program</programArgs>
	</launcherArgs>
	<implicitDependencies>
	<plugin id="javax.servlet"/>
	<plugin id="com.jcraft.jsch"/>
	<plugin id="ie.wombat.jbdiff"/>
	<plugin id="javax.servlet.jsp"/>
	<plugin id="ie.wombat.jbdiff.test"/>
	</implicitDependencies>
	</target>
	
	** Example 3.6 XML **

	<?xml version="1.0" encoding="UTF-8" standalone="no"?>
	<?pde version="3.6"?>
	
	<target name="Everything">
	<locations>
	<location path="${eclipse_home}" type="Directory"/>
	<location path="/home/cwindatt/Eclipse/Eclipse 3.5 Update" type="Profile"/>
	<location id="org.eclipse.pde" path="${eclipse_home}" type="Feature"/>
	<location type="InstallableUnit">
	<unit id="org.eclipse.cvs.feature.group" version="1.1.101.R35x_v20090811-7E79FEd9KKF5H2YDWFLLBL01A16"/>
	</location>
	</locations>
	<repositories>
	<repository location="http://download.eclipse.org/releases/galileo"/>
	</repositories>
	<includeBundles>
	<plugin id="org.apache.commons.codec.source" version="1.3.0.v20080530-1600"/>
	<plugin id="org.apache.commons.codec" version="1.3.0.v20080530-1600"/>
	<plugin id="org.apache.commons.el.source" version="1.0.0.v200806031608"/>
	<plugin id="org.apache.commons.el" version="1.0.0.v200806031608"/>
	<plugin id="org.apache.commons.httpclient.source" version="3.1.0.v20080605-1935"/>
	<plugin id="org.apache.commons.httpclient" version="3.1.0.v20080605-1935"/>
	</includeBundles>
	<optionalBundles>
	<plugin id="org.apache.ant" version="1.7.1.v20090120-1145"/>
	</optionalBundles>
	<implicitDependencies>
	<plugin id="com.ibm.icu" version="0.0.0"/>
	</implicitDependencies>
	</target>
	
	 */

	/**
	 * Serializes a target definition to xml and writes the xml to the given stream
	 * @param definition target definition to serialize
	 * @param output output stream to write xml to
	 * @throws CoreException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public static void persistXML(ITargetDefinition definition, OutputStream output) throws CoreException, ParserConfigurationException, TransformerException, IOException {
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		ProcessingInstruction instruction = doc.createProcessingInstruction(PDE_INSTRUCTION, ATTR_VERSION + "=\"" + ICoreConstants.TARGET36 + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		doc.appendChild(instruction);

		Element rootElement = doc.createElement(ROOT);

		if (definition.getName() != null) {
			rootElement.setAttribute(ATTR_NAME, definition.getName());
		}

		IBundleContainer[] containers = definition.getBundleContainers();
		if (containers.length > 0) {
			Element containersElement = doc.createElement(LOCATIONS);
			for (int i = 0; i < containers.length; i++) {
				Element containerElement = serializeLocation(doc, containers[i]);
				containersElement.appendChild(containerElement);
			}
			rootElement.appendChild(containersElement);
		}

		URI[] repos = definition.getRepositories();
		if (repos.length > 0) {
			Element reposElement = doc.createElement(REPOSITORIES);
			for (int i = 0; i < repos.length; i++) {
				Element repoElement = doc.createElement(REPOSITORY);
				repoElement.setAttribute(LOCATION, repos[i].toASCIIString());
				reposElement.appendChild(repoElement);
			}
			rootElement.appendChild(reposElement);
		}

		NameVersionDescriptor[] includedBundles = definition.getIncluded();
		if (includedBundles != null) {
			Element included = doc.createElement(INCLUDE_BUNDLES);
			serializeBundles(doc, included, includedBundles);
			rootElement.appendChild(included);
		}

		NameVersionDescriptor[] optionalBundles = definition.getOptional();
		if (optionalBundles != null) {
			Element optional = doc.createElement(OPTIONAL_BUNDLES);
			serializeBundles(doc, optional, optionalBundles);
			rootElement.appendChild(optional);
		}

		if (definition.getOS() != null || definition.getWS() != null || definition.getArch() != null || definition.getNL() != null) {
			Element envElement = doc.createElement(ENVIRONMENT);
			if (definition.getOS() != null) {
				Element element = doc.createElement(OS);
				setTextContent(element, definition.getOS());
				envElement.appendChild(element);
			}
			if (definition.getWS() != null) {
				Element element = doc.createElement(WS);
				setTextContent(element, definition.getWS());
				envElement.appendChild(element);
			}
			if (definition.getArch() != null) {
				Element element = doc.createElement(ARCH);
				setTextContent(element, definition.getArch());
				envElement.appendChild(element);
			}
			if (definition.getNL() != null) {
				Element element = doc.createElement(NL);
				setTextContent(element, definition.getNL());
				envElement.appendChild(element);
			}
			rootElement.appendChild(envElement);
		}

		if (definition.getJREContainer() != null) {
			Element jreElement = doc.createElement(TARGET_JRE);
			IPath path = definition.getJREContainer();
			jreElement.setAttribute(ATTR_LOCATION_PATH, path.toPortableString());
			rootElement.appendChild(jreElement);
		}

		if (definition.getVMArguments() != null || definition.getProgramArguments() != null) {
			Element argElement = doc.createElement(ARGUMENTS);
			if (definition.getVMArguments() != null) {
				Element element = doc.createElement(VM_ARGS);
				setTextContent(element, definition.getVMArguments());
				argElement.appendChild(element);
			}
			if (definition.getProgramArguments() != null) {
				Element element = doc.createElement(PROGRAM_ARGS);
				setTextContent(element, definition.getProgramArguments());
				argElement.appendChild(element);
			}
			rootElement.appendChild(argElement);
		}

		NameVersionDescriptor[] implicitDependencies = definition.getImplicitDependencies();
		if (implicitDependencies != null && implicitDependencies.length > 0) {
			Element implicit = doc.createElement(IMPLICIT);
			for (int i = 0; i < implicitDependencies.length; i++) {
				Element plugin = doc.createElement(PLUGIN);
				plugin.setAttribute(ATTR_ID, implicitDependencies[i].getId());
				if (implicitDependencies[i].getVersion() != null) {
					plugin.setAttribute(ATTR_VERSION, implicitDependencies[i].getVersion().toString());
				}
				implicit.appendChild(plugin);
			}
			rootElement.appendChild(implicit);
		}

		doc.appendChild(rootElement);
		DOMSource source = new DOMSource(doc);

		StreamResult outputTarget = new StreamResult(output);
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		transformer.transform(source, outputTarget);
	}

	/**
	 * Parses an xml document from the input stream and deserializes it into a target definition.
	 * 
	 * @param definition definition to be filled with the result of deserialization
	 * @param input stream to get xml input from
	 * @throws CoreException
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static void initFromXML(ITargetDefinition definition, InputStream input) throws CoreException, ParserConfigurationException, SAXException, IOException {
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		parser.setErrorHandler(new DefaultHandler());
		Document doc = parser.parse(new InputSource(input));

		Element root = doc.getDocumentElement();
		if (!root.getNodeName().equalsIgnoreCase(ROOT)) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetDefinitionPersistenceHelper_0));
		}

		String name = root.getAttribute(ATTR_NAME);
		if (name.length() > 0) {
			definition.setName(name);
		}

		IBundleContainer oldStylePrimaryContainer = null;
		List bundleContainers = new ArrayList();
		// If we are reading a 3.5 target file the included/optional are per location and must be collected
		List included35 = new ArrayList();
		List optional35 = new ArrayList();
		List repositores35 = new ArrayList();
		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (nodeName.equalsIgnoreCase(LOCATIONS)) {
					NodeList locations = element.getChildNodes();
					for (int j = 0; j < locations.getLength(); ++j) {
						Node locationNode = locations.item(j);
						if (locationNode.getNodeType() == Node.ELEMENT_NODE) {
							Element locationElement = (Element) locationNode;
							if (locationElement.getNodeName().equalsIgnoreCase(LOCATION)) {
								bundleContainers.add(deserializeBundleContainer(locationElement, included35, optional35, repositores35));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(LOCATION)) {
					// This is the 'home' location in old style target platforms
					oldStylePrimaryContainer = deserializeBundleContainer(element, included35, optional35, repositores35);
				} else if (nodeName.equalsIgnoreCase(CONTENT)) {
					// Additional locations and other bundle content settings were stored under this tag in old style target platforms
					// Only included if the content has useAllPlugins='true' otherwise we create bundle containers for the restrictions
					boolean useAll = Boolean.TRUE.toString().equalsIgnoreCase(element.getAttribute(ATTR_USE_ALL));
					if (useAll) {
						bundleContainers.add(oldStylePrimaryContainer);
					}
					bundleContainers.addAll(deserializeBundleContainersFromOldStyleElement(element, oldStylePrimaryContainer, useAll, included35, optional35));
					// It is possible to have an empty content section, in which case we should add the primary container, bug 268709
					if (bundleContainers.isEmpty()) {
						bundleContainers.add(oldStylePrimaryContainer);
					}
				} else if (nodeName.equalsIgnoreCase(REPOSITORIES)) {
					URI[] uris = deserializeRepositories(element);
					definition.setRepositories(uris);
				} else if (nodeName.equalsIgnoreCase(INCLUDE_BUNDLES)) {
					NameVersionDescriptor[] included = deserializeBundles(element);
					definition.setIncluded(included);
				} else if (nodeName.equalsIgnoreCase(OPTIONAL_BUNDLES)) {
					NameVersionDescriptor[] optional = deserializeBundles(element);
					definition.setOptional(optional);
				} else if (nodeName.equalsIgnoreCase(ENVIRONMENT)) {
					NodeList envEntries = element.getChildNodes();
					for (int j = 0; j < envEntries.getLength(); ++j) {
						Node entry = envEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(OS)) {
								definition.setOS(getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(WS)) {
								definition.setWS(getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(ARCH)) {
								definition.setArch(getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(NL)) {
								definition.setNL(getTextContent(currentElement));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(TARGET_JRE)) {
					String text = element.getAttribute(ATTR_LOCATION_PATH);
					if (text.length() == 0) {
						// old format (or missing)
						NodeList argEntries = element.getChildNodes();
						for (int j = 0; j < argEntries.getLength(); ++j) {
							Node entry = argEntries.item(j);
							if (entry.getNodeType() == Node.ELEMENT_NODE) {
								Element currentElement = (Element) entry;
								IPath path = null;
								if (currentElement.getNodeName().equalsIgnoreCase(EXEC_ENV)) {
									IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(getTextContent(currentElement));
									if (env != null) {
										path = JavaRuntime.newJREContainerPath(env);
									}
								} else if (currentElement.getNodeName().equalsIgnoreCase(JRE_NAME)) {
									String vmName = getTextContent(currentElement);
									IVMInstall vmInstall = VMUtil.getVMInstall(vmName);
									if (vmInstall != null) {
										path = JavaRuntime.newJREContainerPath(vmInstall);
									}
								}
								definition.setJREContainer(path);
							}
						}
					} else {
						// new format - JRE container path
						IPath path = Path.fromPortableString(text);
						definition.setJREContainer(path);
					}
				} else if (nodeName.equalsIgnoreCase(ARGUMENTS)) {
					NodeList argEntries = element.getChildNodes();
					for (int j = 0; j < argEntries.getLength(); ++j) {
						Node entry = argEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(PROGRAM_ARGS)) {
								definition.setProgramArguments(getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(VM_ARGS)) {
								definition.setVMArguments(getTextContent(currentElement));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(ARGUMENTS)) {
					NodeList argEntries = element.getChildNodes();
					for (int j = 0; j < argEntries.getLength(); ++j) {
						Node entry = argEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(PROGRAM_ARGS)) {
								definition.setProgramArguments(getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(VM_ARGS)) {
								definition.setVMArguments(getTextContent(currentElement));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(IMPLICIT)) {
					NodeList implicitEntries = element.getChildNodes();
					List implicit = new ArrayList(implicitEntries.getLength());
					for (int j = 0; j < implicitEntries.getLength(); ++j) {
						Node entry = implicitEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(PLUGIN)) {
								String version = currentElement.getAttribute(ATTR_VERSION);
								NameVersionDescriptor bundle = version.length() > 0 ? new NameVersionDescriptor(currentElement.getAttribute(ATTR_ID), version) : new NameVersionDescriptor(currentElement.getAttribute(ATTR_ID));
								implicit.add(bundle);
							}
						}
					}
					definition.setImplicitDependencies((NameVersionDescriptor[]) implicit.toArray(new NameVersionDescriptor[implicit.size()]));
				}
			}
		}
		definition.setBundleContainers((IBundleContainer[]) bundleContainers.toArray(new IBundleContainer[bundleContainers.size()]));

		if (included35.size() > 0) {
			definition.setIncluded((NameVersionDescriptor[]) included35.toArray(new NameVersionDescriptor[included35.size()]));
		}
		if (optional35.size() > 0) {
			definition.setOptional((NameVersionDescriptor[]) optional35.toArray(new NameVersionDescriptor[optional35.size()]));
		}
		if (repositores35.size() > 0) {
			URI[] repos = definition.getRepositories();
			URI[] newRepos = new URI[repos.length + repositores35.size()];
			repositores35.toArray(newRepos);
			for (int i = 0; i < repos.length; i++) {
				newRepos[i + repositores35.size()] = repos[i];
			}
			definition.setRepositories(newRepos);
		}
	}

	private static Element serializeLocation(Document doc, IBundleContainer location) throws CoreException {
		Element locationElement = doc.createElement(LOCATION);
		if (location instanceof AbstractLocalBundleContainer) {
			locationElement.setAttribute(ATTR_LOCATION_PATH, ((AbstractLocalBundleContainer) location).getLocation(false));
		}

		if (location instanceof FeatureBundleContainer) {
			locationElement.setAttribute(ATTR_LOCATION_TYPE, TYPE_FEATURE);
			locationElement.setAttribute(ATTR_ID, ((FeatureBundleContainer) location).getFeatureId());
			String version = ((FeatureBundleContainer) location).getFeatureVersion();
			if (version != null) {
				locationElement.setAttribute(ATTR_VERSION, version);
			}
		} else if (location instanceof DirectoryBundleContainer) {
			locationElement.setAttribute(ATTR_LOCATION_TYPE, TYPE_DIRECTORY);
		} else if (location instanceof ProfileBundleContainer) {
			locationElement.setAttribute(ATTR_LOCATION_TYPE, TYPE_INSTALLATION);
			String configurationArea = ((ProfileBundleContainer) location).getConfigurationLocation();
			if (configurationArea != null) {
				locationElement.setAttribute(ATTR_CONFIGURATION, configurationArea);
			}
		} else if (location instanceof IUBundleContainer) {
			locationElement.setAttribute(ATTR_LOCATION_TYPE, TYPE_REPOSITORY);
			IUBundleContainer iubc = (IUBundleContainer) location;
			NameVersionDescriptor[] descriptions = iubc.getRootIUs();
			for (int i = 0; i < descriptions.length; i++) {
				Element unit = doc.createElement(INSTALLABLE_UNIT);
				unit.setAttribute(ATTR_ID, descriptions[i].getId());
				unit.setAttribute(ATTR_VERSION, descriptions[i].getVersion());
				locationElement.appendChild(unit);
			}
		}
		return locationElement;
	}

	private static void serializeBundles(Document doc, Element parent, NameVersionDescriptor[] units) {
		for (int i = 0; i < units.length; i++) {
			Element includedBundle = doc.createElement(PLUGIN);
			includedBundle.setAttribute(ATTR_ID, units[i].getId());
			String version = units[i].getVersion();
			if (version != null) {
				includedBundle.setAttribute(ATTR_VERSION, version);
			}
			parent.appendChild(includedBundle);
		}
	}

	private static IBundleContainer deserializeBundleContainer(Element location, List included, List optional, List repositories) throws CoreException {
		String def = location.getAttribute(ATTR_USE_DEFAULT);
		String path = null;
		String type = null;
		if (def.length() > 0) {
			// old style
			if (Boolean.valueOf(def).booleanValue()) {
				path = "${eclipse_home}"; //$NON-NLS-1$
				type = TYPE_INSTALLATION;
			}
		} else {
			path = location.getAttribute(ATTR_LOCATION_PATH);
			type = location.getAttribute(ATTR_LOCATION_TYPE);
		}

		if (path.length() == 0) {
			path = "${eclipse_home}"; //$NON-NLS-1$
		}

		if (type.length() == 0) {
			if (path.endsWith("plugins")) { //$NON-NLS-1$
				type = TYPE_DIRECTORY;
			} else {
				type = TYPE_INSTALLATION;
			}
		}

		IBundleContainer container = null;
		if (TYPE_DIRECTORY.equals(type)) {
			container = getTargetPlatformService().newDirectoryContainer(path);
		} else if (TYPE_INSTALLATION.equals(type)) {
			String configArea = location.getAttribute(ATTR_CONFIGURATION);
			container = getTargetPlatformService().newProfileContainer(path, configArea.length() > 0 ? configArea : null);
		} else if (TYPE_FEATURE.equals(type)) {
			String version = location.getAttribute(ATTR_VERSION);
			container = getTargetPlatformService().newFeatureContainer(path, location.getAttribute(ATTR_ID), version.length() > 0 ? version : null);
		} else if (TYPE_REPOSITORY.equals(type)) {
			List descriptions = new ArrayList();
			NodeList list = location.getChildNodes();
			for (int i = 0; i < list.getLength(); ++i) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element element = (Element) node;
					if (element.getNodeName().equalsIgnoreCase(INSTALLABLE_UNIT)) {
						String id = element.getAttribute(ATTR_ID);
						if (id.length() > 0) {
							String version = element.getAttribute(ATTR_VERSION);
							if (version.length() > 0) {
								NameVersionDescriptor description = new NameVersionDescriptor(id, version);
								descriptions.add(description);
							}
						}
					} else if (element.getNodeName().equalsIgnoreCase(REPOSITORY)) {
						String loc = element.getAttribute(LOCATION);
						if (loc.length() > 0) {
							try {
								repositories.add(new URI(loc));
							} catch (URISyntaxException e) {
							}
						}
					}
				}
			}

			container = new IUBundleContainer((NameVersionDescriptor[]) descriptions.toArray(new NameVersionDescriptor[descriptions.size()]));
		}

		NodeList list = location.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (element.getNodeName().equalsIgnoreCase(INCLUDE_BUNDLES)) {
					NameVersionDescriptor[] oldIncluded = deserializeBundles(element);
					for (int j = 0; j < oldIncluded.length; j++) {
						included.add(oldIncluded[j]);
					}
				} else if (element.getNodeName().equalsIgnoreCase(OPTIONAL_BUNDLES)) {
					NameVersionDescriptor[] oldOptional = deserializeBundles(element);
					for (int j = 0; j < oldOptional.length; j++) {
						optional.add(oldOptional[j]);
					}
				}
			}
		}

		return container;
	}

	private static NameVersionDescriptor[] deserializeBundles(Element parentElement) {
		NodeList nodes = parentElement.getChildNodes();
		List bundles = new ArrayList(nodes.getLength());
		for (int j = 0; j < nodes.getLength(); ++j) {
			Node include = nodes.item(j);
			if (include.getNodeType() == Node.ELEMENT_NODE) {
				Element includeElement = (Element) include;
				if (includeElement.getNodeName().equalsIgnoreCase(PLUGIN)) {
					String id = includeElement.getAttribute(ATTR_ID);
					String version = includeElement.getAttribute(ATTR_VERSION);
					NameVersionDescriptor description = version.length() > 0 ? new NameVersionDescriptor(id, version) : new NameVersionDescriptor(id);
					bundles.add(description);
				}
			}
		}
		return (NameVersionDescriptor[]) bundles.toArray(new NameVersionDescriptor[bundles.size()]);
	}

	private static URI[] deserializeRepositories(Element parentElement) {
		NodeList nodes = parentElement.getChildNodes();
		List repos = new ArrayList();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (element.getNodeName().equalsIgnoreCase(REPOSITORY)) {
					String loc = element.getAttribute(LOCATION);
					if (loc.length() > 0) {
						try {
							repos.add(new URI(loc));
						} catch (URISyntaxException e) {
						}
					}
				}
			}
		}
		return (URI[]) repos.toArray(new URI[repos.size()]);
	}

	/**
	 * Parses old content section.
	 * 
	 * @param content element containing the content section
	 * @param primaryContainer the primary location defined in the xml file, restrictions are based off this container
	 * @param useAll whether all bundles in the locations should be considered vs. only those specified
	 * @return list of bundle containers
	 */
	private static List deserializeBundleContainersFromOldStyleElement(Element content, IBundleContainer primaryContainer, boolean useAll, List included, List optional) throws CoreException {
		List containers = new ArrayList();
		NodeList list = content.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (!useAll && element.getNodeName().equalsIgnoreCase(PLUGINS)) {
					NodeList plugins = element.getChildNodes();
					for (int j = 0; j < plugins.getLength(); j++) {
						Node lNode = plugins.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element plugin = (Element) lNode;
							String id = plugin.getAttribute(ATTR_ID);
							boolean isOptional = plugin.getAttribute(ATTR_OPTIONAL).equalsIgnoreCase(Boolean.toString(true));
							if (id.length() > 0) {
								NameVersionDescriptor bundle = new NameVersionDescriptor(id);
								if (isOptional) {
									optional.add(bundle);
								} else {
									included.add(bundle);
								}
							}
						}
					}
					// Primary container is only added by default if useAllPlugins='true'
					if (included.size() > 0 || optional.size() > 0) {
						containers.add(primaryContainer);
					}
				} else if (element.getNodeName().equalsIgnoreCase(EXTRA_LOCATIONS)) {
					NodeList locations = element.getChildNodes();
					for (int j = 0; j < locations.getLength(); j++) {
						Node lNode = locations.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element location = (Element) lNode;
							String path = location.getAttribute(ATTR_LOCATION_PATH);
							if (path.length() > 0) {
								containers.add(getTargetPlatformService().newDirectoryContainer(path));
							}
						}
					}
				} else if (!useAll && element.getNodeName().equalsIgnoreCase(FEATURES)) {
					NodeList features = element.getChildNodes();
					for (int j = 0; j < features.getLength(); j++) {
						Node lNode = features.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element feature = (Element) lNode;
							String id = feature.getAttribute(ATTR_ID);
							if (id.length() > 0) {
								if (primaryContainer != null && primaryContainer instanceof AbstractLocalBundleContainer) {
									containers.add(getTargetPlatformService().newFeatureContainer(((AbstractLocalBundleContainer) primaryContainer).getLocation(false), id, null));
								}
							}
						}
					}
				}

			}
		}

		// in the old world, setting useAllPlugins to true meant don't set any exclusions
		if (useAll) {
			included.clear();
			optional.clear();
		}

		return containers;
	}

	/**
	 * Returns the value of any text nodes stored as children of the given element
	 * @param element the element to check for text content
	 * @return string containing text content of element or empty string
	 * @throws DOMException
	 */
	private static String getTextContent(Element element) throws DOMException {
		NodeList children = element.getChildNodes();
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < children.getLength(); ++i) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.TEXT_NODE) {
				result.append(currentNode.getNodeValue());
			}
		}
		return result.toString();
	}

	/**
	 * Removes any existing child content and inserts a text node with the given text
	 * @param element element to add text content to
	 * @param text text to add as value
	 * @throws DOMException
	 */
	private static void setTextContent(Element element, String text) throws DOMException {
		Node child;
		while ((child = element.getFirstChild()) != null) {
			element.removeChild(child);
		}
		if (text != null && text.length() > 0) {
			Text textNode = element.getOwnerDocument().createTextNode(text);
			element.appendChild(textNode);
		}
	}

	private static ITargetPlatformService getTargetPlatformService() throws CoreException {
		if (fTargetService == null) {
			fTargetService = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (fTargetService == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetDefinitionPersistenceHelper_1));
			}
		}
		return fTargetService;
	}

}