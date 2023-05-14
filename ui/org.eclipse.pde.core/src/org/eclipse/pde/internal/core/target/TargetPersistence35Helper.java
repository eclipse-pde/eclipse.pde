/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
 * Handles reading of target definition files that were created in 3.5 when the target
 * platform story changed.
 * <p>
 * There are significant changes to the structure of the file from 3.4 and earlier versions.
 * The same xml element names are used, but the target is organized into bundle containers
 * (locations).
 * </p>
 * @see TargetDefinitionPersistenceHelper
 */
public class TargetPersistence35Helper {

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
	 * Support for optional bundles was removed in 3.8, optional bundles should be treated like included bundles
	 */
	static final String OPTIONAL_BUNDLES = "optionalBundles"; //$NON-NLS-1$

	public static void initFromDoc(ITargetDefinition definition, Element root) throws CoreException {
		String name = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_NAME);
		if (name.length() > 0) {
			definition.setName(name);
		}

		NodeList list = root.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				String nodeName = element.getNodeName();
				if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATIONS)) {
					NodeList locations = element.getChildNodes();
					for (int j = 0; j < locations.getLength(); ++j) {
						Node locationNode = locations.item(j);
						if (locationNode.getNodeType() == Node.ELEMENT_NODE) {
							Element locationElement = (Element) locationNode;
							if (locationElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATION)) {
								deserializeBundleContainer(definition, locationElement);
							}
						}
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
	}

	/**
	 * Uses the given location to create a bundle container.  If the container had included or optional bundles set, add them
	 * to the appropriate set (in 3.5 each container had included/optional, in 3.6 only the target has included/optional).  The
	 * sets may be null to indicate that no container has specified inclusion restrictions yet.
	 *
	 * @param definition the target definition to deserialize
	 * @param location document element representing a bundle container
	 * @throws CoreException
	 */
	private static void deserializeBundleContainer(ITargetDefinition definition, Element location) throws CoreException {
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
		switch (type) {
		case DirectoryBundleContainer.TYPE:
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newDirectoryLocation(path);
			break;
		case ProfileBundleContainer.TYPE:
			String configArea = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_CONFIGURATION);
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newProfileLocation(path, configArea.length() > 0 ? configArea : null);
			break;
		case FeatureBundleContainer.TYPE:
			String featureversion = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
			container = TargetDefinitionPersistenceHelper.getTargetPlatformService().newFeatureLocation(path,
					location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID),
					featureversion.length() > 0 ? featureversion : null);
			break;
		case IUBundleContainer.TYPE:
			String includeMode = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_MODE);
			String includeAllPlatforms = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_ALL_PLATFORMS);
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
			container = new IUBundleContainer(iuIDs, iuVer, uris, flags);
			break;
		default:
			break;
		}

		List<NameVersionDescriptor> includedBundles = null;
		NodeList list = location.getChildNodes();
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.INCLUDE_BUNDLES) || element.getNodeName().equalsIgnoreCase(OPTIONAL_BUNDLES)) {
					if (includedBundles == null) {
						includedBundles = new ArrayList<>();
					}
					includedBundles.addAll(deserializeBundles(element));
				}
			}
		}
		definition.setIncluded(includedBundles == null ? null : (NameVersionDescriptor[]) includedBundles.toArray(new NameVersionDescriptor[includedBundles.size()]));

		ITargetLocation[] currentContainers = definition.getTargetLocations();
		if (currentContainers == null || currentContainers.length == 0) {
			definition.setTargetLocations(new ITargetLocation[] {container});
		} else {
			ITargetLocation[] newContainers = new ITargetLocation[currentContainers.length + 1];
			System.arraycopy(currentContainers, 0, newContainers, 0, currentContainers.length);
			newContainers[currentContainers.length] = container;
			definition.setTargetLocations(newContainers);
		}
	}

	private static List/*NameVersionDescriptor*/<NameVersionDescriptor> deserializeBundles(Element bundleContainer) {
		NodeList nodes = bundleContainer.getChildNodes();
		List<NameVersionDescriptor> bundles = new ArrayList<>(nodes.getLength());
		for (int j = 0; j < nodes.getLength(); ++j) {
			Node include = nodes.item(j);
			if (include.getNodeType() == Node.ELEMENT_NODE) {
				Element includeElement = (Element) include;
				if (includeElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.PLUGIN)) {
					String id = includeElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID);
					String version = includeElement.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION);
					bundles.add(new NameVersionDescriptor(id, version.length() > 0 ? version : null));
				}
			}
		}
		return bundles;
	}

}
