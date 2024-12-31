/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles reading of target definition files that were created in 3.8.
 * Only significant different between 3.5 and 3.6 is that the included and optional
 * bundle restrictions are set at the target level rather than the bundle container
 * level.  In addition, features can be use on the included settings to restrict
 * by feature. In 3.8 the custom target locations can be contributed
 *
 * @see TargetDefinitionPersistenceHelper
 */
public class TargetPersistence38Helper {

	/* Example 3.8 target file

	<?xml version="1.0" encoding="UTF-8" standalone="no"?>
	<?pde version="3.8"?>

	<target name="test">
	<locations>
	<location path="${eclipse_home}" type="Directory"/>
	<location path="${eclipse_home}" type="Profile"/>
	<location id="org.eclipse.emf.ecore" path="${eclipse_home}" type="Feature"/>
	<location id="org.eclipse.egit" path="${eclipse_home}" type="Feature"/>
	<location includeAllPlatforms="false" includeMode="slicer" includeSource="false" type="InstallableUnit">
	<unit id="org.eclipse.releng.tools.feature.group" version="3.4.100.v20110503-45-7w31221634"/>
	<repository location="http://fullmoon.ottawa.ibm.com/updates/3.8-I-builds/"/>
	</location>
	<location includeAllPlatforms="false" includeMode="slicer" includeSource="false" type="InstallableUnit">
	<unit id="org.eclipse.sdk.ide" version="3.7.0.I20110603-0909"/>
	<repository location="http://fullmoon.ottawa.ibm.com/updates/3.7-I-builds/"/>
	</location>
	</locations>
	</target>

	*/
	public static void initFromDoc(ITargetDefinition definition, Element root) {
		String name = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_NAME);
		if (name.length() > 0) {
			definition.setName(name);
		}

		String mode = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_MODE);
		if (mode.equalsIgnoreCase(TargetDefinitionPersistenceHelper.FEATURE)) {
			((TargetDefinition) definition).setUIMode(TargetDefinition.MODE_FEATURE);
		}

		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATIONS)) {
					NodeList locations = element.getChildNodes();
					List<ITargetLocation> bundleContainers = new ArrayList<>();
					for (int j = 0; j < locations.getLength(); ++j) {
						Node locationNode = locations.item(j);
						if (locationNode.getNodeType() == Node.ELEMENT_NODE) {
							Element locationElement = (Element) locationNode;
							if (locationElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATION)) {
								try {
									ITargetLocation container = deserializeBundleContainer(locationElement);
									if (container != null) {
										bundleContainers.add(container);
									}
								} catch (CoreException e) {
									// Log the problem and move on to the next location
									PDECore.log(e);
								}
							}
						}
					}
					definition.setTargetLocations(bundleContainers.toArray(new ITargetLocation[bundleContainers.size()]));
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.INCLUDE_BUNDLES) || nodeName.equalsIgnoreCase(TargetPersistence35Helper.OPTIONAL_BUNDLES)) {
					NodeList children = element.getChildNodes();
					List<NameVersionDescriptor> included = new ArrayList<>();
					for (int j = 0; j < children.getLength(); ++j) {
						Node child = children.item(j);
						if (child.getNodeType() == Node.ELEMENT_NODE) {
							Element includeElement = (Element) child;
							if (includeElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.PLUGIN)) {
								String id = includeElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID);
								String version = includeElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
								included.add(new NameVersionDescriptor(id, version.length() > 0 ? version : null));
							} else if (includeElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.FEATURE)) {
								String id = includeElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID);
								String version = includeElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
								included.add(new NameVersionDescriptor(id, version.length() > 0 ? version : null, NameVersionDescriptor.TYPE_FEATURE));
							}
						}
					}
					// Don't overwrite includes with optional or vice versa
					NameVersionDescriptor[] previousIncluded = definition.getIncluded();
					if (previousIncluded == null || previousIncluded.length == 0) {
						definition.setIncluded(included.toArray(new NameVersionDescriptor[included.size()]));
					} else {
						List<NameVersionDescriptor> allIncluded = new ArrayList<>();
						Collections.addAll(allIncluded, previousIncluded);
						allIncluded.addAll(included);
						definition.setIncluded(allIncluded.toArray(new NameVersionDescriptor[included.size()]));
					}
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.ENVIRONMENT)) {
					NodeList envEntries = element.getChildNodes();
					for (int j = 0; j < envEntries.getLength(); ++j) {
						Node entry = envEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.OS)) {
								definition.setOS(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.WS)) {
								definition.setWS(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.ARCH)) {
								definition.setArch(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.NL)) {
								definition.setNL(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.TARGET_JRE)) {
					String text = element.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH);
					if (text.length() != 0) {
						// new format - JRE container path
						IPath path = IPath.fromPortableString(text);
						definition.setJREContainer(path);
					}
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.ARGUMENTS)) {
					NodeList argEntries = element.getChildNodes();
					for (int j = 0; j < argEntries.getLength(); ++j) {
						Node entry = argEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.PROGRAM_ARGS)) {
								definition.setProgramArguments(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
							} else if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.VM_ARGS)) {
								definition.setVMArguments(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
							}
						}
					}
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.IMPLICIT)) {
					NodeList implicitEntries = element.getChildNodes();
					List<NameVersionDescriptor> implicit = new ArrayList<>(implicitEntries.getLength());
					for (int j = 0; j < implicitEntries.getLength(); ++j) {
						Node entry = implicitEntries.item(j);
						if (entry.getNodeType() == Node.ELEMENT_NODE) {
							Element currentElement = (Element) entry;
							if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.PLUGIN)) {
								String version = currentElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
								NameVersionDescriptor bundle = new NameVersionDescriptor(currentElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID), version.length() > 0 ? version : null);
								implicit.add(bundle);
							}
						}
					}
					definition.setImplicitDependencies(implicit.toArray(new NameVersionDescriptor[implicit.size()]));
				}
			}
		}
	}

	/**
	 * Uses the given location to create a target locations.  If the container had included or optional bundles set, add them
	 * to the appropriate set (in 3.5 each container had included/optional, in 3.6 only the target has included/optional).  The
	 * sets may be null to indicate that no container has specified inclusion restrictions yet.
	 * In 3.8 the target location types can be contributed
	 *
	 * @param location document element representing a bundle container
	 * @return target location instance
	 */
	private static ITargetLocation deserializeBundleContainer(Element location) throws CoreException {
		String type = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE);
		String path = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH);

		// Type should always be specified, but if not set, guess at type
		if (type.length() == 0) {
			if (path.endsWith("plugins")) { //$NON-NLS-1$
				type = DirectoryBundleContainer.TYPE;
			} else {
				type = ProfileBundleContainer.TYPE;
			}
		}

		ITargetLocation container = null;
		switch (type)
			{
		case DirectoryBundleContainer.TYPE:
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newDirectoryLocation(path);
			break;
		case ProfileBundleContainer.TYPE:
			String configArea = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_CONFIGURATION);
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newProfileLocation(path, configArea.length() > 0 ? configArea : null);
			break;
		case FeatureBundleContainer.TYPE:
			String version = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newFeatureLocation(path, location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID), version.length() > 0 ? version : null);
			break;
		default:
			// The container is of an unknown type, should have a contribution through
			try {
				// Convert the xml to a string to pass to the extension
				ITargetLocationFactory locFactory = TargetLocationTypeManager.getInstance().getTargetLocationFactory(type);
				if (locFactory == null) {
					throw new CoreException(Status.error(NLS.bind(Messages.TargetPersistence38Helper_NoTargetLocationExtension, type)));
				}
				StreamResult result = new StreamResult(new StringWriter());
				@SuppressWarnings("restriction")
				Transformer transformer = org.eclipse.core.internal.runtime.XmlProcessorFactory
						.createTransformerFactoryWithErrorOnDOCTYPE().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
				transformer.transform(new DOMSource(location), result);
				container = locFactory.getTargetLocation(type, result.getWriter().toString());
			} catch (TransformerException | TransformerFactoryConfigurationError e) {
				throw new CoreException(Status.error(Messages.TargetDefinitionPersistenceHelper_0, e));
			}
			break;
		}
		return container;
	}

}
