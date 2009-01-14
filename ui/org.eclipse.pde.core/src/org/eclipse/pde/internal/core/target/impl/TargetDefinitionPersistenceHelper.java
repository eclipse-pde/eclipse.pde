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
package org.eclipse.pde.internal.core.target.impl;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
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
	private static final String RESTRICTION = "restriction"; //$NON-NLS-1$
	// TODO Add type support to the locations
//	private static final String ATTR_LOCATION_TYPE = "type"; //$NON-NLS-1$
	private static final String ENVIRONMENT = "environment"; //$NON-NLS-1$
	private static final String OS = "os"; //$NON-NLS-1$
	private static final String WS = "ws"; //$NON-NLS-1$
	private static final String ARCH = "arch"; //$NON-NLS-1$
	private static final String NL = "nl"; //$NON-NLS-1$
	private static final String ARGUMENTS = "launcherArgs"; //$NON-NLS-1$
	private static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	private static final String VM_ARGS = "vmArgs"; //$NON-NLS-1$
//	private static final String IMPLICIT = "implicitDependencies"; //$NON-NLS-1$
//	private static final String PLUGIN = "plugin"; //$NON-NLS-1$
//	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String PDE_INSTRUCTION = "pde"; //$NON-NLS-1$
	private static final String ATTR_VERSION = "version="; //$NON-NLS-1$
	private static final String VERSION_3_5 = "3.5"; //$NON-NLS-1$
//	private static final String VERSION_3_2 = "3.2"; //$NON-NLS-1$
	private static final String CONTENT = "content"; //$NON-NLS-1$
//	private static final String PLUGINS = "plugins"; //$NON-NLS-1$
//	private static final String FEATURES = "features"; //$NON-NLS-1$
//	private static final String EXTRA_LOCATIONS = "extraLocations"; //$NON-NLS-1$

	/*
	 * 
	 * Example old style xml
	  
	<targetJRE>
	  <execEnv>CDC-1.0/Foundation-1.0</execEnv>
	</targetJRE>

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

		ProcessingInstruction instruction = doc.createProcessingInstruction(PDE_INSTRUCTION, ATTR_VERSION + "\"" + VERSION_3_5 + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		doc.appendChild(instruction);

		Element rootElement = doc.createElement(ROOT);

		if (definition.getName() != null) {
			rootElement.setAttribute(ATTR_NAME, definition.getName());
		}

		IBundleContainer[] containers = definition.getBundleContainers();
		if (containers != null && containers.length > 0) {
			Element containersElement = doc.createElement(LOCATIONS);
			for (int i = 0; i < containers.length; i++) {
				Element containerElement = doc.createElement(LOCATION);
				containerElement.setAttribute(ATTR_LOCATION_PATH, containers[i].getHomeLocation());
				BundleInfo[] restrictions = containers[i].getRestrictions();
				for (int j = 0; j < restrictions.length; j++) {
					Element restrictionElement = doc.createElement(RESTRICTION);
					// TODO What do we want to store, URI? Symbolic Name?
					restrictionElement.setTextContent(restrictions[j].getBaseLocation().toString());
					containerElement.appendChild(restrictionElement);
				}
				containersElement.appendChild(containerElement);
			}
			rootElement.appendChild(containersElement);
		}

		if (definition.getOS() != null || definition.getWS() != null || definition.getArch() != null || definition.getNL() != null) {
			Element envElement = doc.createElement(ENVIRONMENT);
			if (definition.getOS() != null) {
				Element element = doc.createElement(OS);
				element.setTextContent(definition.getOS());
				envElement.appendChild(element);
			}
			if (definition.getWS() != null) {
				Element element = doc.createElement(WS);
				element.setTextContent(definition.getWS());
				envElement.appendChild(element);
			}
			if (definition.getArch() != null) {
				Element element = doc.createElement(ARCH);
				element.setTextContent(definition.getArch());
				envElement.appendChild(element);
			}
			if (definition.getNL() != null) {
				Element element = doc.createElement(NL);
				element.setTextContent(definition.getNL());
				envElement.appendChild(element);
			}
			rootElement.appendChild(envElement);
		}

		if (definition.getVMArguments() != null || definition.getProgramArguments() != null) {
			Element argElement = doc.createElement(ARGUMENTS);
			if (definition.getVMArguments() != null) {
				Element element = doc.createElement(VM_ARGS);
				element.setTextContent(definition.getVMArguments());
				argElement.appendChild(element);
			}
			if (definition.getProgramArguments() != null) {
				Element element = doc.createElement(PROGRAM_ARGS);
				element.setTextContent(definition.getProgramArguments());
				argElement.appendChild(element);
			}
			rootElement.appendChild(argElement);
		}

		// TODO EE/JRE

		// TODO Implicit Plug-ins

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
	protected static void initFromXML(ITargetDefinition definition, InputStream input) throws CoreException, ParserConfigurationException, SAXException, IOException {
		DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		parser.setErrorHandler(new DefaultHandler());
		Document doc = parser.parse(new InputSource(input));

		String version = null;

//		TODO We may not need the version to process as there are no incompatable differences.
//		NodeList docNodes = doc.getChildNodes();
//		for (int i = 0; i < docNodes.getLength(); ++i) {
//			Node node = docNodes.item(i);
//			if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
//				ProcessingInstruction instruction = (ProcessingInstruction) node;
//				if (PDE_INSTRUCTION.equalsIgnoreCase(instruction.getTarget())) {
//					String data = instruction.getData();
//					if (data.)
//					oldVersion = version.equalsIgnoreCase(ATTR_VERSION.concat(VERSION_3_2));
//					break;
//				}
//			}
//		}

		Element root = doc.getDocumentElement();
		if (!root.getNodeName().equalsIgnoreCase(ROOT)) {
			// TODO Throw a proper core exception;
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "The target file is in an invalid format and could not be opened."));
		}

		String name = root.getAttribute(ATTR_NAME);
		if (name != null) {
			definition.setName(name);
		}

		NodeList list = root.getChildNodes();
		Node node = null;
		Element element = null;
		String nodeName = null;
		for (int i = 0; i < list.getLength(); ++i) {
			node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				element = (Element) node;
				nodeName = element.getNodeName();
				if (nodeName.equalsIgnoreCase(LOCATIONS)) {
					NodeList locations = element.getChildNodes();
					for (int j = 0; j < locations.getLength(); ++j) {
						Node locationNode = locations.item(j);
						if (locationNode.getNodeType() == Node.ELEMENT_NODE) {
							Element locationElement = (Element) locationNode;
							if (locationElement.getNodeName().equalsIgnoreCase(LOCATION)) {
								createBundleContainer(locationElement, definition);
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(LOCATION)) {
					createBundleContainer(element, definition);
				} else if (nodeName.equalsIgnoreCase(CONTENT)) {
					handleOldStyleContent(element, definition);
				} else if (nodeName.equalsIgnoreCase(ENVIRONMENT)) {
					NodeList envEntries = element.getChildNodes();
					for (int j = 0; j < envEntries.getLength(); ++j) {
						Node entry = envEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(OS)) {
								definition.setOS(currentElement.getTextContent());
							} else if (currentElement.getNodeName().equalsIgnoreCase(WS)) {
								definition.setWS(currentElement.getTextContent());
							} else if (currentElement.getNodeName().equalsIgnoreCase(ARCH)) {
								definition.setArch(currentElement.getTextContent());
							} else if (currentElement.getNodeName().equalsIgnoreCase(NL)) {
								definition.setNL(currentElement.getTextContent());
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
								definition.setProgramArguments(currentElement.getTextContent());
							} else if (currentElement.getNodeName().equalsIgnoreCase(VM_ARGS)) {
								definition.setVMArguments(currentElement.getTextContent());
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
								definition.setProgramArguments(currentElement.getTextContent());
							} else if (currentElement.getNodeName().equalsIgnoreCase(VM_ARGS)) {
								definition.setVMArguments(currentElement.getTextContent());
							}
						}
					}

					// TODO EE/JRE (old and new versions)
					// TODO Implicit Dependencies
				}
			}

		}
	}

	private static void handleOldStyleContent(Element element, ITargetDefinition definition) {
		// TODO Auto-generated method stub
	}

	private static void createBundleContainer(Element location, ITargetDefinition definition) {
		String path = location.getAttribute(ATTR_LOCATION_PATH);
		if (path != null) {
			// TODO Requires knowledge of the container type to recreate it
//			IBundleContainer container = .newProfileContainer(TargetPlatform.getDefaultLocation());
			// TODO Handle restriction creation
		}
	}

}