/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.*;

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
	
	<target name="test" sequenceNumber="9">
	<locations>
	<location path="${eclipse_home}" type="Directory"/>
	<location path="${eclipse_home}" type="Profile"/>
	<location id="org.eclipse.cvs" path="${eclipse_home}" type="Feature"/>
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
					List bundleContainers = new ArrayList();
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
					definition.setTargetLocations((ITargetLocation[]) bundleContainers.toArray(new ITargetLocation[bundleContainers.size()]));
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.INCLUDE_BUNDLES) || nodeName.equalsIgnoreCase(TargetPersistence35Helper.OPTIONAL_BUNDLES)) {
					NodeList children = element.getChildNodes();
					List included = new ArrayList();
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
						definition.setIncluded((NameVersionDescriptor[]) included.toArray(new NameVersionDescriptor[included.size()]));
					} else {
						List allIncluded = new ArrayList();
						for (int j = 0; j < previousIncluded.length; j++) {
							allIncluded.add(previousIncluded[j]);
						}
						allIncluded.addAll(included);
						definition.setIncluded((NameVersionDescriptor[]) allIncluded.toArray(new NameVersionDescriptor[included.size()]));
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
						IPath path = Path.fromPortableString(text);
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
					List implicit = new ArrayList(implicitEntries.getLength());
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
					definition.setImplicitDependencies((NameVersionDescriptor[]) implicit.toArray(new NameVersionDescriptor[implicit.size()]));
				}
			}
		}

		// Set the sequence number at the very end
		String sequenceNumber = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_SEQUENCE_NUMBER);
		try {
			((TargetDefinition) definition).setSequenceNumber(Integer.parseInt(sequenceNumber));
		} catch (NumberFormatException e) {
			((TargetDefinition) definition).setSequenceNumber(0);
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
	 * @throws CoreException
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
		if (DirectoryBundleContainer.TYPE.equals(type)) {
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newDirectoryLocation(path);
		} else if (ProfileBundleContainer.TYPE.equals(type)) {
			String configArea = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_CONFIGURATION);
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newProfileLocation(path, configArea.length() > 0 ? configArea : null);
		} else if (FeatureBundleContainer.TYPE.equals(type)) {
			String version = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newFeatureLocation(path, location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID), version.length() > 0 ? version : null);
		} else {
			// The container is of an unknown type, should have a contribution through
			try {
				// Convert the xml to a string to pass to the extension
				ITargetLocationFactory locFactory = TargetLocationTypeManager.getInstance().getTargetLocationFactory(type);
				if (locFactory == null) {
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetPersistence38Helper_NoTargetLocationExtension, type)));
				}
				StreamResult result = new StreamResult(new StringWriter());
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
				transformer.transform(new DOMSource(location), result);
				container = locFactory.getTargetLocation(type, result.getWriter().toString());
			} catch (TransformerConfigurationException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetDefinitionPersistenceHelper_0, e));
			} catch (TransformerException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetDefinitionPersistenceHelper_0, e));
			} catch (TransformerFactoryConfigurationError e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.TargetDefinitionPersistenceHelper_0, e));
			}
		}
		return container;
	}

}
