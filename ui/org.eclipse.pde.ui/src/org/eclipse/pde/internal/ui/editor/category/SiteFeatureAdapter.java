/*******************************************************************************
 * Copyright (c) 2009, 2019 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 547222
 ******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
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
	 * For retaining selection in the tree, when modyfing or moving features,
	 * SiteFeatureAdapter are equal if features are equal
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SiteFeatureAdapter) {
			SiteFeatureAdapter adapter = (SiteFeatureAdapter) obj;
			return Objects.equals(feature, adapter.feature);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return feature.hashCode();
	}
}
