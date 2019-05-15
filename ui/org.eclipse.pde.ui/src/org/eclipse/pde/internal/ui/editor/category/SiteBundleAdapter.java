/*******************************************************************************
.
. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Mickael Istria (Red Hat Inc.) - 383795: <bundle...> support
*   IBM Corporation - ongoing enhancements
******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Objects;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.ISiteBundle;

public class SiteBundleAdapter implements Serializable, IWritable {

	private static final long serialVersionUID = 1L;

	String category;
	ISiteBundle bundle;

	public SiteBundleAdapter(String category, ISiteBundle bundle) {
		this.category = category;
		this.bundle = bundle;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		bundle.write(indent, writer);
	}

	/*
	 * For retaining selectiong in the tree, when modyfing or moving features,
	 * SiteFeatureAdapter are equal if features are equal (same ID and version)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SiteBundleAdapter) {
			SiteBundleAdapter adapter = (SiteBundleAdapter) obj;
			return Objects.equals(bundle, adapter.bundle);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return bundle.hashCode();
	}
}
