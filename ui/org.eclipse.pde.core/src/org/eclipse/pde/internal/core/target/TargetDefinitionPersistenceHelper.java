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

import org.eclipse.equinox.p2.metadata.Version;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
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
	private static final String INSTALLABLE_UNIT = "unit"; //$NON-NLS-1$
	private static final String REPOSITORY = "repository"; //$NON-NLS-1$
	private static final String ATTR_INCLUDE_MODE = "includeMode"; //$NON-NLS-1$
	public static final String MODE_SLICER = "slicer"; //$NON-NLS-1$
	public static final String MODE_PLANNER = "planner"; //$NON-NLS-1$
	private static final String ATTR_INCLUDE_ALL_PLATFORMS = "includeAllPlatforms"; //$NON-NLS-1$
	private static final String ATTR_OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	private static final String ATTR_CONFIGURATION = "configuration"; //$NON-NLS-1$
	private static final String CONTENT = "content"; //$NON-NLS-1$
	private static final String ATTR_USE_ALL = "useAllPlugins"; //$NON-NLS-1$
	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
	private static final String FEATURES = "features"; //$NON-NLS-1$
	private static final String EXTRA_LOCATIONS = "extraLocations"; //$NON-NLS-1$
	private static ITargetPlatformService fTargetService;

	/* Example Old Style Xml
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

	 */

	/* Example New Style XML

	<?xml version="1.0" encoding="UTF-8" standalone="no"?>
	<?pde version="3.5"?>

	<target description="A description" name="A name">
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

		ProcessingInstruction instruction = doc.createProcessingInstruction(PDE_INSTRUCTION, ATTR_VERSION + "=\"" + ICoreConstants.TARGET35 + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		doc.appendChild(instruction);

		Element rootElement = doc.createElement(ROOT);

		if (definition.getName() != null) {
			rootElement.setAttribute(ATTR_NAME, definition.getName());
		}

		IBundleContainer[] containers = definition.getBundleContainers();
		if (containers != null && containers.length > 0) {
			Element containersElement = doc.createElement(LOCATIONS);
			for (int i = 0; i < containers.length; i++) {
				Element containerElement = serializeBundleContainer(doc, (AbstractBundleContainer) containers[i]);
				containersElement.appendChild(containerElement);
			}
			rootElement.appendChild(containersElement);
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

		BundleInfo[] implicitDependencies = definition.getImplicitDependencies();
		if (implicitDependencies != null && implicitDependencies.length > 0) {
			Element implicit = doc.createElement(IMPLICIT);
			for (int i = 0; i < implicitDependencies.length; i++) {
				Element plugin = doc.createElement(PLUGIN);
				plugin.setAttribute(ATTR_ID, implicitDependencies[i].getSymbolicName());
				if (implicitDependencies[i].getVersion() != null) {
					plugin.setAttribute(ATTR_VERSION, implicitDependencies[i].getVersion());
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

		AbstractBundleContainer oldStylePrimaryContainer = null;
		List bundleContainers = new ArrayList();
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
								bundleContainers.add(deserializeBundleContainer(locationElement));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(LOCATION)) {
					// This is the 'home' location in old style target platforms
					oldStylePrimaryContainer = (AbstractBundleContainer) deserializeBundleContainer(element);
				} else if (nodeName.equalsIgnoreCase(CONTENT)) {
					// Additional locations and other bundle content settings were stored under this tag in old style target platforms
					// Only included if the content has useAllPlugins='true' otherwise we create bundle containers for the restrictions
					boolean useAll = Boolean.TRUE.toString().equalsIgnoreCase(element.getAttribute(ATTR_USE_ALL));
					if (useAll) {
						bundleContainers.add(oldStylePrimaryContainer);
					}
					bundleContainers.addAll(deserializeBundleContainersFromOldStyleElement(element, oldStylePrimaryContainer, useAll));
					// It is possible to have an empty content section, in which case we should add the primary container, bug 268709
					if (bundleContainers.isEmpty()) {
						bundleContainers.add(oldStylePrimaryContainer);
					}
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
								BundleInfo bundle = new BundleInfo(currentElement.getAttribute(ATTR_ID), version.length() > 0 ? version : null, null, BundleInfo.NO_LEVEL, false);
								implicit.add(bundle);
							}
						}
					}
					definition.setImplicitDependencies((BundleInfo[]) implicit.toArray(new BundleInfo[implicit.size()]));
				}
			}
		}
		definition.setBundleContainers((IBundleContainer[]) bundleContainers.toArray(new IBundleContainer[bundleContainers.size()]));
	}

	private static Element serializeBundleContainer(Document doc, AbstractBundleContainer container) throws CoreException {
		Element containerElement = doc.createElement(LOCATION);
		if (!(container instanceof IUBundleContainer)) {
			containerElement.setAttribute(ATTR_LOCATION_PATH, container.getLocation(false));
		}
		containerElement.setAttribute(ATTR_LOCATION_TYPE, container.getType());
		if (container instanceof FeatureBundleContainer) {
			containerElement.setAttribute(ATTR_ID, ((FeatureBundleContainer) container).getFeatureId());
			String version = ((FeatureBundleContainer) container).getFeatureVersion();
			if (version != null) {
				containerElement.setAttribute(ATTR_VERSION, version);
			}
		} else if (container instanceof ProfileBundleContainer) {
			String configurationArea = ((ProfileBundleContainer) container).getConfigurationLocation();
			if (configurationArea != null) {
				containerElement.setAttribute(ATTR_CONFIGURATION, configurationArea);
			}
		} else if (container instanceof IUBundleContainer) {
			IUBundleContainer iubc = (IUBundleContainer) container;
			containerElement.setAttribute(ATTR_INCLUDE_MODE, iubc.getIncludeAllRequired() ? MODE_PLANNER : MODE_SLICER);
			containerElement.setAttribute(ATTR_INCLUDE_ALL_PLATFORMS, Boolean.toString(iubc.getIncludeAllEnvironments()));
			String[] ids = iubc.getIds();
			Version[] versions = iubc.getVersions();
			for (int i = 0; i < ids.length; i++) {
				Element unit = doc.createElement(INSTALLABLE_UNIT);
				unit.setAttribute(ATTR_ID, ids[i]);
				unit.setAttribute(ATTR_VERSION, versions[i].toString());
				containerElement.appendChild(unit);
			}
			URI[] repositories = iubc.getRepositories();
			if (repositories != null) {
				for (int i = 0; i < repositories.length; i++) {
					Element repo = doc.createElement(REPOSITORY);
					repo.setAttribute(LOCATION, repositories[i].toASCIIString());
					containerElement.appendChild(repo);
				}
			}
		}
		BundleInfo[] includedBundles = container.getIncludedBundles();
		if (includedBundles != null) {
			Element included = doc.createElement(INCLUDE_BUNDLES);
			serializeBundles(doc, included, includedBundles);
			containerElement.appendChild(included);
		}
		BundleInfo[] optionalBundles = container.getOptionalBundles();
		if (optionalBundles != null) {
			Element optional = doc.createElement(OPTIONAL_BUNDLES);
			serializeBundles(doc, optional, optionalBundles);
			containerElement.appendChild(optional);
		}
		return containerElement;
	}

	private static void serializeBundles(Document doc, Element parent, BundleInfo[] bundles) {
		for (int j = 0; j < bundles.length; j++) {
			Element includedBundle = doc.createElement(PLUGIN);
			includedBundle.setAttribute(ATTR_ID, bundles[j].getSymbolicName());
			String version = bundles[j].getVersion();
			if (version != null) {
				includedBundle.setAttribute(ATTR_VERSION, version);
			}
			parent.appendChild(includedBundle);
		}
	}

	private static IBundleContainer deserializeBundleContainer(Element location) throws CoreException {
		String def = location.getAttribute(ATTR_USE_DEFAULT);
		String path = null;
		String type = null;
		if (def.length() > 0) {
			// old style
			if (Boolean.valueOf(def).booleanValue()) {
				path = "${eclipse_home}"; //$NON-NLS-1$
				type = ProfileBundleContainer.TYPE;
			}
		} else {
			path = location.getAttribute(ATTR_LOCATION_PATH);
			type = location.getAttribute(ATTR_LOCATION_TYPE);
		}
		if (type.length() == 0) {
			if (path.endsWith("plugins")) { //$NON-NLS-1$
				type = DirectoryBundleContainer.TYPE;
			} else {
				type = ProfileBundleContainer.TYPE;
			}
		}
		IBundleContainer container = null;
		if (DirectoryBundleContainer.TYPE.equals(type)) {
			container = getTargetPlatformService().newDirectoryContainer(path);
		} else if (ProfileBundleContainer.TYPE.equals(type)) {
			String configArea = location.getAttribute(ATTR_CONFIGURATION);
			container = getTargetPlatformService().newProfileContainer(path, configArea.length() > 0 ? configArea : null);
		} else if (FeatureBundleContainer.TYPE.equals(type)) {
			String version = location.getAttribute(ATTR_VERSION);
			container = getTargetPlatformService().newFeatureContainer(path, location.getAttribute(ATTR_ID), version.length() > 0 ? version : null);
		} else if (IUBundleContainer.TYPE.equals(type)) {
			String includeMode = location.getAttribute(ATTR_INCLUDE_MODE);
			String includeAllPlatforms = location.getAttribute(ATTR_INCLUDE_ALL_PLATFORMS);
			NodeList list = location.getChildNodes();
			List ids = new ArrayList();
			List versions = new ArrayList();
			List repos = new ArrayList();
			for (int i = 0; i < list.getLength(); ++i) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					// TODO: missing id/version
					Element element = (Element) node;
					if (element.getNodeName().equalsIgnoreCase(INSTALLABLE_UNIT)) {
						String id = element.getAttribute(ATTR_ID);
						if (id.length() > 0) {
							String version = element.getAttribute(ATTR_VERSION);
							if (version.length() > 0) {
								ids.add(id);
								versions.add(version);
							}
						}
					} else if (element.getNodeName().equalsIgnoreCase(REPOSITORY)) {
						String loc = element.getAttribute(LOCATION);
						if (loc.length() > 0) {
							try {
								repos.add(new URI(loc));
							} catch (URISyntaxException e) {
								// TODO: illegal syntax
							}
						}
					}
				}
			}
			String[] iuIDs = (String[]) ids.toArray(new String[ids.size()]);
			String[] iuVer = (String[]) versions.toArray(new String[versions.size()]);
			URI[] uris = (URI[]) repos.toArray(new URI[repos.size()]);
			container = new IUBundleContainer(iuIDs, iuVer, uris);
			if (includeMode != null && includeMode.trim().length() > 0) {
				if (includeMode.equals(MODE_PLANNER)) {
					((IUBundleContainer) container).setIncludeAllRequired(true, null);
				} else if (includeMode.equals(MODE_SLICER)) {
					((IUBundleContainer) container).setIncludeAllRequired(false, null);
				}
			}
			if (includeAllPlatforms != null && includeAllPlatforms.trim().length() > 0) {
				((IUBundleContainer) container).setIncludeAllEnvironments(Boolean.valueOf(includeAllPlatforms).booleanValue(), null);
			}

		}

		NodeList list = location.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (element.getNodeName().equalsIgnoreCase(INCLUDE_BUNDLES)) {
					BundleInfo[] included = deserializeBundles(element);
					container.setIncludedBundles(included);
				} else if (element.getNodeName().equalsIgnoreCase(OPTIONAL_BUNDLES)) {
					BundleInfo[] optional = deserializeBundles(element);
					container.setOptionalBundles(optional);
				}
			}
		}

		return container;
	}

	private static BundleInfo[] deserializeBundles(Element bundleContainer) {
		NodeList nodes = bundleContainer.getChildNodes();
		List bundles = new ArrayList(nodes.getLength());
		for (int j = 0; j < nodes.getLength(); ++j) {
			Node include = nodes.item(j);
			if (include.getNodeType() == Node.ELEMENT_NODE) {
				Element includeElement = (Element) include;
				if (includeElement.getNodeName().equalsIgnoreCase(PLUGIN)) {
					String id = includeElement.getAttribute(ATTR_ID);
					String version = includeElement.getAttribute(ATTR_VERSION);
					bundles.add(new BundleInfo(id, version.length() > 0 ? version : null, null, BundleInfo.NO_LEVEL, false));
				}
			}
		}
		return (BundleInfo[]) bundles.toArray(new BundleInfo[bundles.size()]);
	}

	/**
	 * Parses old content section.
	 * 
	 * @param content element containing the content section
	 * @param primaryContainer the primary location defined in the xml file, restrictions are based off this container
	 * @param useAll whether all bundles in the locations should be considered vs. only those specified
	 * @return list of bundle containers
	 */
	private static List deserializeBundleContainersFromOldStyleElement(Element content, AbstractBundleContainer primaryContainer, boolean useAll) throws CoreException {
		List containers = new ArrayList();
		NodeList list = content.getChildNodes();
		List included = new ArrayList(list.getLength());
		List optional = new ArrayList();
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
								BundleInfo info = new BundleInfo(id, null, null, BundleInfo.NO_LEVEL, false);
								if (isOptional) {
									optional.add(info);
								} else {
									included.add(info);
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
								if (primaryContainer != null) {
									containers.add(getTargetPlatformService().newFeatureContainer(primaryContainer.getLocation(false), id, null));
								}
							}
						}
					}
				}

			}
		}
		// in the old world, the restrictions were global to all containers
		if (!useAll && (included.size() > 0 || optional.size() > 0)) {
			Iterator iterator = containers.iterator();
			while (iterator.hasNext()) {
				IBundleContainer container = (IBundleContainer) iterator.next();
				if (!(container instanceof FeatureBundleContainer)) {
					if (included.size() > 0) {
						container.setIncludedBundles((BundleInfo[]) included.toArray(new BundleInfo[included.size()]));
					}
					if (optional.size() > 0) {
						container.setOptionalBundles((BundleInfo[]) optional.toArray(new BundleInfo[optional.size()]));
					}
				}
			}
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