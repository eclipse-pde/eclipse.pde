/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
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
 *     Red Hat Inc. - <bundle...> in category.xml (copied from SiteFeature)
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SiteBundle extends VersionableObject implements ISiteBundle {
	private static final long serialVersionUID = 1L;
	private final Vector<ISiteCategory> fCategories = new Vector<>();

	@Override
	public boolean isValid() {
		for (int i = 0; i < fCategories.size(); i++) {
			ISiteCategory category = fCategories.get(i);
			if (!category.isValid()) {
				return false;
			}
		}
		return true;
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
	protected void parse(Node node) {
		super.parse(node);
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("category")) { //$NON-NLS-1$
				SiteCategory category = (SiteCategory) getModel().getFactory().createCategory(this);
				category.parse(child);
				category.setInTheModel(true);
				fCategories.add(category);
			}
		}
	}

	@Override
	protected void reset() {
		super.reset();
		fCategories.clear();
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<bundle"); //$NON-NLS-1$
		if (id != null) {
			writer.print(" id=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (version != null) {
			writer.print(" version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (label != null) {
			writer.print(" label=\"" + getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!fCategories.isEmpty()) {
			writer.println(">"); //$NON-NLS-1$
			String indent2 = indent + "   "; //$NON-NLS-1$
			for (int i = 0; i < fCategories.size(); i++) {
				ISiteCategory category = fCategories.get(i);
				category.write(indent2, writer);
			}
			writer.println(indent + "</bundle>"); //$NON-NLS-1$
		} else {
			writer.println("/>"); //$NON-NLS-1$
		}
	}

}
