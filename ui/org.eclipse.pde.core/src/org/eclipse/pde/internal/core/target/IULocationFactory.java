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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Location factory contributed through extension to org.eclipse.pde.core.targetLocations
 *
 * Provides serialization and deserialize method for InstallableUnit target location
 */
public class IULocationFactory implements ITargetLocationFactory {

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {

		Element location;
		try {
			@SuppressWarnings("restriction")
			Document document = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.parseWithErrorOnDOCTYPE(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
			location = document.getDocumentElement();
		} catch (Exception e) {
			throw new CoreException(Status.error(e.getMessage(), e));
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
			String followRepositoryReferences = location.getAttribute(TargetDefinitionPersistenceHelper.ATTR_FOLLOW_REPOSITORY_REFERENCES);

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
							ids.add(id);
							versions.add(element.getAttribute(TargetDefinitionPersistenceHelper.ATTR_VERSION));
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

			int flags = IUBundleContainer.INCLUDE_REQUIRED;
			if (includeMode != null && includeMode.trim().length() > 0) {
				if (includeMode.equals(TargetDefinitionPersistenceHelper.MODE_SLICER)) {
					flags = 0;
				}
			}
			flags |= Boolean.parseBoolean(includeAllPlatforms) ? IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS : 0;
			flags |= Boolean.parseBoolean(includeSource) ? IUBundleContainer.INCLUDE_SOURCE : 0;
			flags |= Boolean.parseBoolean(includeConfigurePhase) ? IUBundleContainer.INCLUDE_CONFIGURE_PHASE : 0;
			// For backwards compatibility, followRepositoryReferences should be
			// true when it's absent
			if (followRepositoryReferences.isEmpty()) {
				flags |= IUBundleContainer.FOLLOW_REPOSITORY_REFERENCES;
			} else {
				flags |= Boolean.parseBoolean(followRepositoryReferences) ? IUBundleContainer.FOLLOW_REPOSITORY_REFERENCES : 0;
			}
			return TargetPlatformService.getDefault().newIULocation( //
					ids.toArray(String[]::new), versions.toArray(String[]::new), repos.toArray(URI[]::new), flags);
		}
		return null;
	}
}
