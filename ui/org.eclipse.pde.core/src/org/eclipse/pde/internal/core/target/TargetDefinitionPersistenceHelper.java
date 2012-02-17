/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
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
	static final String ROOT = "target"; //$NON-NLS-1$
	static final String ATTR_NAME = "name"; //$NON-NLS-1$
	static final String LOCATIONS = "locations"; //$NON-NLS-1$
	static final String LOCATION = "location"; //$NON-NLS-1$
	static final String ATTR_LOCATION_PATH = "path"; //$NON-NLS-1$
	static final String ATTR_LOCATION_TYPE = "type"; //$NON-NLS-1$
	static final String ATTR_USE_DEFAULT = "useDefault"; //$NON-NLS-1$
	static final String INCLUDE_BUNDLES = "includeBundles"; //$NON-NLS-1$
	static final String ENVIRONMENT = "environment"; //$NON-NLS-1$
	static final String OS = "os"; //$NON-NLS-1$
	static final String WS = "ws"; //$NON-NLS-1$
	static final String ARCH = "arch"; //$NON-NLS-1$
	static final String NL = "nl"; //$NON-NLS-1$
	static final String TARGET_JRE = "targetJRE"; //$NON-NLS-1$
	static final String EXEC_ENV = "execEnv"; //$NON-NLS-1$
	static final String JRE_NAME = "jreName"; //$NON-NLS-1$
	static final String ARGUMENTS = "launcherArgs"; //$NON-NLS-1$
	static final String PROGRAM_ARGS = "programArgs"; //$NON-NLS-1$
	static final String VM_ARGS = "vmArgs"; //$NON-NLS-1$
	static final String IMPLICIT = "implicitDependencies"; //$NON-NLS-1$
	static final String PLUGIN = "plugin"; //$NON-NLS-1$
	static final String PDE_INSTRUCTION = "pde"; //$NON-NLS-1$
	static final String ATTR_ID = "id"; //$NON-NLS-1$
	static final String INSTALLABLE_UNIT = "unit"; //$NON-NLS-1$
	static final String REPOSITORY = "repository"; //$NON-NLS-1$
	static final String ATTR_INCLUDE_MODE = "includeMode"; //$NON-NLS-1$
	public static final String MODE_SLICER = "slicer"; //$NON-NLS-1$
	public static final String MODE_PLANNER = "planner"; //$NON-NLS-1$
	static final String ATTR_INCLUDE_ALL_PLATFORMS = "includeAllPlatforms"; //$NON-NLS-1$
	static final String ATTR_INCLUDE_SOURCE = "includeSource"; //$NON-NLS-1$
	static final String ATTR_INCLUDE_CONFIGURE_PHASE = "includeConfigurePhase"; //$NON-NLS-1$
	static final String ATTR_VERSION = "version"; //$NON-NLS-1$
	static final String ATTR_CONFIGURATION = "configuration"; //$NON-NLS-1$
	static final String ATTR_SEQUENCE_NUMBER = "sequenceNumber"; //$NON-NLS-1$
	static final String CONTENT = "content"; //$NON-NLS-1$
	static final String ATTR_USE_ALL = "useAllPlugins"; //$NON-NLS-1$
	static final String PLUGINS = "plugins"; //$NON-NLS-1$
	static final String FEATURES = "features"; //$NON-NLS-1$
	static final String FEATURE = "feature"; //$NON-NLS-1$
	static final String EXTRA_LOCATIONS = "extraLocations"; //$NON-NLS-1$
	private static ITargetPlatformService fTargetService;

	/**
	 * Serializes a target definition to xml and writes the xml to the given stream
	 * @param definition target definition to serialize
	 * @param output output stream to write xml to
	 * @throws CoreException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws IOException 
	 * @throws SAXException 
	 */
	public static void persistXML(ITargetDefinition definition, OutputStream output) throws CoreException, ParserConfigurationException, TransformerException, IOException, SAXException {
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();

		ProcessingInstruction instruction = doc.createProcessingInstruction(PDE_INSTRUCTION, ATTR_VERSION + "=\"" + ICoreConstants.TARGET38 + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		doc.appendChild(instruction);

		Element rootElement = doc.createElement(ROOT);

		if (definition.getName() != null) {
			rootElement.setAttribute(ATTR_NAME, definition.getName());
		}

		if (((TargetDefinition) definition).getUIMode() == TargetDefinition.MODE_FEATURE) {
			rootElement.setAttribute(ATTR_INCLUDE_MODE, FEATURE);
		}

		rootElement.setAttribute(ATTR_SEQUENCE_NUMBER, Integer.toString(((TargetDefinition) definition).getSequenceNumber()));

		ITargetLocation[] containers = definition.getTargetLocations();
		if (containers != null && containers.length > 0) {
			Element containersElement = doc.createElement(LOCATIONS);
			for (int i = 0; i < containers.length; i++) {
				Element containerElement = serializeBundleContainer(docBuilder, doc, containers[i]);
				if (containerElement != null) {
					containersElement.appendChild(containerElement);
				}
			}
			rootElement.appendChild(containersElement);
		}

		NameVersionDescriptor[] included = definition.getIncluded();
		if (included != null) {
			Element includedElement = doc.createElement(INCLUDE_BUNDLES);
			serializeBundles(doc, includedElement, included);
			rootElement.appendChild(includedElement);
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

		String version = null;
		NodeList list = doc.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
				ProcessingInstruction instruction = (ProcessingInstruction) node;
				if (instruction.getTarget() == PDE_INSTRUCTION) {
					String data = instruction.getData();
					Pattern pattern = Pattern.compile(ATTR_VERSION + "=\"(.*)\""); //$NON-NLS-1$
					Matcher matcher = pattern.matcher(data);
					if (matcher.matches()) {
						version = matcher.group(1);
						break;
					}
				}
			}
		}

		// Select the correct helper class to use
		if (version == null || version.length() == 0) {
			TargetPersistence38Helper.initFromDoc(definition, root);
		} else if (version.equals(ICoreConstants.TARGET38)) {
			TargetPersistence38Helper.initFromDoc(definition, root);
		} else if (version.equals(ICoreConstants.TARGET36)) { // it can not be 3.7
			TargetPersistence36Helper.initFromDoc(definition, root);
		} else if (version.equals(ICoreConstants.TARGET35)) {
			TargetPersistence35Helper.initFromDoc(definition, root);
		} else if (version.compareTo(ICoreConstants.TARGET34) <= 0) {
			TargetPersistence34Helper.initFromDoc(definition, root);
		}
	}

	static ITargetPlatformService getTargetPlatformService() throws CoreException {
		if (fTargetService == null) {
			fTargetService = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (fTargetService == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetDefinitionPersistenceHelper_1));
			}
		}
		return fTargetService;
	}

	/**
	 * Returns the value of any text nodes stored as children of the given element
	 * @param element the element to check for text content
	 * @return string containing text content of element or empty string
	 * @throws DOMException
	 */
	static String getTextContent(Element element) throws DOMException {
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

	private static Element serializeBundleContainer(DocumentBuilder docBuilder, Document doc, ITargetLocation targetLocation) throws CoreException, SAXException, IOException, ParserConfigurationException {
		if (targetLocation instanceof DirectoryBundleContainer) {
			Element containerElement = doc.createElement(LOCATION);
			containerElement.setAttribute(ATTR_LOCATION_TYPE, targetLocation.getType());
			containerElement.setAttribute(ATTR_LOCATION_PATH, targetLocation.getLocation(false));
			return containerElement;
		} else if (targetLocation instanceof FeatureBundleContainer) {
			Element containerElement = doc.createElement(LOCATION);
			containerElement.setAttribute(ATTR_LOCATION_TYPE, targetLocation.getType());
			containerElement.setAttribute(ATTR_LOCATION_PATH, targetLocation.getLocation(false));
			containerElement.setAttribute(ATTR_ID, ((FeatureBundleContainer) targetLocation).getFeatureId());
			String version = ((FeatureBundleContainer) targetLocation).getFeatureVersion();
			if (version != null) {
				containerElement.setAttribute(ATTR_VERSION, version);
			}
			return containerElement;
		} else if (targetLocation instanceof ProfileBundleContainer) {
			Element containerElement = doc.createElement(LOCATION);
			containerElement.setAttribute(ATTR_LOCATION_TYPE, targetLocation.getType());
			containerElement.setAttribute(ATTR_LOCATION_PATH, targetLocation.getLocation(false));
			String configurationArea = ((ProfileBundleContainer) targetLocation).getConfigurationLocation();
			if (configurationArea != null) {
				containerElement.setAttribute(ATTR_CONFIGURATION, configurationArea);
			}
			return containerElement;
		} else {
			String xml = targetLocation.serialize();
			if (xml == null)
				return null;
			Document document = docBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))); //$NON-NLS-1$
			Element root = document.getDocumentElement();
			if (!root.getNodeName().equalsIgnoreCase(LOCATION)) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetDefinitionPersistenceHelper_WrongRootElementInXML, targetLocation.getType(), xml)));
			}
			root.setAttribute(ATTR_LOCATION_TYPE, targetLocation.getType());
			return (Element) doc.importNode(root, true);
		}
	}

	private static void serializeBundles(Document doc, Element parent, NameVersionDescriptor[] bundles) {
		for (int j = 0; j < bundles.length; j++) {
			if (bundles[j].getType() == NameVersionDescriptor.TYPE_FEATURE) {
				Element includedBundle = doc.createElement(FEATURE);
				includedBundle.setAttribute(ATTR_ID, bundles[j].getId());
				String version = bundles[j].getVersion();
				if (version != null) {
					includedBundle.setAttribute(ATTR_VERSION, version);
				}
				parent.appendChild(includedBundle);
			} else {
				Element includedBundle = doc.createElement(PLUGIN);
				includedBundle.setAttribute(ATTR_ID, bundles[j].getId());
				String version = bundles[j].getVersion();
				if (version != null) {
					includedBundle.setAttribute(ATTR_VERSION, version);
				}
				parent.appendChild(includedBundle);
			}
		}
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

}