/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

import java.util.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.*;

public class PDESchemaHelper {

	/**
	 * Returns valid attributes given a schema attribute
	 * 
	 * @param attribute 
	 * 		a schema identifier attribute (e.g., org.eclipse.ui.perspectives/perspective/@id)
	 * @return A map with the ids as keys and respective {@link IConfigurationElement} as pairs
	 */
	public static Map getValidAttributes(ISchemaAttribute attribute) {
		Map attributeMap = new HashMap();

		// TODO support multiple 

		gatherAttributes(attributeMap, attribute.getBasedOn());

		// this adds the restrictions on top of the referenced identifiers
		ISchemaRestriction restriction = attribute.getType().getRestriction();
		if (restriction != null) {
			Object[] children = restriction.getChildren();
			for (int i = 0; i < children.length; i++) {
				Object child = children[i];
				if (child instanceof ISchemaEnumeration) {
					ISchemaEnumeration enumeration = (ISchemaEnumeration) child;
					String value = enumeration.getName();
					if (value != null && value.length() > 0)
						attributeMap.put(value, null);
				}
			}
		}

		return attributeMap;
	}

	/**
	 * 
	 * Returns a reference identifier given a schema attribute
	 * 
	 * @param attribute 
	 * 		a schema attribute 
	 * @return a reference identifier (e.g., org.eclipse.ui.perspectives/perspective/@id)
	 */
	public static String getReferenceIdentifier(ISchemaAttribute attribute) {
		String rootId = attribute.getSchema().getSchemaDescriptor().getPointId();
		String refId = buildBasedOnValue(attribute.getParent()) + "/@" + attribute.getName(); //$NON-NLS-1$
		return rootId + refId;
	}

	// TODO can we do this any faster?
	private static void gatherAttributes(Map attributesInfo, String basedOn) {
		if (basedOn == null) // check for null
			return;
		String[] path = basedOn.split("/"); //$NON-NLS-1$
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions(path[0], true);

		List members = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			// handle the core style identifier case
			if (path.length == 2) {
				attributesInfo.put(extensions[i].getUniqueIdentifier(), null);
				continue;
			}

			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals(path[1])) {
					members.add(elements[j]);
				}
			}
		}
		List parents = members;
		for (int i = 2; i < path.length; i++) {
			if (path[i].startsWith("@")) { //$NON-NLS-1$
				String attName = path[i].substring(1);
				for (Iterator iterator = parents.iterator(); iterator.hasNext();) {
					IConfigurationElement element = (IConfigurationElement) iterator.next();
					String value = element.getAttribute(attName);
					if (value != null) {
						// see bug 248248 for why we have this contentTypes check
						String extpt = element.getDeclaringExtension().getExtensionPointUniqueIdentifier();
						if (value.indexOf('.') == -1 && extpt.equalsIgnoreCase("org.eclipse.core.contenttype.contentTypes")) { //$NON-NLS-1$
							attributesInfo.put(element.getNamespaceIdentifier() + '.' + value, element);
						}
						attributesInfo.put(value, element);
					}
				}
				return;
			}
			members = new ArrayList();
			for (Iterator iterator = parents.iterator(); iterator.hasNext();) {
				IConfigurationElement element = (IConfigurationElement) iterator.next();
				members.addAll(keepGoing(element, path[i]));
			}
			parents = members;
		}
	}

	private static List keepGoing(IConfigurationElement element, String tag) {
		return Arrays.asList(element.getChildren(tag));
	}

	private static String buildBasedOnValue(ISchemaObject object) {
		if (object instanceof ISchemaElement && !(object instanceof ISchemaRootElement)) {
			ISchemaElement element = (ISchemaElement) object;
			ISchema schema = element.getSchema();
			ISchemaElement[] elements = schema.getElements();
			for (int i = 0; i < elements.length; i++) {
				ISchemaElement[] children = schema.getCandidateChildren(elements[i]);
				for (int j = 0; j < children.length; j++) {
					if (object.getName().equals(children[j].getName())) {
						return buildBasedOnValue(elements[i]) + '/' + object.getName();
					}
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

}
