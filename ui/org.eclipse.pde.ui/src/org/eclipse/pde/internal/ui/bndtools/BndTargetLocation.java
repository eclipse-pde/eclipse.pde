/*******************************************************************************
 * Copyright (c) 2017, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Elias N Vasylenko <eliasvasylenko@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aqute.biz> - ongoing enhancements
 *     Juergen Albert <j.albert@data-in-motion.biz> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import java.io.StringWriter;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.bndtools.core.ui.icons.Icons;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.AbstractBundleContainer;
import org.eclipse.pde.internal.core.target.TargetDefinition;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import aQute.lib.xml.XML;

@SuppressWarnings("deprecation")
public abstract class BndTargetLocation extends AbstractBundleContainer
	implements ITargetLocationHandler, ILabelProvider {
	static final String		PLUGIN_ID							= "bndtools.pde";

	static final String		MESSAGE_UNABLE_TO_LOCATE_WORKSPACE	= "Unable to locate the Bnd workspace";
	static final String		MESSAGE_UNABLE_TO_RESOLVE_BUNDLES	= "Unable to resolve bundles";

	static final String		ELEMENT_LOCATION					= "location";
	static final String		ATTRIBUTE_LOCATION_TYPE				= "type";

	private final String	type;
	private final Image		containerIcon;

	public BndTargetLocation(String type, String containerIconName) {
		this.type = Objects.requireNonNull(type);
		this.containerIcon = Icons.image("/icons/" + containerIconName);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ILabelProvider.class) {
			return (T) this;

		} else {
			return super.getAdapter(adapter);
		}
	}


	@Override
	public boolean canEdit(ITargetDefinition targetLocation, TreePath treePath) {
		return targetLocation == this;
	}

	@Override
	public boolean canUpdate(ITargetDefinition targetLocation, TreePath treePath) {
		return targetLocation == this;
	}

	@Override
	public IStatus update(ITargetDefinition target, TreePath[] treePaths, IProgressMonitor monitor) {
		clearResolutionStatus();
		return Status.OK_STATUS;
	}

	@Override
	public Image getImage(Object element) {
		return containerIcon;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
		throws CoreException {
		if (definition instanceof TargetDefinition) {
			return ((TargetDefinition) definition).resolveFeatures(getLocation(false), monitor);
		}
		return new TargetFeature[] {};
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public String serialize() {
		Document document;
		try {
			DocumentBuilder docBuilder = XML.newDocumentBuilderFactory()
				.newDocumentBuilder();
			document = docBuilder.newDocument();

			Element locationElement = document.createElement(ELEMENT_LOCATION);
			locationElement.setAttribute(ATTRIBUTE_LOCATION_TYPE, getType());
			document.appendChild(locationElement);

			serialize(document, locationElement);

			StreamResult result = new StreamResult(new StringWriter());
			Transformer transformer = XML.newTransformerFactory()
				.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.transform(new DOMSource(document), result);
			return result.getWriter()
				.toString();
		} catch (Exception e) {
			PDECore.log(e);
			return null;
		}
	}

	protected abstract void serialize(Document document, Element locationElement);
}
