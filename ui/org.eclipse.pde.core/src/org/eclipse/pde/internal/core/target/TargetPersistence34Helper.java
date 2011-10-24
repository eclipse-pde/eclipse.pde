/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.util.VMUtil;
import org.w3c.dom.*;

/**
 * Handles reading of target definition files that were created before the new target platform
 * story (3.4 and earlier).
 * 
 * @see TargetDefinitionPersistenceHelper
 */
public class TargetPersistence34Helper {

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
	public static void initFromDoc(ITargetDefinition definition, Element root) throws CoreException {

		String name = root.getAttribute(TargetDefinitionPersistenceHelper.ATTR_NAME);
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
				if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.LOCATION)) {
					// This is the 'home' location in old style target platforms
					oldStylePrimaryContainer = (AbstractBundleContainer) deserializeBundleContainer(element);
				} else if (nodeName.equalsIgnoreCase(TargetDefinitionPersistenceHelper.CONTENT)) {
					// Additional locations and other bundle content settings were stored under this tag in old style target platforms
					// Only included if the content has useAllPlugins='true' otherwise we create bundle containers for the restrictions
					boolean useAll = Boolean.TRUE.toString().equalsIgnoreCase(element.getAttribute(TargetDefinitionPersistenceHelper.ATTR_USE_ALL));
					if (useAll) {
						bundleContainers.add(oldStylePrimaryContainer);
					}
					bundleContainers.addAll(deserializeBundleContainersFromOldStyleElement(element, definition, oldStylePrimaryContainer, useAll));
					// It is possible to have an empty content section, in which case we should add the primary container, bug 268709
					if (bundleContainers.isEmpty()) {
						bundleContainers.add(oldStylePrimaryContainer);
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
					if (text.length() == 0) {
						// old format (or missing)
						NodeList argEntries = element.getChildNodes();
						for (int j = 0; j < argEntries.getLength(); ++j) {
							Node entry = argEntries.item(j);
							if (entry.getNodeType() == Node.ELEMENT_NODE) {
								Element currentElement = (Element) entry;
								IPath path = null;
								if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.EXEC_ENV)) {
									IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(TargetDefinitionPersistenceHelper.getTextContent(currentElement));
									if (env != null) {
										path = JavaRuntime.newJREContainerPath(env);
									}
								} else if (currentElement.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.JRE_NAME)) {
									String vmName = TargetDefinitionPersistenceHelper.getTextContent(currentElement);
									IVMInstall vmInstall = VMUtil.getVMInstall(vmName);
									if (vmInstall != null) {
										path = JavaRuntime.newJREContainerPath(vmInstall);
									}
								}
								definition.setJREContainer(path);
							}
						}
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
		definition.setTargetLocations((ITargetLocation[]) bundleContainers.toArray(new ITargetLocation[bundleContainers.size()]));
	}

	private static ITargetLocation deserializeBundleContainer(Element location) throws CoreException {
		String def = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_USE_DEFAULT);
		String path = null;
		String type = null;
		if (def.length() > 0 && Boolean.valueOf(def).booleanValue()) {
			path = "${eclipse_home}"; //$NON-NLS-1$
			type = ProfileBundleContainer.TYPE;
		} else {
			path = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH);
		}

		if (type == null) {
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
		}

		return container;
	}

	/**
	 * Parses old content section.
	 * 
	 * @param content element containing the content section
	 * @param primaryContainer the primary location defined in the xml file, restrictions are based off this container
	 * @param useAll whether all bundles in the locations should be considered vs. only those specified
	 * @return list of bundle containers
	 */
	private static List deserializeBundleContainersFromOldStyleElement(Element content, ITargetDefinition definition, AbstractBundleContainer primaryContainer, boolean useAll) throws CoreException {
		List containers = new ArrayList();
		NodeList list = content.getChildNodes();
		List included = new ArrayList(list.getLength());
		for (int i = 0; i < list.getLength(); ++i) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (!useAll && element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.PLUGINS)) {
					NodeList plugins = element.getChildNodes();
					for (int j = 0; j < plugins.getLength(); j++) {
						Node lNode = plugins.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element plugin = (Element) lNode;
							String id = plugin.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID);
							if (id.length() > 0) {
								NameVersionDescriptor info = new NameVersionDescriptor(id, null);
								included.add(info);
							}
						}
					}
					// Primary container is only added by default if useAllPlugins='true'
					if (included.size() > 0) {
						containers.add(primaryContainer);
					}
				} else if (element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.EXTRA_LOCATIONS)) {
					NodeList locations = element.getChildNodes();
					for (int j = 0; j < locations.getLength(); j++) {
						Node lNode = locations.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element location = (Element) lNode;
							String path = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_PATH);
							if (path.length() > 0) {
								containers.add(TargetDefinitionPersistenceHelper.getTargetPlatformService().newDirectoryLocation(path));
							}
						}
					}
				} else if (!useAll && element.getNodeName().equalsIgnoreCase(TargetDefinitionPersistenceHelper.FEATURES)) {
					NodeList features = element.getChildNodes();
					for (int j = 0; j < features.getLength(); j++) {
						Node lNode = features.item(j);
						if (lNode.getNodeType() == Node.ELEMENT_NODE) {
							Element feature = (Element) lNode;
							String id = feature.getAttribute(TargetDefinitionPersistenceHelper.ATTR_ID);
							if (id.length() > 0) {
								if (primaryContainer != null) {
									containers.add(TargetDefinitionPersistenceHelper.getTargetPlatformService().newFeatureLocation(primaryContainer.getLocation(false), id, null));
								}
							}
						}
					}
				}

			}
		}
		// restrictions are global to all containers
		if (!useAll && included.size() > 0) {
			if (included.size() > 0) {
				definition.setIncluded((NameVersionDescriptor[]) included.toArray(new NameVersionDescriptor[included.size()]));
			}
		}
		return containers;
	}

}
