/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import java.io.PrintWriter;
import java.io.Serializable;

import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.ISiteFeature;

public class SiteFeatureAdapter implements Serializable, IWritable {

	private static final long serialVersionUID = 1L;

	String category;
	ISiteFeature feature;

	public SiteFeatureAdapter(String category, ISiteFeature feature) {
		this.category = category;
		this.feature = feature;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		feature.write(indent, writer);
	}

	/*
	 * For retaining selection in the tree, when modifying or moving features,
	 * SiteFeatureAdapter are equal if features are equal (same ID and version)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SiteFeatureAdapter) {
			SiteFeatureAdapter adapter = (SiteFeatureAdapter) obj;
			String id = feature.getId();
			String id2 = adapter.feature.getId();
			boolean sameFeature = id != null && id2 != null && id.equals(id2);
			if (sameFeature) {
				String version = feature.getVersion();
				String version2 = adapter.feature.getVersion();
				sameFeature = version != null && version2 != null && version.equals(version2);
			}
			boolean sameCategory = adapter.category != null && category != null ? adapter.category.equals(category) : true;
			return sameFeature && sameCategory;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		if (feature.getId() == null) {
			return super.hashCode();
		}
		int code = feature.getId().hashCode();
		if (feature.getVersion() != null) {
			code += feature.getVersion().hashCode();
		}
		if (category != null) {
			code += category.hashCode();
		}
		return code;
	}
}
