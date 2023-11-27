/*******************************************************************************
 * Copyright (c) 2014 OPCoach.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     OPCoach - initial API and implementation for bug #437478
 *******************************************************************************/
package org.eclipse.pde.internal.spy.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.internal.contexts.Computation;
import org.eclipse.e4.core.internal.contexts.EclipseContext;
import org.eclipse.e4.core.services.log.Logger;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@SuppressWarnings("restriction")
@Creatable
@Singleton
public class ContextDataFilter extends ViewerFilter {

	@Inject
	Logger log;

	private String pattern;

	// Implements the filter for the data table content
	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if ((element == ContextDataProvider.LOCAL_VALUE_NODE)
				|| (element == ContextDataProvider.INHERITED_INJECTED_VALUE_NODE))
			return true;

		// Must only select objects matching the pattern or objects under a kept
		// node (to see where it is injected)
		TreeViewer tv = (TreeViewer) viewer;
		ContextDataProvider lpkey = (ContextDataProvider) tv.getLabelProvider(0);
		ContextDataProvider lpval = (ContextDataProvider) tv.getLabelProvider(1);

		// If the text matches in one of the column, must keep it...
		String skey = lpkey.getText(element);
		String sval = lpval.getText(element);

		// Must also keep the listener elements if the parent is selected ->
		// Must compute parent keys
		String sparentkey = lpkey.getText(parentElement);
		String sparentval = lpval.getText(parentElement);

		Set<Computation> listeners = lpkey.getListeners(parentElement);
		boolean mustKeepParent = (matchText(sparentkey) || matchText(sparentval)) && (listeners != null)
				&& !listeners.isEmpty();
		boolean mustKeepElement = matchText(skey) || matchText(sval);

		return mustKeepElement || (!mustKeepElement && mustKeepParent);

	}

	/** Set the pattern and use it as lowercase */
	public void setPattern(String newPattern) {
		if ((newPattern == null) || (newPattern.length() == 0))
			pattern = null;
		else
			pattern = newPattern.toLowerCase();
	}

	/**
	 * This method search for an object and check if it contains the text or a
	 * pattern matching this text
	 */
	public boolean containsText(IEclipseContext ctx) {
		// It is useless to store the values in a map, because context changes
		// everytime and it should be tracked.
		Collection<String> values = computeValues(ctx);

		// Search if string is just in one of the values... manage ignore case
		// and contain...
		boolean found = false;
		for (String s : values) {
			if (matchText(s)) {
				found = true;
				break;
			}
		}
		return found;
	}

	public boolean matchText(String text) {
		return ((text == null) || (pattern == null)) ? false : text.toLowerCase().contains(pattern);
	}

	/**
	 * Extract all string values in context
	 */
	private Collection<String> computeValues(IEclipseContext ctx) {
		Collection<String> result = new ArrayList<>();
		if (ctx instanceof EclipseContext) {
			// Search for all strings in this context (values and context
			// function)

			EclipseContext currentContext = (EclipseContext) ctx;
			extractStringsFromMap(currentContext.localData(), result);

			// Search also in context functions
			extractStringsFromMap(currentContext.localContextFunction(), result);

			// Search for the inherited values injected using this context but
			// defined in
			// parent
			// Keep only the names that are not already displayed in local
			// values
			Collection<String> localKeys = currentContext.localData().keySet();
			Collection<String> localContextFunctionsKeys = currentContext.localContextFunction().keySet();

			if (currentContext.getRawListenerNames() != null) {
				for (String name : currentContext.getRawListenerNames()) {
					if (!localKeys.contains(name) && !localContextFunctionsKeys.contains(name))
						result.add(name);
				}
			}

		} else {
			log.warn(Messages.ContextDataFilter_0
					+ ctx.getClass().toString());
		}

		return result;
	}

	/**
	 *
	 * @param map
	 *            the map to extract the strings (keys and values)
	 * @param result
	 *            the result to fill with strings
	 */
	private void extractStringsFromMap(Map<String, Object> map, Collection<String> result) {
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			result.add(entry.getKey());
			Object value = entry.getValue();
			if (value != null) {
				result.add(value.toString());
			}
		}
	}

}
