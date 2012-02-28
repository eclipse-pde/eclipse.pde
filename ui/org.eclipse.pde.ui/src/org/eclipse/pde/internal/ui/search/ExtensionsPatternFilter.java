/*******************************************************************************
 *  Copyright (c) 2011, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sascha Becher <s.becher@qualitype.de> - bug 360894
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.util.*;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.bundle.BundlePlugin;
import org.eclipse.pde.internal.ui.util.ExtensionsFilterUtil;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * An extended filtering capability for the filtered tree of ExtensionsPage. The
 * search criteria is splitted by / first. The resulting values are used to
 * perform a search on all node's values. All elements fitting at least one of
 * the split values will be displayed. This extensions does not compromise the
 * default filtering behaviour of the tree while providing the ability to
 * highlight related items such as commands along with their command images,
 * handlers, menu entries and activities.
 * 
 * @see org.eclipse.ui.dialogs.FilteredTree
 * @since 3.8
 * 
 */
public class ExtensionsPatternFilter extends PatternFilter {

	/**
	 * Limits the maximum number of attributes handled by the filter
	 */
	public static final int ATTRIBUTE_LIMIT = 30;

	protected String fSearchPattern;

	protected Set fSearchPatterns = new HashSet();
	protected final Set fMatchingLeafs = new HashSet();
	protected final Set fFoundAnyElementsCache = new HashSet();

	/**
	 * Check if the leaf element is a match with the filter text. The default behavior 
	 * checks that the element name or extension point is a match employing wildcards.
	 * An implicit wild card is added at the end always (default behaviour).
	 * 
	 * Subclasses should override this method.
	 * 
	 * @param viewer
	 *            the viewer that contains the element
	 * @param element
	 *            the tree element to check
	 * @return true if the given element's label matches the filter text
	 */
	protected boolean isLeafMatch(Viewer viewer, Object element) {
		// match element name or extension point with wildcards; modified default behaviour
		if (isNameMatch(element)) {
			return true;
		}
		// match all splitted attribute's values of IPluginElement against splitted filter patterns
		if (element instanceof IPluginElement) {
			return doIsLeafMatch((IPluginElement) element);
		}
		return false;
	}

	protected boolean doIsLeafMatch(IPluginElement pluginElement) {
		List syntheticAttributes = ExtensionsFilterUtil.handlePropertyTester(pluginElement);
		if (fSearchPatterns != null && fSearchPatterns.size() > 0) {
			int attributeNumber = 0;
			for (Iterator iterator = fSearchPatterns.iterator(); iterator.hasNext();) {
				String searchPattern = (String) iterator.next();
				if (attributeNumber < fSearchPatterns.size() && attributeNumber < ATTRIBUTE_LIMIT) {
					boolean quoted = isQuoted(searchPattern);
					if (searchPattern != null && searchPattern.length() > 0) {
						if (quoted) {
							searchPattern = searchPattern.substring(1, searchPattern.length() - 1);
						}
						int attributeCount = pluginElement.getAttributeCount();
						IPluginAttribute[] elementAttributes = pluginElement.getAttributes();

						for (int i = 0; i < attributeCount; i++) {
							IPluginAttribute attributeElement = elementAttributes[i];
							if (attributeElement != null && attributeElement.getValue() != null) {
								String[] attributes = getAttributeSplit(attributeElement.getValue(), quoted);
								if (attributes != null) {
									List attributeList = new ArrayList(Arrays.asList(attributes));
									attributeList.addAll(syntheticAttributes);
									if (matchWithAttributes(pluginElement, searchPattern, attributeElement.getName(), attributeList, quoted)) {
										return true;
									}
								}
							}
						}
						if (searchPattern.equalsIgnoreCase(pluginElement.getName())) {
							return true;
						}
					}
				}
				attributeNumber++;
			}
		}
		return false;
	}

	protected boolean isNameMatch(Object element) {
		if (element != null) {
			if (element instanceof IPluginElement) {
				if (super.wordMatches(((IPluginElement) element).getName())) {
					return true;
				}
			} else if (element instanceof IPluginExtension) {
				if (super.wordMatches(((IPluginExtension) element).getPoint())) {
					return true;
				}
			}
		}
		return false;
	}

	protected boolean matchWithAttributes(IPluginElement pluginElement, String searchPattern, String attributeName, List attributeList, boolean quoted) {
		for (int k = 0; k < attributeList.size(); k++) {
			String attributeValue = (String) attributeList.get(k);
			if (attributeValue != null && attributeValue.length() > 0) {
				if (!attributeValue.startsWith("%") || quoted) { //$NON-NLS-1$
					int delimiterPosition = attributeValue.indexOf('?'); // strip right of '?'
					if (delimiterPosition != -1) {
						attributeValue = attributeValue.substring(0, delimiterPosition);
					}
					// case insensitive exact match required
					if (attributeValue.equalsIgnoreCase(searchPattern)) {
						return true;
						// missing use of resource bundle localization requires wildcard enabled search
					} else if (!quoted && isNoneResourceMatch(attributeValue, attributeName, searchPattern)) {
						return true;
					}
				} else { // resource bundle key found
					String resourceValue = pluginElement.getResourceString(attributeValue);
					attributeValue = (resourceValue != null && resourceValue.length() > 0) ? resourceValue : attributeValue;
					super.setPattern(new String(searchPattern));
					// case insensitive match required with wildcards enabled
					boolean match = (super.wordMatches(attributeValue));
					super.setPattern(fSearchPattern);
					if (match) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * While the plugin model offers resource bundle localization, some plugins may skip this and use fix text for display.
	 * Wildcard enabled search of the PatternFilter should be available in this case. Only a list of attributes that 
	 * are expected to contain resource bundles are evaluated as long as the value doesn't contain a point. On some elements
	 * for example a name attribute can contain an id. Those are skipped though.
	 * 
	 * @param attributeValue
	 * @param attributeName
	 * @param searchPattern
	 * @return whether this is a match
	 */
	protected boolean isNoneResourceMatch(String attributeValue, String attributeName, String searchPattern) {
		if (ExtensionsFilterUtil.isAttributeNameMatch(attributeName, ExtensionsFilterUtil.RESOURCE_ATTRIBUTES)) {
			if (attributeValue.indexOf('.') == -1 && searchPattern.indexOf('.') == -1) { // no ids
				super.setPattern(searchPattern);
				boolean match = super.wordMatches(attributeValue);
				super.setPattern(fSearchPattern);
				if (match) {
					return true;
				}
				super.setPattern(fSearchPattern);
			}
		}
		return false;
	}

	static boolean isQuoted(String value) {
		return value.startsWith("\"") && value.endsWith("\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Splits attributes on occurrence of /<br>
	 * If <code>quoted</code> is set to <code>true</code> parameter <code>text</code> is returned as the only
	 * element in the array, thus skipping the splitting.
	 * 
	 * @param text text to split
	 * @param quoted decides whether splitting actually occurs
	 * @return split array containing the splitted attributes or one element containing the value of parameter <code>text</code>
	 */
	static String[] getAttributeSplit(String text, boolean quoted) {
		if (text.length() < 2) {
			return null;
		}
		if (!quoted) {
			return text.replaceAll("/{1,}", "/").split("/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		return new String[] {text};
	}

	public boolean isElementVisible(Viewer viewer, Object element) {
		if (fFoundAnyElementsCache.contains(element)) {
			return true;
		}
		return isLeafMatch(viewer, element);
	}

	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		if (parent != null && parent instanceof BundlePlugin) {
			if (fFoundAnyElementsCache.size() == 0 && fSearchPattern != null && fSearchPattern.length() > 0) {
				BundlePlugin pluginPlugin = (BundlePlugin) parent;
				doFilter(viewer, pluginPlugin, pluginPlugin.getExtensions(), false);
			}
		}
		if (fFoundAnyElementsCache.size() > 0) {
			List found = new ArrayList();
			for (int i = 0; i < elements.length; i++) {
				if (fFoundAnyElementsCache.contains(elements[i])) {
					found.add(elements[i]);
				}
			}
			return found.toArray();
		}
		return super.filter(viewer, parent, elements);
	}

	protected boolean doFilter(Viewer viewer, Object parent, IPluginObject[] children, boolean addChildren) {
		boolean isParentMatch = fFoundAnyElementsCache.contains(parent) ? true : false;

		// find leaf matches
		boolean isAnyLeafMatch = false;
		for (int j = 0; j < children.length; j++) {
			IPluginObject iPluginObject = children[j];
			boolean isChildMatch = true;
			if (!isParentMatch || children.length > 0) {
				isChildMatch = this.isLeafMatch(viewer, iPluginObject);
				isAnyLeafMatch |= isChildMatch;
				if (isChildMatch) {
					fMatchingLeafs.add(iPluginObject);
				}
			}
			if (isChildMatch || addChildren) {
				fFoundAnyElementsCache.add(iPluginObject);
			}
		}

		// traverse children when available
		boolean isAnyChildMatch = false;
		for (int i = 0; i < children.length; i++) {
			IPluginObject iPluginObject = children[i];
			if (iPluginObject instanceof IPluginParent) {
				IPluginParent pluginElement = (IPluginParent) iPluginObject;
				if (pluginElement.getChildren().length > 0) {
					boolean isChildrenMatch = doFilter(viewer, pluginElement, pluginElement.getChildren(), addChildren | fMatchingLeafs.contains(pluginElement));
					isAnyChildMatch |= isChildrenMatch;
					if (isChildrenMatch) {
						fFoundAnyElementsCache.add(pluginElement);
					}
				}
			}
		}
		return isAnyChildMatch | isAnyLeafMatch;
	}

	/**
	 * Splits a string at the occurrences of <code>/</code>. Any quoted parts of the <code>filterText</code>
	 * are not to be splitted but remain as a whole along with the quotation.
	 *   
	 * @param filterText text to split
	 * @return split array
	 */
	protected String[] splitWithQuoting(String filterText) {
		// remove multiple separators
		String text = filterText.replaceAll("/{1,}", "/"); //$NON-NLS-1$//$NON-NLS-2$ 
		boolean containsQuoting = text.indexOf('\"') != -1;
		if (containsQuoting) {
			// remove multiple quotes
			text = text.replaceAll("\"{1,}", "\""); //$NON-NLS-1$//$NON-NLS-2$
			// treat quoted text as a whole, thus enables searching for file paths
			if (text.replaceAll("[^\"]", "").length() % 2 == 0) { //$NON-NLS-1$//$NON-NLS-2$
				return text.split("/(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))"); //$NON-NLS-1$
			} // filter text must have erroneous quoting, replacing all
			text = text.replaceAll("[\"]", ""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return text.split("/"); //$NON-NLS-1$
	}

	/**
	 * Enables the filter to temporarily display arbitrary elements
	 * 
	 * @param element
	 */
	public boolean addElement(Object element) {
		return fFoundAnyElementsCache.add(element);
	}

	/**
	 * Removes elements from the filter
	 * 
	 * @param element
	 */
	public boolean removeElement(Object element) {
		return fFoundAnyElementsCache.remove(element);
	}

	/*
	 * The pattern string for which this filter should select 
	 * elements in the viewer.
	 * 
	 * @see org.eclipse.ui.dialogs.PatternFilter#setPattern(java.lang.String)
	 */
	public final void setPattern(String patternString) {
		super.setPattern(patternString);
		fSearchPattern = patternString;
		String[] patterns = (patternString != null) ? splitWithQuoting(patternString) : new String[] {};
		fSearchPatterns.clear();
		fSearchPatterns.addAll(Arrays.asList(patterns));
		fFoundAnyElementsCache.clear();
	}

	/**
	 * @return the whole filter text (unsplit) 
	 */
	public String getPattern() {
		return fSearchPattern;
	}

	public void clearMatchingLeafs() {
		fMatchingLeafs.clear();
	}

	public Object[] getMatchingLeafsAsArray() {
		return fMatchingLeafs.toArray();
	}

	public Set getMatchingLeafs() {
		return fMatchingLeafs;
	}

	public boolean containsElement(Object element) {
		return fFoundAnyElementsCache.contains(element);
	}

}