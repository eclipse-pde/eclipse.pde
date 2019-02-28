/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.NameVersionDescriptor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles reading of target definition files that were created in 3.6.
 * Only significant different between 3.5 and 3.6 is that the included and optional
 * bundle restrictions are set at the target level rather than the bundle container
 * level.  In addition, features can be use on the included settings to restrict
 * by feature.
 *
 * @see TargetDefinitionPersistenceHelper
 */
public class TargetPersistence36Helper {

	/* Example of Software location in Target XML

	<?xml version="1.0" encoding="UTF-8"?><?pde version="3.6"?><target name="SoftwareSiteTarget" sequenceNumber="6">
	<locations>
	<location includeAllPlatforms="false" includeMode="slicer" includeSource="true" type="InstallableUnit">
	<unit id="org.eclipse.egit.feature.group" version="0.11.3"/>
	<unit id="org.eclipse.jgit.feature.group" version="0.11.3"/>
	<repository location="http://download.eclipse.org/releases/indigo"/>
	</location>
	</locations>
	</target>

	*/
	public static void initFromDoc(ITargetDefinition definition, Element root) throws CoreException {
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
								bundleContainers.add(deserializeBundleContainer(locationElement));
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
						for (NameVersionDescriptor nameVersionDescriptor : previousIncluded) {
							allIncluded.add(nameVersionDescriptor);
						}
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

		// Set the sequence number at the very end
		String sequenceNumber = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_SEQUENCE_NUMBER);
		try {
			((TargetDefinition) definition).setSequenceNumber(Integer.parseInt(sequenceNumber));
		} catch (NumberFormatException e) {
			((TargetDefinition) definition).setSequenceNumber(0);
		}
	}

	/**
	 * Uses the given location to create a bundle container.  If the container had included or optional bundles set, add them
	 * to the appropriate set (in 3.5 each container had included/optional, in 3.6 only the target has included/optional).  The
	 * sets may be null to indicate that no container has specified inclusion restrictions yet.
	 *
	 * @param location document element representing a bundle container
	 * @return target location instance
	 * @throws CoreException
	 */
	private static ITargetLocation deserializeBundleContainer(Element location) throws CoreException {
		String path = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH);
		String type = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE);
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
		} else if (IUBundleContainer.TYPE.equals(type)) {
			String includeMode = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_MODE);
			String includeAllPlatforms = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_ALL_PLATFORMS);
			String includeSource = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_SOURCE);
			NodeList list = location.getChildNodes();
			List<String> ids = new ArrayList<>();
			List<String> versions = new ArrayList<>();
			List<URI> repos = new ArrayList<>();
			for (int i = 0; i < list.getLength(); ++i) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					// TODO: missing id/version
					Element element = (Element) node;
					if (element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.INSTALLABLE_UNIT)) {
						String id = element.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID);
						if (id.length() > 0) {
							String version = element.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
							if (version.length() > 0) {
								ids.add(id);
								versions.add(version);
							}
						}
					} else if (element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.REPOSITORY)) {
						String loc = element.getAttribute(TargetDefinitionPersistenceHelper.LOCATION);
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
			String[] iuIDs = ids.toArray(new String[ids.size()]);
			String[] iuVer = versions.toArray(new String[versions.size()]);
			URI[] uris = repos.toArray(new URI[repos.size()]);

			int flags = IUBundleContainer.INCLUDE_REQUIRED;
			if (includeMode != null && includeMode.trim().length() > 0) {
				if (includeMode.equals(TargetDefinitionPersistenceHelper.MODE_SLICER)) {
					flags = 0;
				}
			}
			flags |= Boolean.parseBoolean(includeAllPlatforms) ? IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS : 0;
			flags |= Boolean.parseBoolean(includeSource) ? IUBundleContainer.INCLUDE_SOURCE : 0;
			container = new IUBundleContainer(iuIDs, iuVer, uris, flags);
		}
		return container;
	}

}
