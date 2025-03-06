/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com>  - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.templating;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bndtools.templating.Category;
import org.bndtools.templating.Template;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class LatestTemplateFilter extends ViewerFilter {

	@Override
	public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
		Object[] result;
		if (parent instanceof Category) {
			Map<String, Template> selected = new LinkedHashMap<>(); // Preserves
																	// the order
																	// of names,
																	// as they
																	// were
																	// already
																	// sorted by
																	// the
																	// content
																	// provider.
			for (Object element : elements) {
				Template template = (Template) element;
				Template existing = selected.get(template.getName());

				if (existing == null) {
					// no selected template for this name -> add
					selected.put(template.getName(), template);
				} else if (template.getVersion()
					.compareTo(existing.getVersion()) > 0) {
					// existing selected template for this name is lower ->
					// replace
					selected.put(template.getName(), template);
				}
			}
			result = selected.values()
				.toArray();
		} else {
			result = elements;
		}
		return result;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// not invoked
		throw new UnsupportedOperationException();
	}

}
