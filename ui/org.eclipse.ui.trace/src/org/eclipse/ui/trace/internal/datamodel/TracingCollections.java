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
package org.eclipse.ui.trace.internal.datamodel;

import java.util.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.eclipse.ui.trace.internal.utils.TracingUtils;
import org.osgi.framework.Bundle;

/**
 * A utility class for handling the various collections of the product tracing UI.
 */
public class TracingCollections {

	/**
	 * Constructor for the {@link TracingCollections}
	 */
	protected TracingCollections() {

		this.fComponentCollection = new HashMap<String, TracingComponent>();
		this.fDebugOptionCollection = new HashMap<String, List<TracingComponentDebugOption>>();
		this.fBundleOptionsCollection = new HashMap<Bundle, Properties>();
		this.fBundleConsumedCollection = new HashMap<Bundle, Boolean>();
		this.fBundleComponentCollection = new HashMap<Bundle, List<TracingComponent>>();
		this.fModifiedDebugOptions = new ModifiedDebugOptions();
	}

	/**
	 * Accessor for the singleton instance of the {@link TracingCollections}
	 * 
	 * @return Return the single instance of the {@link TracingCollections}
	 */
	public static TracingCollections getInstance() {

		if (TracingCollections.instance == null) {
			TracingCollections.instance = new TracingCollections();
		}
		return TracingCollections.instance;
	}

	/**
	 * Accessor for the modified options model
	 * 
	 * @return Return the <code>ModifiedDebugOptions</code> model
	 */
	public ModifiedDebugOptions getModifiedDebugOptions() {
		return fModifiedDebugOptions;
	}

	/**
	 * Store a {@link TracingComponentDebugOption} element if it has not already been stored.
	 * 
	 * @param newDebugOption
	 *            The {@link TracingComponentDebugOption} to add to the collection.
	 */
	public void storeTracingDebugOption(final TracingComponentDebugOption newDebugOption) {
		if (newDebugOption != null) {
			List<TracingComponentDebugOption> debugOptions = this.fDebugOptionCollection.get(newDebugOption.getOptionPath());
			if (debugOptions == null) {
				// create the list of {@link TracingComponentDebugOption} elements
				debugOptions = new ArrayList<TracingComponentDebugOption>();
				fDebugOptionCollection.put(newDebugOption.getOptionPath(), debugOptions);
			}
			// add the newly created {@link TracingComponentDebugOption}
			if (!debugOptions.contains(newDebugOption)) {
				debugOptions.add(newDebugOption);
			}
		}
	}

	/**
	 * Access the list of debug options that contain the specified option-path value.
	 * 
	 * @param optionPath
	 *            The name of the option-path.
	 * @return An array of stored {@link TracingComponentDebugOption} elements that contain the option-path value.
	 */
	public TracingComponentDebugOption[] getTracingDebugOptions(final String optionPath) {
		List<TracingComponentDebugOption> debugOptions = null;
		if (optionPath != null) {
			debugOptions = fDebugOptionCollection.get(optionPath);
		}
		if (debugOptions == null) {
			debugOptions = Collections.emptyList();
		}
		return debugOptions.toArray(new TracingComponentDebugOption[debugOptions.size()]);
	}

	/**
	 * Access a {@link TracingComponent} from the internal collection. If a {@link TracingComponent} does not exist based on
	 * the content in the {@link IConfigurationElement} then a new {@link TracingComponent} will be created and stored.
	 * 
	 * @param element
	 *            The {@link IConfigurationElement} for a 'tracingComponent' extension.
	 * @return Returns a {@link TracingComponent} object based on the information in the specified
	 *         {@link IConfigurationElement}.
	 */
	public TracingComponent getTracingComponent(final IConfigurationElement element) {
		TracingComponent component = null;
		if (element != null) {
			String id = element.getAttribute(TracingConstants.TRACING_EXTENSION_ID_ATTRIBUTE);
			if (id != null) {
				component = fComponentCollection.get(id);
				if (component == null) {
					// create a new tracing component since one doesn't exist with this id
					component = new TracingComponent(element);
					fComponentCollection.put(id, component);
				} else {
					// A tracing component already exists with this id. Add the bundles provided by
					// the new component to the existing one.
					component.addBundles(element);
					// update the label (if necessary)
					final String newComponentLabel = element.getAttribute(TracingConstants.TRACING_EXTENSION_LABEL_ATTRIBUTE);
					if (newComponentLabel != null) {
						component.setLabel(newComponentLabel);
					}
				}
			}
		}
		return component;
	}

	/**
	 * Accessor for a {@link Properties} of debug options where the key of the {@link Properties} is the debug option
	 * path and the value of the {@link Properties} is the debug option value. If the debug options for a specific
	 * {@link Bundle} have not been accessed then it will attempt to read in all of the entries defined in the .options
	 * file for this bundle.
	 * 
	 * @param bundle
	 *            The {@link Bundle} to access the debug options defined for it.
	 * @return A {@link Properties} of debug options where the key of the {@link Properties} is the debug option path
	 *         and the value of the {@link Properties} is the debug option value.
	 */
	public Properties getDebugOptions(final Bundle bundle) {
		Properties results = null;
		if (bundle != null) {
			results = fBundleOptionsCollection.get(bundle);
			if (results == null) {
				// this bundle has not been processed yet - so do it now.
				results = TracingUtils.loadOptionsFromBundle(bundle);
				// and store the results
				fBundleOptionsCollection.put(bundle, results);
			}
		}
		return results;
	}

	/**
	 * Set the consumed state of the specified {@link Bundle}.
	 * 
	 * @param bundle
	 *            The {@link Bundle} to set the consumed state on.
	 * @param consumed
	 *            Is this bundle consumed by another tracing component?
	 */
	public void setBundleIsConsumed(final Bundle bundle, final boolean consumed) {
		if (bundle != null) {
			fBundleConsumedCollection.put(bundle, Boolean.valueOf(consumed));
		}
	}

	/**
	 * Accessor to find out if a specific bundle has been consumed by a tracing component.
	 * 
	 * @param bundle
	 *            The {@link Bundle} to check.
	 * @return Returns true if this bundle has been previous consumed by another tracing component. Otherwise, false is
	 *         returned.
	 */
	public boolean isBundleConsumed(final Bundle bundle) {
		boolean result = false;
		if (bundle != null) {
			Boolean isConsumed = fBundleConsumedCollection.get(bundle);
			if (isConsumed != null) {
				result = isConsumed.booleanValue();
			}
		}
		return result;
	}

	/**
	 * Store that the specified {@link Bundle} has been added to the specified {@link TracingComponent}.
	 * 
	 * @param component
	 *            The {@link TracingComponent} that includes the specified {@link Bundle}.
	 * @param bundle
	 *            The {@link Bundle} included in the specified {@link TracingComponent}.
	 */
	public void storeBundleInComponent(final TracingComponent component, final Bundle bundle) {
		if ((bundle != null) && (component != null)) {
			List<TracingComponent> components = fBundleComponentCollection.get(bundle);
			if (components == null) {
				components = new ArrayList<TracingComponent>();
				fBundleComponentCollection.put(bundle, components);
			}
			components.add(component);
		}
	}

	/**
	 * Accessor for an array of {@link TracingComponent} objects that contain the specified {@link Bundle}.
	 * 
	 * @param bundle
	 *            The {@link Bundle} used to locate the {@link TracingComponent} that include it.
	 * @return An array of {@link TracingComponent} objects that contain the specified {@link Bundle}.
	 */
	public TracingComponent[] getComponentsContainingBundle(final Bundle bundle) {
		List<TracingComponent> components = null;
		if (bundle != null) {
			components = fBundleComponentCollection.get(bundle);
		}
		if (components == null) {
			components = Collections.emptyList();
		}
		return components.toArray(new TracingComponent[components.size()]);
	}

	/**
	 * Clear the various collections
	 */
	public final void clear() {
		fComponentCollection.clear();
		fDebugOptionCollection.clear();
		fBundleOptionsCollection.clear();
		fBundleConsumedCollection.clear();
		fBundleComponentCollection.clear();
	}

	/** The debug options {@link ModifiedDebugOptions} added or removed */
	private ModifiedDebugOptions fModifiedDebugOptions = null;

	/** A collection of {@link TracingComponent} entries that are constructed for a specific {@link String} id. */
	private Map<String, TracingComponent> fComponentCollection = null;

	/**
	 * A collection of {@link TracingComponentDebugOption} entries that are constructed for a specific {@link String}
	 * option-path name.
	 */
	private Map<String, List<TracingComponentDebugOption>> fDebugOptionCollection = null;

	/** A collection of {@link Properties} entries which contain the various tracing strings for a specific {@link Bundle}. */
	private Map<Bundle, Properties> fBundleOptionsCollection = null;

	/**
	 * A collection of {@link Bundle} entries with a {@link Boolean} value to state if it is a consumed by any tracing
	 * component.
	 */
	private Map<Bundle, Boolean> fBundleConsumedCollection = null;

	/**
	 * A collection of {@link Bundle} entries with a {@link List} of the {@link TracingComponent}'s (i.e. components that
	 * contain this bundle)
	 */
	private Map<Bundle, List<TracingComponent>> fBundleComponentCollection = null;

	/** The singleton instance of this class */
	private static TracingCollections instance = null;
}