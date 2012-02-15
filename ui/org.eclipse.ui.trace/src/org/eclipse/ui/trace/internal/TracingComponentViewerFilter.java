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
package org.eclipse.ui.trace.internal;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.trace.internal.datamodel.TracingComponent;
import org.eclipse.ui.trace.internal.datamodel.TracingComponentDebugOption;

/**
 * A {@link ViewerFilter} for filtering the contents of the trace component tree viewer.
 */
public class TracingComponentViewerFilter extends PatternFilter {

	/**
	 * Construct a new {@link TracingComponentViewerFilter}
	 */
	public TracingComponentViewerFilter() {

		visibleTracingComponentsCache = new HashMap<TracingComponent, Boolean>();
		visibleTracingDebugOptions = new HashMap<TracingComponentDebugOption, Boolean>();
	}

	@Override
	public void setPattern(final String patternString) {

		super.setPattern(patternString);
		/**
		 * Hack to clear my own caches (whenever the contents changes)
		 */
		visibleTracingComponentsCache.clear();
		visibleTracingDebugOptions.clear();
	}

	/*
	 * Force the array of {@link TracingComponentDebugOption} and their children to be visible. 
	 */
	private void forceVisibleDebugOptions(final TracingComponentDebugOption[] options) {

		for (int optionsIndex = 0; optionsIndex < options.length; optionsIndex++) {
			if (options[optionsIndex].hasChildren()) {
				forceVisibleDebugOptions(options[optionsIndex].getChildren());
			}
			visibleTracingDebugOptions.put(options[optionsIndex], Boolean.TRUE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.PatternFilter#isElementVisible(org.eclipse.jface.viewers.Viewer, java.lang.Object)
	 */
	@Override
	public boolean isElementVisible(final Viewer viewer, final Object element) {

		boolean isVisible = false;
		if (element instanceof TracingComponent) {
			TracingComponent component = (TracingComponent) element;
			boolean textMatches = super.isLeafMatch(viewer, component);
			if (textMatches) {
				// the text matches - make sure all children of this component are visible
				isVisible = true;
				visibleTracingComponentsCache.put(component, Boolean.TRUE);
				// show all children
				forceVisibleDebugOptions(component.getChildren());
			} else {
				// show only if has 1 child that matches
				isVisible = super.isElementVisible(viewer, element);
				visibleTracingComponentsCache.put(component, Boolean.valueOf(isVisible));
			}
		} else if (element instanceof TracingComponentDebugOption) {
			// check to see if this debug option is forced to be visible
			Boolean enabled = visibleTracingDebugOptions.get(element);
			if (enabled != null) {
				// it should be enabled because the parent is enabled and forced it to be enabled
				isVisible = enabled.booleanValue();
			} else {
				// make it visible only if it matches
				isVisible = super.isLeafMatch(viewer, element);
				if (!isVisible) {
					// This debug option did not match - does anything for this branch/category match?
					isVisible = super.isParentMatch(viewer, element);
				}
			}
		}
		return isVisible;
	}

	private final Map<TracingComponent, Boolean> visibleTracingComponentsCache;

	private final Map<TracingComponentDebugOption, Boolean> visibleTracingDebugOptions;
}