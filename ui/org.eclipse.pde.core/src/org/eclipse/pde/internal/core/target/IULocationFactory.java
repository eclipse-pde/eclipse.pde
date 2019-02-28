/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Location factory contributed through extension to org.eclipse.pde.core.targetLocations
 *
 * Provides serialization and deserialize method for InstallableUnit target location
 *
 */
public class IULocationFactory implements ITargetLocationFactory {

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {

		Element location;
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder
					.parse(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
			location = document.getDocumentElement();
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, e.getMessage(), e));
		}

		if (IUBundleContainer.TYPE.equals(type) && location != null) {
			String locationType = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE);
			if (!type.equals(locationType)) {
				return null;
			}

			String includeMode = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_MODE);
			String includeAllPlatforms = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_ALL_PLATFORMS);
			String includeSource = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_SOURCE);
			String includeConfigurePhase = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_INCLUDE_CONFIGURE_PHASE);

			NodeList list = location.getChildNodes();
			List<String> ids = new ArrayList<>();
			List<String> versions = new ArrayList<>();
			List<URI> repos = new ArrayList<>();
			for (int i = 0; i < list.getLength(); ++i) {
				Node node = list.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
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
			flags |= Boolean.parseBoolean(includeConfigurePhase) ? IUBundleContainer.INCLUDE_CONFIGURE_PHASE : 0;
			IUBundleContainer targetLocation = new IUBundleContainer(iuIDs, iuVer, uris, flags);
			return targetLocation;
		}
		return null;
	}
}
