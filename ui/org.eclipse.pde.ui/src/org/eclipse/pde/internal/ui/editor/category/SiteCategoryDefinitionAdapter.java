/*******************************************************************************
 *  Copyright (c) 2016 Red Hat Inc. and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Red Hat Inc. - Copied from SiteFeatureAdapter
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.io.PrintWriter;
import java.io.Serializable;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;

public class SiteCategoryDefinitionAdapter implements Serializable, IWritable {

	private static final long serialVersionUID = 1L;

	String parentCategory;
	ISiteCategoryDefinition category;

	public SiteCategoryDefinitionAdapter(String parentCategory, ISiteCategoryDefinition category) {
		this.parentCategory = parentCategory;
		this.category = category;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		category.write(indent, writer);
	}

	/*
	 * For retaining selection in the tree, when modifying or moving features,
	 * SiteFeatureAdapter are equal if features are equal (same ID and version)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SiteCategoryDefinitionAdapter) {
			SiteCategoryDefinitionAdapter otherAdapter = (SiteCategoryDefinitionAdapter) obj;
			String id = category.getName();
			String id2 = otherAdapter.category.getName();
			boolean sameCategory = id != null && id2 != null && id.equals(id2);
			boolean sameParentCategory = parentCategory == null && otherAdapter.parentCategory == null;
			if (!sameParentCategory) {
				// first, second or both are nested categories
				// if both are nested then compare by category id, if one is top
				// level and other inner then they can not be equal
				sameParentCategory = otherAdapter.parentCategory != null
						&& (parentCategory != null ? parentCategory.equals(otherAdapter.parentCategory) : true);
			}

			return sameCategory && sameParentCategory;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		if (category.getName() == null) {
			return super.hashCode();
		}
		int code = category.getName().hashCode();
		if (parentCategory != null) {
			code += parentCategory.hashCode();
		}
		return code;
	}

	@Override
	public String toString() {
		String separator = ", "; //$NON-NLS-1$
		StringBuilder builder = new StringBuilder();
		builder.append(SiteCategoryDefinitionAdapter.class.getSimpleName()).append("{") //$NON-NLS-1$
				.append("name=").append(category == null ? null : category.getName()).append(separator) //$NON-NLS-1$
				.append("parentCategory=").append(parentCategory)//$NON-NLS-1$
				.append("}"); //$NON-NLS-1$
		return builder.toString();
	}
}
