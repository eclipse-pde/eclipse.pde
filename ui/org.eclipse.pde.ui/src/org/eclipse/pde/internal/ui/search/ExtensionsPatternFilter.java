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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

	protected Set fAttributes = new HashSet();
	protected final Set fMatchingLeafs = new HashSet();
	protected final Set fFoundAnyElementsCache = new HashSet();

	/**
	 * Check if the leaf element is a match with the filter text. The
	 * default behavior checks that the label of the element is a match.
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
		// match label; default behaviour
		if (viewer != null && super.isLeafMatch(viewer, element)) {
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
		if (fAttributes != null && fAttributes.size() > 0) {
			int attributeNumber = 0;
			for (Iterator iterator = fAttributes.iterator(); iterator.hasNext();) {
				String valuePattern = (String) iterator.next();
				if (attributeNumber < fAttributes.size() && attributeNumber < ATTRIBUTE_LIMIT) {
					boolean quoted = isQuoted(valuePattern);
					if (valuePattern != null && valuePattern.length() > 0) {
						int attributeCount = pluginElement.getAttributeCount();
						IPluginAttribute[] pluginAttributes = pluginElement.getAttributes();

						for (int i = 0; i < attributeCount; i++) {
							IPluginAttribute attributeElement = pluginAttributes[i];
							if (attributeElement != null && attributeElement.getValue() != null) {
								String[] attributes = getAttributeSplit(attributeElement.getValue(), quoted);
								if (attributes != null) {
									List attributeList = new ArrayList(Arrays.asList(attributes));
									attributeList.addAll(syntheticAttributes);
									if (matchWithAttributes(pluginElement, valuePattern, attributeList, quoted)) {
										return true;
									}
								}
							}
						}
						if (valuePattern.equalsIgnoreCase(pluginElement.getName())) {
							return true;
						}
					}
				}
				attributeNumber++;
			}
		}
		return false;
	}

	private boolean matchWithAttributes(IPluginElement pluginElement, String valuePattern, List attributeList, boolean quoted) {
		for (int k = 0; k < attributeList.size(); k++) {
			String attribute = (String) attributeList.get(k);
			if (attribute != null && attribute.length() > 0) {
				if (!attribute.startsWith("%")) { //$NON-NLS-1$
					int delimiterPosition = attribute.indexOf('?'); // strip right of '?'
					if (delimiterPosition != -1) {
						attribute = attribute.substring(0, delimiterPosition);
					}
				} else {
					String resourceValue = pluginElement.getResourceString(attribute);
					attribute = (resourceValue != null && resourceValue.length() > 0) ? resourceValue : attribute;
				}
				String pattern = valuePattern.toLowerCase();
				if (quoted) {
					pattern = pattern.substring(1, pattern.length() - 1);
				}
				if (attribute.toLowerCase().equals(pattern)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isQuoted(String value) {
		return value.startsWith("\"") && value.endsWith("\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String[] getAttributeSplit(String text, boolean quoted) {
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
				List patterns = new ArrayList();
				List matchList = new ArrayList();
				Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'"); //$NON-NLS-1$
				Matcher regexMatcher = regex.matcher(text);
				while (regexMatcher.find()) {
					matchList.add(regexMatcher.group());
				}
				for (int i = 0; i < matchList.size(); i++) {
					String element = (String) matchList.get(i);
					if (isQuoted(element)) {
						patterns.add(element);
					} else {
						String[] elements = element.split("/"); //$NON-NLS-1$
						for (int k = 0; k < elements.length; k++) {
							String splitted = elements[k];
							if (splitted.length() > 0) {
								patterns.add(splitted);
							}
						}
					}
				}
				return (String[]) patterns.toArray(new String[0]);
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
		fAttributes.clear();
		fAttributes.addAll(Arrays.asList(patterns));
		fFoundAnyElementsCache.clear();
	}

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