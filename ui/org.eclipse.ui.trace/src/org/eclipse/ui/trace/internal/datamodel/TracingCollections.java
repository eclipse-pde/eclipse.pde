/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
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
	public final static TracingCollections getInstance() {

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
	public final ModifiedDebugOptions getModifiedDebugOptions() {
		return this.fModifiedDebugOptions;
	}

	/**
	 * Store a {@link TracingComponentDebugOption} element if it has not already been stored.
	 * 
	 * @param newDebugOption
	 *            The {@link TracingComponentDebugOption} to add to the collection.
	 */
	public final void storeTracingDebugOption(final TracingComponentDebugOption newDebugOption) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, newDebugOption);
		}
		if (newDebugOption != null) {
			List<TracingComponentDebugOption> debugOptions = this.fDebugOptionCollection.get(newDebugOption.getOptionPath());
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Existing debug options for '" //$NON-NLS-1$
						+ newDebugOption.getOptionPath() + "': " + debugOptions); //$NON-NLS-1$
			}
			if (debugOptions == null) {
				// create the list of {@link TracingComponentDebugOption} elements
				debugOptions = new ArrayList<TracingComponentDebugOption>();
				this.fDebugOptionCollection.put(newDebugOption.getOptionPath(), debugOptions);
			}
			// add the newly created {@link TracingComponentDebugOption}
			if (!debugOptions.contains(newDebugOption)) {
				if (TracingUIActivator.DEBUG_MODEL) {
					TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Caching '" + newDebugOption + "'"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				debugOptions.add(newDebugOption);
			}
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Access the list of debug options that contain the specified option-path value.
	 * 
	 * @param optionPath
	 *            The name of the option-path.
	 * @return An array of stored {@link TracingComponentDebugOption} elements that contain the option-path value.
	 */
	public final TracingComponentDebugOption[] getTracingDebugOptions(final String optionPath) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, optionPath);
		}
		List<TracingComponentDebugOption> debugOptions = null;
		if (optionPath != null) {
			debugOptions = this.fDebugOptionCollection.get(optionPath);
		}
		if (debugOptions == null) {
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "There are no debug options for '" + optionPath + "' so returning an empty list."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			debugOptions = Collections.emptyList();
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, debugOptions);
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
	public final TracingComponent getTracingComponent(final IConfigurationElement element) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, element);
		}
		TracingComponent component = null;
		if (element != null) {
			String id = element.getAttribute(TracingConstants.TRACING_EXTENSION_ID_ATTRIBUTE);
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "tracing component id: " + id); //$NON-NLS-1$
			}
			if (id != null) {
				component = this.fComponentCollection.get(id);
				if (component == null) {
					if (TracingUIActivator.DEBUG_MODEL) {
						TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Creating a new tracing component for id: " + id); //$NON-NLS-1$
					}
					// create a new tracing component since one doesn't exist with this id
					component = new TracingComponent(element);
					this.fComponentCollection.put(id, component);
				} else {
					// A tracing component already exists with this id. Add the bundles provided by
					// the new component to the existing one.
					if (TracingUIActivator.DEBUG_MODEL) {
						TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "The tracing component for id '" + id + "' already exists.  The bundles for this new component are added to the existing component."); //$NON-NLS-1$ //$NON-NLS-2$
					}
					component.addBundles(element);
					// update the label (if necessary)
					final String newComponentLabel = element.getAttribute(TracingConstants.TRACING_EXTENSION_LABEL_ATTRIBUTE);
					if (newComponentLabel != null) {
						component.setLabel(newComponentLabel);
					}
				}
			}
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, component);
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
	public final Properties getDebugOptions(final Bundle bundle) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, bundle);
		}
		Properties results = null;
		if (bundle != null) {
			results = this.fBundleOptionsCollection.get(bundle);
			if (results == null) {
				// this bundle has not been processed yet - so do it now.
				if (TracingUIActivator.DEBUG_MODEL) {
					TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "The options for bundle '" + bundle + "' have not been processed."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				results = TracingUtils.loadOptionsFromBundle(bundle);
				// and store the results
				this.fBundleOptionsCollection.put(bundle, results);
			}
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, results);
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
	public final void setBundleIsConsumed(final Bundle bundle, final boolean consumed) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, new Object[] {bundle, Boolean.valueOf(consumed)});
		}
		if (bundle != null) {
			this.fBundleConsumedCollection.put(bundle, Boolean.valueOf(consumed));
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
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
	public final boolean isBundleConsumed(final Bundle bundle) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, bundle);
		}
		boolean result = false;
		if (bundle != null) {
			Boolean isConsumed = this.fBundleConsumedCollection.get(bundle);
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Checking the collection if the bundle '" + bundle.getSymbolicName() + "' is consumed... result: " + isConsumed); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (isConsumed != null) {
				result = isConsumed.booleanValue();
			}
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, Boolean.valueOf(result));
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
	public final void storeBundleInComponent(final TracingComponent component, final Bundle bundle) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, new Object[] {component, bundle});
		}
		if ((bundle != null) && (component != null)) {
			List<TracingComponent> components = this.fBundleComponentCollection.get(bundle);
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Existing components in collection: " + components); //$NON-NLS-1$
			}
			if (components == null) {
				components = new ArrayList<TracingComponent>();
				this.fBundleComponentCollection.put(bundle, components);
			}
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Adding component to collection: " + component); //$NON-NLS-1$
			}
			components.add(component);
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Accessor for an array of {@link TracingComponent} objects that contain the specified {@link Bundle}.
	 * 
	 * @param bundle
	 *            The {@link Bundle} used to locate the {@link TracingComponent} that include it.
	 * @return An array of {@link TracingComponent} objects that contain the specified {@link Bundle}.
	 */
	public final TracingComponent[] getComponentsContainingBundle(final Bundle bundle) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, bundle);
		}
		List<TracingComponent> components = null;
		if (bundle != null) {
			components = this.fBundleComponentCollection.get(bundle);
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Existing components in collection: " + components); //$NON-NLS-1$
			}
		}
		if (components == null) {
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "There are no components in the collection so creating an empty list."); //$NON-NLS-1$
			}
			components = Collections.emptyList();
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, components);
		}
		return components.toArray(new TracingComponent[components.size()]);
	}

	/**
	 * Clear the various collections
	 */
	public final void clear() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);
		}
		this.fComponentCollection.clear();
		this.fDebugOptionCollection.clear();
		this.fBundleOptionsCollection.clear();
		this.fBundleConsumedCollection.clear();
		this.fBundleComponentCollection.clear();
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
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

	/** Trace object for this bundle */
	private final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();

	/** The singleton instance of this class */
	private static TracingCollections instance = null;
}