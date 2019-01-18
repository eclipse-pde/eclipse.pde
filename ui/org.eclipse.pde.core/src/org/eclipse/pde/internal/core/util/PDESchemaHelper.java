/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

package org.eclipse.pde.internal.core.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaEnumeration;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaRootElement;

public class PDESchemaHelper {

	/**
	 * Returns valid attributes given a schema attribute
	 *
	 * @param attribute
	 * 		a schema identifier attribute (e.g., org.eclipse.ui.perspectives/perspective/@id)
	 * @return A map with the ids as keys and respective {@link IConfigurationElement} as pairs
	 */
	public static Map<String, IConfigurationElement> getValidAttributes(ISchemaAttribute attribute) {
		Map<String, IConfigurationElement> attributeMap = new HashMap<>();

                // support multiple separated using comma
                String basedOnValue = attribute.getBasedOn();
                if (basedOnValue != null) {
                        String[] basedOnList = basedOnValue.split(","); //$NON-NLS-1$
                        for (String basedOn : basedOnList) {
                                gatherAttributes(attributeMap, basedOn);
                        }
                }

		// this adds the restrictions on top of the referenced identifiers
		ISchemaRestriction restriction = attribute.getType().getRestriction();
		if (restriction != null) {
			Object[] children = restriction.getChildren();
			for (Object child : children) {
				if (child instanceof ISchemaEnumeration) {
					ISchemaEnumeration enumeration = (ISchemaEnumeration) child;
					String value = enumeration.getName();
					if (value != null && value.length() > 0) {
						attributeMap.put(value, null);
					}
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
	private static void gatherAttributes(Map<String, IConfigurationElement> attributesInfo, String basedOn) {
		if (basedOn == null) {
			return;
		}
		String[] path = basedOn.split("/"); //$NON-NLS-1$
		if (path.length < 2) {
			return; // No plug-in identifier
		}
		IExtension[] extensions = PDECore.getDefault().getExtensionsRegistry().findExtensions(path[0], true);

		List<IConfigurationElement> members = new ArrayList<>();
		for (IExtension extension : extensions) {
			// handle the core style identifier case
			if (path.length == 2) {
				attributesInfo.put(extension.getUniqueIdentifier(), null);
				continue;
			}

			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equals(path[1])) {
					members.add(element);
				}
			}
		}
		List<IConfigurationElement> parents = members;
		for (int i = 2; i < path.length; i++) {
			if (path[i].startsWith("@")) { //$NON-NLS-1$
				String attName = path[i].substring(1);
				for (IConfigurationElement element : parents) {
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
			members = new ArrayList<>();
			for (IConfigurationElement element : parents) {
				members.addAll(keepGoing(element, path[i]));
			}
			parents = members;
		}
	}

	private static List<IConfigurationElement> keepGoing(IConfigurationElement element, String tag) {
		return Arrays.asList(element.getChildren(tag));
	}

	private static String buildBasedOnValue(ISchemaObject object) {
		if (object instanceof ISchemaElement && !(object instanceof ISchemaRootElement)) {
			ISchemaElement schemaElement = (ISchemaElement) object;
			ISchema schema = schemaElement.getSchema();
			ISchemaElement[] elements = schema.getElements();
			for (ISchemaElement element : elements) {
				ISchemaElement[] children = schema.getCandidateChildren(element);
				for (ISchemaElement childElement : children) {
					if (object.getName().equals(childElement.getName())) {
						return buildBasedOnValue(element) + '/' + object.getName();
					}
				}
			}
		}
		return ""; //$NON-NLS-1$
	}

}
