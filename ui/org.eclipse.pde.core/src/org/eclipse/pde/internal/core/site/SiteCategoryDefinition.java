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
 *     Red Hat Inc. - Support for nested category
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SiteCategoryDefinition extends SiteObject implements ISiteCategoryDefinition {

	private static final long serialVersionUID = 1L;
	private String name;
	private ISiteDescription description;
	private final Vector<ISiteCategory> fCategories = new Vector<>();

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isValid() {
		return name != null && getLabel() != null;
	}

	@Override
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	@Override
	public ISiteDescription getDescription() {
		return description;
	}

	@Override
	public void setDescription(ISiteDescription description) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_DESCRIPTION, oldValue, description);
	}

	@Override
	protected void reset() {
		super.reset();
		name = null;
		description = null;
	}

	@Override
	protected void parse(Node node) {
		super.parse(node);
		name = getNodeAttribute(node, "name"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("description")) { //$NON-NLS-1$
				description = getModel().getFactory().createDescription(this);
				((SiteDescription) description).parse(child);
				((SiteDescription) description).setInTheModel(true);
				break;
			} else if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("category")) { //$NON-NLS-1$
				SiteCategory category = (SiteCategory) getModel().getFactory().createCategory(this);
				category.parse(child);
				category.setInTheModel(true);
				fCategories.add(category);
			}
		}
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_DESCRIPTION) && newValue instanceof ISiteDescription) {
			setDescription((ISiteDescription) newValue);
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<category-def"); //$NON-NLS-1$
		if (name != null) {
			writer.print(" name=\"" + SiteObject.getWritableString(name) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (label != null) {
			writer.print(" label=\"" + SiteObject.getWritableString(label) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		boolean hasChildrenElements = description != null || !this.fCategories.isEmpty();
		if (hasChildrenElements) {
			writer.println(">"); //$//$NON-NLS-1$
			for (ISiteCategory category : fCategories) {
				category.write(indent + Site.INDENT, writer);
			}
			if (description != null) {
				description.write(indent + Site.INDENT, writer);
			}
			writer.println(indent + "</category-def>"); //$NON-NLS-1$
		} else {
			writer.println("/>"); //$NON-NLS-1$
		}
	}

	@Override
	public void addCategories(ISiteCategory[] newCategories) throws CoreException {
		ensureModelEditable();
		for (ISiteCategory category : newCategories) {
			((SiteCategory) category).setInTheModel(true);
			fCategories.add(category);
		}
		fireStructureChanged(newCategories, IModelChangedEvent.INSERT);
	}

	@Override
	public void removeCategories(ISiteCategory[] newCategories) throws CoreException {
		ensureModelEditable();
		for (ISiteCategory category : newCategories) {
			((SiteCategory) category).setInTheModel(false);
			fCategories.remove(category);
		}
		fireStructureChanged(newCategories, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getCategories()
	 */
	@Override
	public ISiteCategory[] getCategories() {
		return fCategories.toArray(new ISiteCategory[fCategories.size()]);
	}

	@Override
	public String toString() {
		String separator = ", "; //$NON-NLS-1$
		StringBuilder builder = new StringBuilder();
		builder.append(SiteCategoryDefinition.class.getSimpleName()).append("{") //$NON-NLS-1$
				.append("name=").append(name).append(separator) //$NON-NLS-1$
				.append("label=").append(label).append(separator) //$NON-NLS-1$
				.append("categories=").append(fCategories.size()) //$NON-NLS-1$
				.append("}"); //$NON-NLS-1$
		return builder.toString();
	}

}
