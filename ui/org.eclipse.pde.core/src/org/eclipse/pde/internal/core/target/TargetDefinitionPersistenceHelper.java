/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 541188
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
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
	 * Serializes a target definition to xml and writes the xml to the given
	 * stream
	 *
	 * @param definition
	 *            target definition to serialize
	 * @param output
	 *            output stream to write xml to
	 * @throws CoreException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void persistXML(ITargetDefinition definition, OutputStream output)
			throws CoreException, ParserConfigurationException, TransformerException, IOException, SAXException {
		Document document = definition.getDocument();
		NodeList childNodes = document.getChildNodes();
		int length = childNodes.getLength();
		if (length == 0) {
			return;
		}
		StreamResult outputTarget = new StreamResult(output);
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.STANDALONE, "no"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); //$NON-NLS-1$
		transformer.transform(new DOMSource(childNodes.item(0)), outputTarget);
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "no"); //$NON-NLS-1$
		for (int i = 1; i < length; i++) {
			transformer.transform(new DOMSource(childNodes.item(i)), outputTarget);
		}
	}

	/**
	 * Parses an xml document from the input stream and deserializes it into a
	 * target definition.
	 *
	 * @param definition
	 *            definition to be filled with the result of deserialization
	 * @param input
	 *            stream to get xml input from
	 * @throws CoreException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public static void initFromXML(ITargetDefinition definition, InputStream input)
			throws CoreException, ParserConfigurationException, SAXException, IOException {
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
		// Avoid doing the rebuilding of each part of the document, just set the
		// document at the end
		definition.setDocument(null);
		// Select the correct helper class to use
		// Note: If file structure is updated, make sure to update both default cases
		if (version == null || version.length() == 0) {
			// No version, default to latest
			TargetPersistence38Helper.initFromDoc(definition, root);
		} else if (version.equals(ICoreConstants.TARGET38)) {
			TargetPersistence38Helper.initFromDoc(definition, root);
		} else if (version.equals(ICoreConstants.TARGET36)) { // it can not be 3.7
			TargetPersistence36Helper.initFromDoc(definition, root);
		} else if (version.equals(ICoreConstants.TARGET35)) {
			TargetPersistence35Helper.initFromDoc(definition, root);
		} else if (version.compareTo(ICoreConstants.TARGET34) <= 0) {
			TargetPersistence34Helper.initFromDoc(definition, root);
		} else {
			// Version doesn't match any known file structure, default to latest
			String name = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_NAME);
			PDECore.log(new Status(IStatus.WARNING, PDECore.PLUGIN_ID, MessageFormat.format(Messages.TargetDefinitionPersistenceHelper_2, version, name)));
			TargetPersistence38Helper.initFromDoc(definition, root);
		}
		definition.setDocument(doc);
	}

	static ITargetPlatformService getTargetPlatformService() throws CoreException {
		if (fTargetService == null) {
			fTargetService = PDECore.getDefault().acquireService(ITargetPlatformService.class);
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
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < children.getLength(); ++i) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.TEXT_NODE) {
				result.append(currentNode.getNodeValue());
			}
		}
		return result.toString();
	}
}
