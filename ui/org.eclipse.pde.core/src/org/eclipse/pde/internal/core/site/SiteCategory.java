/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.w3c.dom.Node;

public class SiteCategory extends SiteObject implements ISiteCategory {
	private static final long serialVersionUID = 1L;
	private String name;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategory#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid() {
		return name != null;
	}

	@Override
	protected void parse(Node node) {
		name = getNodeAttribute(node, "name"); //$NON-NLS-1$
	}

	@Override
	protected void reset() {
		name = null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategory#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<category"); //$NON-NLS-1$
		if (name != null) {
			writer.print(" name=\"" + SiteObject.getWritableString(name) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println("/>"); //$NON-NLS-1$
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	@Override
	public ISiteCategoryDefinition getDefinition() {
		ISite site = getSite();
		ISiteCategoryDefinition[] definitions = site.getCategoryDefinitions();
		for (ISiteCategoryDefinition def : definitions) {
			if (def.getName().equals(getName())) {
				return def;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append("{") //$NON-NLS-1$
				.append("name=").append(name) //$NON-NLS-1$
				.append("}"); //$NON-NLS-1$
		return builder.toString();
	}

}
