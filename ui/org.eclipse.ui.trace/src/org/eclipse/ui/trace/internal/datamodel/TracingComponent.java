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
import org.eclipse.ui.trace.internal.Messages;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.utils.DebugOptionsHandler;
import org.eclipse.ui.trace.internal.utils.TracingConstants;
import org.osgi.framework.Bundle;

/**
 * A tracing component contains all of the information retrieved via the 'tracingComponent' extension point.
 */
public class TracingComponent extends AbstractTracingNode {

	/**
	 * Constructor for a new {@link TracingComponent}
	 * 
	 * @param element
	 *            A non-null configuration element
	 */
	public TracingComponent(IConfigurationElement element) {
		super();
		assert (element != null);
		// set the id
		id = element.getAttribute(TracingConstants.TRACING_EXTENSION_ID_ATTRIBUTE);
		// set the label
		setLabel(element.getAttribute(TracingConstants.TRACING_EXTENSION_LABEL_ATTRIBUTE));
		// set the bundles
		bundles = new ArrayList<Bundle>();
		addBundles(element);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TracingComponent)) {
			return false;
		}
		TracingComponent other = (TracingComponent) obj;
		if (getId() == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!getId().equals(other.getId())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {

		final StringBuilder builder = new StringBuilder();
		builder.append("TracingComponent (id="); //$NON-NLS-1$
		builder.append(id);
		builder.append(", label="); //$NON-NLS-1$
		builder.append(getLabel());
		builder.append(", bundle count="); //$NON-NLS-1$
		builder.append(bundles.size());
		builder.append(")"); //$NON-NLS-1$
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.trace.internal.datamodel.AbstractTracingNode#isEnabled()
	 */
	public boolean isEnabled() {

		// a tracing component is enabled if all of its children are enabled
		boolean isEnabled = true;
		final TracingComponentDebugOption[] componentChildren = this.getChildren();
		if (componentChildren.length > 0) {
			for (int i = 0; i < componentChildren.length; i++) {
				if (!componentChildren[i].isEnabled()) {
					isEnabled = false;
					break;
				}
			}
		} else {
			isEnabled = false;
		}
		return isEnabled;
	}

	@Override
	protected void populateChildren() {
		// Iterate over each bundle and populate the list of {@link TracingComponentDebugOption} objects.
		if (bundles.size() > 0) {
			// get all debug options (this ensures that the disabled debug options are used when populating)
			final Map<?, ?> currentDebugOptions = DebugOptionsHandler.getDebugOptions().getOptions();
			final Iterator<Bundle> bundleIterator = bundles.iterator();
			while (bundleIterator.hasNext()) {
				Bundle bundle = bundleIterator.next();
				Properties options = TracingCollections.getInstance().getDebugOptions(bundle);
				if (options.size() > 0) {
					// this bundle has debug options - loop over each one and build a TracingComponentDebugOption for it
					Iterator<Map.Entry<Object, Object>> optionsIterator = options.entrySet().iterator();
					while (optionsIterator.hasNext()) {
						Map.Entry<Object, Object> option = optionsIterator.next();
						// check to see if this debug option already exists in the cache
						String key = (String) option.getKey();
						String value = (String) option.getValue();
						final String debugOptionValue = (String) currentDebugOptions.get(key);
						String finalValue = null;
						if (debugOptionValue != null) {
							// This entry is already in the debug options - so use its value.
							finalValue = debugOptionValue;
						} else {
							// create a TracingComponentDebugOption for this entry
							final TracingComponentDebugOption[] debugOptions = TracingCollections.getInstance().getTracingDebugOptions(key);
							if (debugOptions.length > 0) {
								// An existing tracing debug option has already been created but it does not
								// exist in the debug options (yet). Use the value of this existing debug option
								// despite what the .option file may say to ensure the initial value of all debug
								// options are the same.
								finalValue = debugOptions[0].getOptionPathValue();
							} else {
								// An existing tracing debug option does not exist nor does it exist in the
								// debug options (yet). Use the value read in from the .options file.
								finalValue = value;
							}
						}
						// create the TracingComponentDebugOption object
						final TracingComponentDebugOption newDebugOption = new TracingComponentDebugOption(key, finalValue);
						newDebugOption.setParent(this);
						// and cache it
						TracingCollections.getInstance().storeTracingDebugOption(newDebugOption);
					}
				}
			}
		}
	}

	@Override
	public TracingComponentDebugOption[] getChildren() {

		final TracingNode[] componentChildren = super.getChildren();
		// each element will be a TracingComponentDebugOption
		final TracingComponentDebugOption[] debugOptions = new TracingComponentDebugOption[componentChildren.length];
		for (int i = 0; i < componentChildren.length; i++) {
			debugOptions[i] = (TracingComponentDebugOption) componentChildren[i];
		}
		return debugOptions;
	}

	/**
	 * Add a set of bundles that match the specified name to the list of bundles.
	 * 
	 * @param name
	 *            A name of a bundle to add. It could be a regular expression.
	 */
	private void addBundle(final String name, final boolean consumed, final Bundle[] allBundles) {
		if (name != null) {
			for (int bundleIndex = 0; bundleIndex < allBundles.length; bundleIndex++) {
				String symbolicName = allBundles[bundleIndex].getSymbolicName();
				if ((symbolicName != null) && symbolicName.matches(name)) {
					if (!TracingCollections.getInstance().isBundleConsumed(allBundles[bundleIndex])) {
						// this bundle has not been consumed by any other component yet so include it here.
						bundles.add(allBundles[bundleIndex]);
						// cache that this bundle is in this component
						TracingCollections.getInstance().storeBundleInComponent(this, allBundles[bundleIndex]);
						// check to see if this bundle is being consumed (meaning that this bundle should not appear in
						// any other tracing component
						if (consumed) {
							// tell the cache that this bundle is consumed
							TracingCollections.getInstance().setBundleIsConsumed(allBundles[bundleIndex], consumed);
							// remove this bundle from any other tracing component that is including it (except this
							// component)
							TracingComponent[] components = TracingCollections.getInstance().getComponentsContainingBundle(allBundles[bundleIndex]);
							for (int componentIndex = 0; componentIndex < components.length; componentIndex++) {
								if (!components[componentIndex].equals(this)) {
									components[componentIndex].removeBundle(allBundles[bundleIndex]);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Add the bundles found in the specified {@link IConfigurationElement} instance to this {@link TracingComponent}.
	 * 
	 * @param element
	 *            A non-null {@link IConfigurationElement} instance of the 'tracingComponent'
	 */
	public void addBundles(final IConfigurationElement element) {
		assert (element != null);
		final IConfigurationElement[] componentChildren = element.getChildren();
		final Bundle[] installedBundles = TracingUIActivator.getDefault().getBundle().getBundleContext().getBundles();
		for (int i = 0; i < componentChildren.length; i++) {
			if (componentChildren[i].getName().equals(TracingConstants.TRACING_EXTENSION_BUNDLE_ATTRIBUTE)) {
				String name = componentChildren[i].getAttribute(TracingConstants.TRACING_EXTENSION_BUNDLE_NAME_ATTRIBUTE);
				boolean consumed = Boolean.valueOf(componentChildren[i].getAttribute(TracingConstants.TRACING_EXTENSION_BUNDLE_CONSUMED_ATTRIBUTE)).booleanValue();
				this.addBundle(name, consumed, installedBundles);
			}
		}
	}

	/**
	 * Add a set of bundles that match the specified name to the list of bundles.
	 * 
	 * @param name
	 *            A name of a bundle to add. It could be a regular expression.
	 * @param isConsumed
	 *            Is this bundle consumed by this {@link TracingComponent}
	 */
	public void addBundle(final String name, final boolean isConsumed) {
		if (name != null) {
			final Bundle[] installedBundles = TracingUIActivator.getDefault().getBundle().getBundleContext().getBundles();
			addBundle(name, isConsumed, installedBundles);
		}
	}

	/**
	 * Removes a specific bundle from from the list of bundles
	 * 
	 * @param bundle
	 *            A {@link Bundle} to remove from the set of bundles belonging to this {@link TracingComponent}
	 */
	public void removeBundle(final Bundle bundle) {
		if (bundle != null) {
			bundles.remove(bundle);
		}
	}

	/**
	 * A {@link TracingComponent} is consumed if it contributes no bundles.
	 * 
	 * @return Returns true if this {@link TracingComponent} contains no bundles; Otherwise, false is returned.
	 */
	public boolean isConsumed() {
		return this.bundles.size() <= 0;
	}

	/**
	 * Accessor for the id value for this {@link TracingComponent}
	 * 
	 * @return The id value for this {@link TracingComponent}
	 */
	public String getId() {
		return id;
	}

	/**
	 * Accessor for an array of the {@link Bundle} objects for which this {@link TracingComponent} provides tracing
	 * options.
	 * 
	 * @return A {@link Bundle} array
	 */
	public Bundle[] getBundles() {
		return bundles.toArray(new Bundle[this.bundles.size()]);
	}

	@Override
	public String getLabel() {
		String componentLabel = label;
		if (componentLabel == null) {
			componentLabel = Messages.missingLabelValue;
		}
		return componentLabel;
	}

	/** The 'id' of this {@link TracingComponent} */
	private String id = null;

	/** A list of bundles included in this {@link TracingComponent} */
	private List<Bundle> bundles = null;
}