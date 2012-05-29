/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.util.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.text.plugin.PluginNode;
import org.eclipse.pde.internal.ui.search.ExtensionsPatternFilter;

public class ExtensionsFilterUtil {

	/**
	 * <code>id, class, commandId, pattern, locationURI, defaultHandler, variable
	 * property, contentTypeId, path, plugin, perspective, targetID</code>
	 */
	// TODO related attributes might be configured through preferences
	public static final String[] RELATED_ATTRIBUTES = {"id", //$NON-NLS-1$
			"class", "commandId", "pattern", "locationURI", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			"defaultHandler", "variable", "property", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"contentTypeId", "path", "plugin", "perspective", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"targetID"}; //$NON-NLS-1$

	public static final String ATTRIBUTE_ACTIVITYID = "activityId"; //$NON-NLS-1$
	public static final String ATTRIBUTE_CATEGORY = "category"; //$NON-NLS-1$
	public static final String ATTRIBUTE_CATEGORYID = "categoryId"; //$NON-NLS-1$
	public static final String ATTRIBUTE_COMMANDID = "commandId"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DEFAULTHANDLER = "defaultHandler"; //$NON-NLS-1$
	public static final String ATTRIBUTE_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$
	public static final String ATTRIBUTE_MNEMONIC = "mnemonic"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAMESPACE = "namespace"; //$NON-NLS-1$
	public static final String ATTRIBUTE_PROPERTIES = "properties"; //$NON-NLS-1$
	public static final String ATTRIBUTE_PATTERN = "pattern"; //$NON-NLS-1$
	public static final String ATTRIBUTE_REQUIREDACTIVITYID = "requiredActivityId"; //$NON-NLS-1$
	public static final String ATTRIBUTE_TOOLTIP = "tooltip"; //$NON-NLS-1$
	public static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$

	public static final String ELEMENT_ACATEGORY = "org.eclipse.ui.activities.category"; //$NON-NLS-1$
	public static final String ELEMENT_ACTIVITY = "org.eclipse.ui.activities.activity"; //$NON-NLS-1$
	public static final String ELEMENT_AR_BINDING = "org.eclipse.ui.activities.activityRequirementBinding"; //$NON-NLS-1$
	public static final String ELEMENT_CA_BINDING = "org.eclipse.ui.activities.categoryActivityBinding"; //$NON-NLS-1$
	public static final String ELEMENT_COMMAND = "org.eclipse.ui.commands.command"; //$NON-NLS-1$
	public static final String ELEMENT_EQUALS = "equals"; //$NON-NLS-1$
	public static final String ELEMENT_HELP_TOC = "org.eclipse.help.toc.toc"; //$NON-NLS-1$
	public static final String ELEMENT_INSTANCEOF = "instanceof"; //$NON-NLS-1$
	public static final String ELEMENT_MENU_COMMAND = "org.eclipse.ui.menus.command"; //$NON-NLS-1$
	public static final String ELEMENT_PARAMETER = "parameter"; //$NON-NLS-1$
	public static final String ELEMENT_PATTERNBINDING = "org.eclipse.ui.activities.activityPatternBinding"; //$NON-NLS-1$
	public static final String ELEMENT_PROPERTYTESTER = "org.eclipse.core.expressions.propertyTesters.propertyTester"; //$NON-NLS-1$
	public static final String ELEMENT_VARIABLE = "variable"; //$NON-NLS-1$

	public static final String[] HIGH_PRIORITY_ELEMENTS = new String[] {ELEMENT_COMMAND, ELEMENT_MENU_COMMAND, ELEMENT_INSTANCEOF, ELEMENT_EQUALS};
	public static final String[] LOW_PRIORITY_ELEMENTS = new String[] {ELEMENT_PARAMETER, ELEMENT_CA_BINDING, ELEMENT_AR_BINDING, ELEMENT_VARIABLE, ELEMENT_HELP_TOC};
	public static final String[] RESOURCE_ATTRIBUTES = new String[] {ATTRIBUTE_LABEL, ATTRIBUTE_NAME, ATTRIBUTE_TOOLTIP, ATTRIBUTE_MNEMONIC, ATTRIBUTE_DESCRIPTION};

	public static final Map CUSTOM_RELATIONS;

	static {
		CUSTOM_RELATIONS = new HashMap();
		CUSTOM_RELATIONS.put(ELEMENT_COMMAND, new String[] {ATTRIBUTE_ID, ATTRIBUTE_DEFAULTHANDLER});
		CUSTOM_RELATIONS.put(ELEMENT_INSTANCEOF, new String[] {ATTRIBUTE_VALUE});
		CUSTOM_RELATIONS.put(ELEMENT_EQUALS, new String[] {ATTRIBUTE_VALUE});
		CUSTOM_RELATIONS.put(ELEMENT_PARAMETER, new String[] {ATTRIBUTE_NAME});
		CUSTOM_RELATIONS.put(ELEMENT_VARIABLE, new String[] {ATTRIBUTE_NAME});
		CUSTOM_RELATIONS.put(ELEMENT_MENU_COMMAND, new String[] {ATTRIBUTE_COMMANDID, ATTRIBUTE_ID});
		CUSTOM_RELATIONS.put(ELEMENT_CA_BINDING, new String[] {ATTRIBUTE_ACTIVITYID, ATTRIBUTE_CATEGORYID});
		CUSTOM_RELATIONS.put(ELEMENT_AR_BINDING, new String[] {ATTRIBUTE_REQUIREDACTIVITYID, ATTRIBUTE_ACTIVITYID});
		CUSTOM_RELATIONS.put(ELEMENT_HELP_TOC, new String[] {ATTRIBUTE_CATEGORY});
	}

	private static String BOOLEAN_TRUE = "true"; //$NON-NLS-1$
	private static String BOOLEAN_FALSE = "false"; //$NON-NLS-1$
	private static String ADDITIONS = "additions"; //$NON-NLS-1$

	public static boolean add(Set pattern, IPluginElement pluginElement, String attributeName) {
		IPluginAttribute attribute = pluginElement.getAttribute(attributeName);
		if (attribute != null) {
			return add(pattern, attribute.getValue());
		}
		return false;
	}

	public static boolean add(Set pattern, String value) {
		if (value != null && value.length() > 0) {
			String trimmed = value.trim();
			if (!isBoolean(trimmed)) {
				return pattern.add(trimmed);
			}
		}
		return false;
	}

	public static void addAll(Set pattern, IPluginElement pluginElement, String elementName) {
		Object attributes = CUSTOM_RELATIONS.get(elementName);
		if (attributes != null) {
			String[] attributesArray = (String[]) attributes;
			for (int i = 0; i < attributesArray.length; i++) {
				add(pattern, pluginElement, attributesArray[i]);
			}
		}
	}

	/**
	 * Get unique plugin element name. This will work only with 
	 * editable plugin models, otherwise <code>null</code> is returned.
	 * 
	 * @param pluginElement the element to get the unique name from
	 * @return extensionpoint name concatenated with the element name
	 */
	public static String getElementPath(IPluginElement pluginElement) {
		IPluginObject element = pluginElement;
		while (element.getParent() != null && !(element.getParent() instanceof PluginNode)) {
			element = element.getParent();
		}
		if (element instanceof IPluginExtension) {
			IPluginExtension extension = (IPluginExtension) element;
			return extension.getPoint() + '.' + pluginElement.getName();
		}
		return null;
	}

	/**
	 * Obtains common attributes from selected plugin element to filter tree for; 
	 * attribute values are concatenated with a slash and set as filter text
	 * 
	 * @param selection selected items to filter for related plugin elements 
	 */
	public static String getFilterRelatedPattern(IStructuredSelection selection) {
		Iterator it = selection.iterator();
		Set filterPatterns = new HashSet();
		while (it.hasNext()) {
			Object treeElement = it.next();
			if (treeElement instanceof IPluginElement) {
				IPluginElement pluginElement = (IPluginElement) treeElement;
				Set customAttributes = getCustomRelations(pluginElement);
				if (customAttributes.size() == 0) {
					for (int i = 0; i < RELATED_ATTRIBUTES.length; i++) {
						String property = RELATED_ATTRIBUTES[i];
						IPluginAttribute attribute = pluginElement.getAttribute(property);
						if (attribute != null && attribute.getValue() != null && attribute.getValue().length() > 0) {
							String value = attribute.getValue();
							if (!value.startsWith("%")) { //$NON-NLS-1$
								int delimiterPosition = value.indexOf('?'); // split before '?' and right after last '='
								if (delimiterPosition == -1) {
									if (!isBoolean(value)) {
										filterPatterns.add(value);
									}
								} else {
									filterPatterns.add(value.substring(0, delimiterPosition));
									int position = value.lastIndexOf('=');
									if (position != -1) {
										String placeHolder = value.substring(position + 1, value.length());
										if (!placeHolder.equalsIgnoreCase(ADDITIONS)) {
											filterPatterns.add(placeHolder);
										}
									}
								}
							} else {
								String resourceValue = pluginElement.getResourceString(value);
								if ((resourceValue != null && resourceValue.length() > 0)) {
									filterPatterns.add(resourceValue);
								}
							}
						}
					}
				} else {
					filterPatterns.addAll(customAttributes);
				}
			}
		}
		StringBuffer patternBuffer = new StringBuffer();
		int attributeCount = 0;
		for (Iterator iterator = filterPatterns.iterator(); iterator.hasNext();) {
			attributeCount++;
			Object pattern = iterator.next();
			if (attributeCount < ExtensionsPatternFilter.ATTRIBUTE_LIMIT) {
				if (pattern != null) {
					patternBuffer.append(pattern);
					patternBuffer.append('/');
				}
			}
		}

		String filterPattern = patternBuffer.toString();
		if (filterPattern.endsWith("/")) { //$NON-NLS-1$
			filterPattern = filterPattern.substring(0, filterPattern.length() - 1);
		}
		return filterPattern;
	}

	public static Set getCustomRelations(IPluginElement pluginElement) {
		Set customAttributes = new TreeSet();
		String elementName = (pluginElement != null) ? getElementPath(pluginElement) : null;
		if (elementName == null) {
			return customAttributes;
		} else if (addMatchingElements(customAttributes, pluginElement, elementName, HIGH_PRIORITY_ELEMENTS)) {
		} else if (ELEMENT_ACTIVITY.equalsIgnoreCase(elementName) || ELEMENT_ACATEGORY.equals(elementName)) {
			if (!add(customAttributes, pluginElement, ATTRIBUTE_ID)) {
				add(customAttributes, pluginElement, ATTRIBUTE_NAME);
			}
		} else if (ELEMENT_PROPERTYTESTER.equalsIgnoreCase(elementName)) {
			customAttributes = handlePropertyTester(customAttributes, pluginElement);
			add(customAttributes, pluginElement, ATTRIBUTE_ID);
		} else if (ELEMENT_PATTERNBINDING.equals(elementName)) {
			add(customAttributes, pluginElement, ATTRIBUTE_ACTIVITYID);
			String attributeValue = pluginElement.getAttribute(ATTRIBUTE_PATTERN).getValue();
			if (attributeValue.length() > 0) {
				int lastSeparator = attributeValue.lastIndexOf('/');
				if (lastSeparator > 0 && attributeValue.length() > lastSeparator + 1) {
					customAttributes.add(attributeValue.substring(lastSeparator + 1, attributeValue.length()));
				}
			}
		} else {
			addMatchingElements(customAttributes, pluginElement, elementName, LOW_PRIORITY_ELEMENTS);
		}
		return customAttributes;
	}

	private static boolean addMatchingElements(Set customAttributes, IPluginElement pluginElement, String elementName, final String[] elements) {
		boolean elementMatch = false;
		if (elementName != null) {
			for (int i = 0; i < elements.length; i++) {
				if (elementName.endsWith(elements[i])) {
					addAll(customAttributes, pluginElement, elements[i]);
					elementMatch = true;
				}
			}
		}
		return elementMatch;
	}

	private static Set handlePropertyTester(Set customAttributes, IPluginElement pluginElement) {
		String namespace = pluginElement.getAttribute(ATTRIBUTE_NAMESPACE).getValue();
		String properties = pluginElement.getAttribute(ATTRIBUTE_PROPERTIES).getValue();
		if (namespace.length() > 0) {
			String[] propertiesArray = properties.split(","); //$NON-NLS-1$
			for (int i = 0; i < propertiesArray.length; i++) {
				String property = propertiesArray[i].trim();
				if (property.length() > 0) {
					customAttributes.add(namespace + '.' + property);
				}
			}
			if (propertiesArray.length == 0) {
				customAttributes.add(namespace);
			}
		}
		return customAttributes;
	}

	public static List handlePropertyTester(IPluginElement pluginElement) {
		List propertyTestAttributes = new ArrayList();
		if (isElementNameMatch(pluginElement, ELEMENT_PROPERTYTESTER)) {
			Set attributes = handlePropertyTester(new HashSet(), pluginElement);
			for (Iterator iterator = attributes.iterator(); iterator.hasNext();) {
				propertyTestAttributes.add(iterator.next());
			}
		}
		return propertyTestAttributes;
	}

	public static boolean isElementNameMatch(IPluginElement pluginElement, String expected) {
		String elementName = getElementPath(pluginElement);
		if (elementName == null) {
			// workaround for non-editable plugins of the target platform
			if (expected.endsWith(pluginElement.getName())) {
				return true;
			}
		} else if (elementName.endsWith(expected)) {
			return true;
		}
		return false;
	}

	public static boolean isAttributeNameMatch(String attributeName, String[] matches) {
		for (int i = 0; i < matches.length; i++) {
			String matchingAttribute = matches[i];
			if (matchingAttribute.equals(attributeName)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isFilterRelatedEnabled(IPluginElement pluginElement) {
		for (int i = 0; i < RELATED_ATTRIBUTES.length; i++) {
			String property = RELATED_ATTRIBUTES[i];
			IPluginAttribute attribute = pluginElement.getAttribute(property);
			if (attribute != null) {
				return true;
			}
		}
		// test for custom relations
		String elementName = getElementPath(pluginElement);
		if (elementName != null) {
			Set keySet = CUSTOM_RELATIONS.keySet();
			for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				if (elementName.endsWith(key)) {
					Object attributes = CUSTOM_RELATIONS.get(key);
					if (attributes != null) {
						String[] attributesArray = (String[]) attributes;
						for (int i = 0; i < attributesArray.length; i++) {
							IPluginAttribute attribute = pluginElement.getAttribute(attributesArray[i]);
							if (attribute != null) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	public static boolean isFilterRelatedEnabled(IStructuredSelection structuredSelection) {
		boolean createFilterRelatedAction = false;
		if (structuredSelection != null && !structuredSelection.isEmpty()) {
			Iterator it = structuredSelection.iterator();
			while (it.hasNext()) {
				Object treeElement = it.next();
				if (treeElement instanceof IPluginElement) {
					createFilterRelatedAction |= isFilterRelatedEnabled((IPluginElement) treeElement);
				}
			}
		}
		return createFilterRelatedAction;
	}

	public static boolean isBoolean(String bool) {
		return bool.equalsIgnoreCase(BOOLEAN_TRUE) || bool.equalsIgnoreCase(BOOLEAN_FALSE);
	}

}