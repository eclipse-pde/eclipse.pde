/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.eclipse.pde.internal.core.PDECore;
import org.w3c.dom.*;

/**
 * Location factory contributed through extension to org.eclipse.pde.core.targetLocations
 * 
 * Provides serialization and deserialize method for InstallableUnit target location
 * 
 */
public class IULocationFactory implements ITargetLocationFactory {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetLocationFactory#getTargetLocation(java.lang.String, java.lang.String)
	 */
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {

		Element location;
		try {
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = docBuilder.parse(new ByteArrayInputStream(serializedXML.getBytes("UTF-8"))); //$NON-NLS-1$
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
			List ids = new ArrayList();
			List versions = new ArrayList();
			List repos = new ArrayList();
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
			String[] iuIDs = (String[]) ids.toArray(new String[ids.size()]);
			String[] iuVer = (String[]) versions.toArray(new String[versions.size()]);
			URI[] uris = (URI[]) repos.toArray(new URI[repos.size()]);

			int flags = IUBundleContainer.INCLUDE_REQUIRED;
			if (includeMode != null && includeMode.trim().length() > 0) {
				if (includeMode.equals(TargetDefinitionPersistenceHelper.MODE_SLICER)) {
					flags = 0;
				}
			}
			flags |= Boolean.valueOf(includeAllPlatforms).booleanValue() ? IUBundleContainer.INCLUDE_ALL_ENVIRONMENTS : 0;
			flags |= Boolean.valueOf(includeSource).booleanValue() ? IUBundleContainer.INCLUDE_SOURCE : 0;
			flags |= Boolean.valueOf(includeConfigurePhase).booleanValue() ? IUBundleContainer.INCLUDE_CONFIGURE_PHASE : 0;
			IUBundleContainer targetLocation = new IUBundleContainer(iuIDs, iuVer, uris, flags);
			return targetLocation;
		}
		return null;
	}
}
