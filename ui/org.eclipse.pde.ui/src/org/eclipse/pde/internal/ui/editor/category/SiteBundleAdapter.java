/******************************************************************************* 
* Copyright (c) 2013 Red Hat Inc. and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Mickael Istria (Red Hat Inc.) - 383795: <bundle...> support
*   IBM Corporation - ongoing enhancements
******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.io.PrintWriter;
import java.io.Serializable;
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		bundle.write(indent, writer);
	}

	/*
	 * For retaining selectiong in the tree, when modyfing or moving features,
	 * SiteFeatureAdapter are equal if features are equal (same ID and version)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof SiteBundleAdapter) {
			SiteBundleAdapter adapter = (SiteBundleAdapter) obj;
			String id = bundle.getId();
			String id2 = adapter.bundle.getId();
			boolean sameBundle = id != null && id2 != null && id.equals(id2);
			if (sameBundle) {
				String version = bundle.getVersion();
				String version2 = adapter.bundle.getVersion();
				sameBundle = version != null && version2 != null && version.equals(version2);
			}
			boolean sameCategory = adapter.category != null && category != null ? adapter.category.equals(category) : true;
			return sameBundle && sameCategory;
		}
		return super.equals(obj);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (bundle.getId() == null) {
			return super.hashCode();
		}
		int code = bundle.getId().hashCode();
		if (bundle.getVersion() != null) {
			code += bundle.getVersion().hashCode();
		}
		if (category != null) {
			code += category.hashCode();
		}
		return code;
	}
}
