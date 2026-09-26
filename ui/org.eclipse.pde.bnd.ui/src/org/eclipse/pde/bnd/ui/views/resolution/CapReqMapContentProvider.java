/*******************************************************************************
 * Copyright (c) 2014, 2023 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Rueger <chrisrueger@gmail.com> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.views.resolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.bnd.ui.model.resolution.RequirementWithChildren;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;

import aQute.bnd.unmodifiable.Sets;
import aQute.libg.glob.Glob;

public class CapReqMapContentProvider implements ITreeContentProvider {

	private static final Object[]		EMPTY		= new Object[0];

	private static final Set<String>	NAMESPACES	= Sets.of(BundleNamespace.BUNDLE_NAMESPACE,
		IdentityNamespace.IDENTITY_NAMESPACE, HostNamespace.HOST_NAMESPACE, PackageNamespace.PACKAGE_NAMESPACE);

	private final Comparator<Object>	comparator	= new CapReqComparator();

	private String						wildcardFilter	= null;

	@Override
	public void dispose() {}

	@Override
	public void inputChanged(Viewer viewer, Object oldValue, Object newValue) {}

	@Override
	public Object[] getElements(Object input) {
		List<Object[]> arrays = new LinkedList<>();

		@SuppressWarnings("unchecked")
		Map<String, List<Object>> map = (Map<String, List<Object>>) input;

		// Add entries for our preferred ordering of namespaces
		for (String namespace : NAMESPACES) {
			List<Object> listForNs = map.get(namespace);
			if (listForNs != null) {
				Object[] array = listForNs.toArray();
				Arrays.sort(array, comparator);
				arrays.add(array);
			}
		}

		// Now the rest in any order
		for (Entry<String, List<Object>> entry : map.entrySet()) {
			// Skip if namespace is a member of the namespaces we have already
			// added.
			if (NAMESPACES.contains(entry.getKey())) {
				continue;
			}

			List<Object> listForNs = entry.getValue();
			Object[] array = listForNs.toArray();
			Arrays.sort(array, comparator);
			arrays.add(array);
		}

		return filter(flatten(arrays));
	}

	private Object[] flatten(List<Object[]> arrays) {
		// Iterate over once to count the lengths
		int length = 0;
		for (Object[] array : arrays) {
			length += array.length;
		}
		Object[] result = new Object[length];

		// Iterate again to flatten out the arrays
		int position = 0;
		for (Object[] array : arrays) {
			System.arraycopy(array, 0, result, position, array.length);
			position += array.length;
		}
		return result;
	}

	@Override
	public Object getParent(Object object) {
		return null;
	}

	@Override
	public boolean hasChildren(Object object) {
		RequirementWithChildren rw = Adapters.adapt(object, RequirementWithChildren.class);
		if (rw != null) {
			return !rw.getChildren().isEmpty();
		}
		return false;
	}

	@Override
	public Object[] getChildren(Object parent) {
		RequirementWithChildren rw = Adapters.adapt(parent, RequirementWithChildren.class);
		if (rw != null) {
			return rw.getChildren().toArray();
		}
		return EMPTY;
	}

	public void setFilter(String filterString) {
		if (filterString == null || filterString.length() == 0 || filterString.trim()
			.equals("*")) {
			wildcardFilter = null;
		} else {
			wildcardFilter = "*" + filterString.trim() + "*";
		}

	}

	private Object[] filter(Object[] array) {
		List<Object> filteredResults = new ArrayList<>();
		if (wildcardFilter == null || wildcardFilter.equals("*") || wildcardFilter.equals("")) {
			return array;
		} else {
			String[] split = wildcardFilter.split("\\s+");
			Glob globs[] = new Glob[split.length];
			for (int i = 0; i < split.length; i++) {
				globs[i] = new Glob(split[i].toLowerCase());
			}

			// parallel search
			Arrays.stream(array).parallel().forEach(obj -> {

				if (obj instanceof RequirementWrapper rw) {

					for (Glob g : globs) {
							if (g.matcher(RequirementWrapperLabelProvider.tooltipText(rw)
								.toLowerCase())
							.find()) {
							filteredResults.add(obj);
							return;
						}
					}
				}
				else if (obj instanceof Capability cap) {

					for (Glob g : globs) {
							if (g.matcher(CapabilityLabelProvider.tooltipText(cap)
								.toLowerCase())
							.find()) {
							filteredResults.add(obj);
							return;
						}
					}
				}

			});

		}

		// sort because parallel search causes random ordering
		filteredResults.sort(comparator);

		return filteredResults.toArray();
	}

}
