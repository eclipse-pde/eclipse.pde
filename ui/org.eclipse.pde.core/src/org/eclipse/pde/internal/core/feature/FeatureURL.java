/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeatureURL;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FeatureURL extends FeatureObject implements IFeatureURL {
	private static final long serialVersionUID = 1L;
	private IFeatureURLElement fUpdate;
	private final Vector<IFeatureURLElement> fDiscoveries = new Vector<>();

	@Override
	public void addDiscovery(IFeatureURLElement discovery) throws CoreException {
		ensureModelEditable();
		fDiscoveries.add(discovery);
		((FeatureURLElement) discovery).setInTheModel(true);
		fireStructureChanged(discovery, IModelChangedEvent.INSERT);
	}

	@Override
	public void setUpdate(IFeatureURLElement update) throws CoreException {
		ensureModelEditable();
		if (fUpdate == update) {
			return;
		}
		if (fUpdate != null) {
			((FeatureURLElement) fUpdate).setInTheModel(false);
		}
		IFeatureURLElement oldValue = fUpdate;
		fUpdate = update;
		if (oldValue != null) {
			fireStructureChanged(oldValue, IModelChangedEvent.REMOVE);
		}
		if (update != null) {
			((FeatureURLElement) update).setInTheModel(true);
			fireStructureChanged(update, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public IFeatureURLElement[] getDiscoveries() {
		return fDiscoveries.toArray(new IFeatureURLElement[fDiscoveries.size()]);
	}

	@Override
	public IFeatureURLElement getUpdate() {
		return fUpdate;
	}

	@Override
	protected void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
				int urlType = -1;
				if (tag.equals("update")) { //$NON-NLS-1$
					urlType = IFeatureURLElement.UPDATE;
				} else if (tag.equals("discovery")) { //$NON-NLS-1$
					urlType = IFeatureURLElement.DISCOVERY;
				}
				if (urlType != -1) {
					IFeatureURLElement element = getModel().getFactory().createURLElement(this, urlType);
					((FeatureURLElement) element).parse(child);
					if (urlType == IFeatureURLElement.UPDATE) {
						((FeatureURLElement) element).setInTheModel(true);
						fUpdate = element;
					} else if (urlType == IFeatureURLElement.DISCOVERY) {
						((FeatureURLElement) element).setInTheModel(true);
						fDiscoveries.add(element);
					}
				}
			}
		}
	}

	@Override
	public void removeDiscovery(IFeatureURLElement discovery) throws CoreException {
		ensureModelEditable();
		fDiscoveries.remove(discovery);
		((FeatureURLElement) discovery).setInTheModel(false);
		fireStructureChanged(discovery, IModelChangedEvent.REMOVE);
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if (fUpdate == null && fDiscoveries.isEmpty()) {
			return;
		}
		writer.println();
		writer.println(indent + "<url>"); //$NON-NLS-1$
		String indent2 = indent + Feature.INDENT;
		if (fUpdate != null) {
			fUpdate.write(indent2, writer);
		}
		for (int i = 0; i < fDiscoveries.size(); i++) {
			IFeatureURLElement element = fDiscoveries.elementAt(i);
			element.write(indent2, writer);
		}
		writer.println(indent + "</url>"); //$NON-NLS-1$
	}
}
