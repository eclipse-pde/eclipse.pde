/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureObject;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.w3c.dom.Node;

public abstract class FeatureObject extends PlatformObject implements IFeatureObject {

	private static final long serialVersionUID = 1L;
	transient IFeatureModel model;
	transient IFeatureObject parent;
	protected String label;
	boolean inTheModel;

	void setInTheModel(boolean value) {
		inTheModel = value;
	}

	@Override
	public boolean isInTheModel() {
		return inTheModel;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException(PDECoreMessages.FeatureObject_readOnlyChange);
		}
	}

	protected void firePropertyChanged(String property, Object oldValue, Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	protected void firePropertyChanged(IFeatureObject object, String property, Object oldValue, Object newValue) {
		if (model.isEditable()) {
			IModelChangeProvider provider = model;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(IFeatureObject child, int changeType) {
		fireStructureChanged(new IFeatureObject[] {child}, changeType);
	}

	protected void fireStructureChanged(IFeatureObject[] children, int changeType) {
		IFeatureModel model = getModel();
		if (model.isEditable()) {
			IModelChangeProvider provider = model;
			provider.fireModelChanged(new ModelChangedEvent(provider, changeType, children, null));
		}
	}

	@Override
	public IFeature getFeature() {
		return model.getFeature();
	}

	@Override
	public String getLabel() {
		if (label == null) {
			return ""; //$NON-NLS-1$
		}
		return label;
	}

	@Override
	public String getTranslatableLabel() {
		if (label == null) {
			return ""; //$NON-NLS-1$
		}
		return model.getResourceString(label);
	}

	@Override
	public IFeatureModel getModel() {
		return model;
	}

	String getNodeAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null) {
			return attribute.getNodeValue();
		}
		return null;
	}

	int getIntegerAttribute(Node node, String name) {
		String value = getNodeAttribute(node, name);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}

	boolean getBooleanAttribute(Node node, String name) {
		String value = getNodeAttribute(node, name);
		if (value != null) {
			return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return false;
	}

	protected String getNormalizedText(String source) {
		String result = source.replace('\t', ' ');
		result = result.trim();

		return result;
	}

	@Override
	public IFeatureObject getParent() {
		return parent;
	}

	protected void parse(Node node) {
		label = getNodeAttribute(node, "label"); //$NON-NLS-1$
	}

	protected void reset() {
		label = null;
	}

	@Override
	public void setLabel(String newLabel) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.label;
		label = newLabel;
		firePropertyChanged(P_LABEL, oldValue, newLabel);
	}

	protected void throwCoreException(String message) throws CoreException {
		Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, message, null);
		CoreException ce = new CoreException(status);
		ce.fillInStackTrace();
		throw ce;
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_LABEL)) {
			setLabel(newValue != null ? newValue.toString() : null);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
	}

	public void setModel(IFeatureModel model) {
		this.model = model;
	}

	public void setParent(IFeatureObject parent) {
		this.parent = parent;
	}

	protected String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}
}
