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
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ISourceObject;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelProvider;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;

public abstract class PluginObject extends PlatformObject implements IPluginObject, ISourceObject, Serializable, IWritableDelimiter {
	private static final long serialVersionUID = 1L;

	protected String fName;

	private transient String fTranslatedName;
	private transient IPluginObject fParent;
	private transient ISharedPluginModel fModel;
	private transient boolean fInTheModel;

	protected int fStartLine = 1;

	public PluginObject() {
	}

	@Override
	public boolean isValid() {
		return true;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!fModel.isEditable()) {
			throwCoreException(PDECoreMessages.PluginObject_readOnlyChange);
		}
	}

	@Override
	public void setInTheModel(boolean value) {
		fInTheModel = value;
	}

	@Override
	public boolean isInTheModel() {
		return fInTheModel;
	}

	protected void firePropertyChanged(String property, Object oldValue, Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	protected void firePropertyChanged(IPluginObject object, String property, Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(IPluginObject child, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent((IModelChangeProvider) model, changeType, new Object[] {child}, null);
			fireModelChanged(e);
		}
	}

	protected void fireStructureChanged(IPluginObject[] children, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent((IModelChangeProvider) model, changeType, children, null);
			fireModelChanged(e);
		}
	}

	protected void fireModelChanged(IModelChangedEvent e) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider provider) {
			provider.fireModelChanged(e);
		}
	}

	@Override
	public ISharedPluginModel getModel() {
		return fModel;
	}

	@Override
	public IPluginModelBase getPluginModel() {
		if (fModel instanceof IBundlePluginModelProvider) {
			return ((IBundlePluginModelProvider) fModel).getBundlePluginModel();
		}

		return fModel instanceof IPluginModelBase ? (IPluginModelBase) fModel : null;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public String getTranslatedName() {
		if (fTranslatedName != null && !fModel.isEditable()) {
			return fTranslatedName;
		}
		if (fTranslatedName == null && fName != null && fModel != null) {
			fTranslatedName = fModel.getResourceString(fName);
		}
		return fTranslatedName;
	}

	String getNodeAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null) {
			return attribute.getNodeValue();
		}
		return null;
	}

	@Override
	public IPluginObject getParent() {
		return fParent;
	}

	@Override
	public IPluginBase getPluginBase() {
		IPluginModelBase pluginModel = getPluginModel();
		return pluginModel != null ? pluginModel.getPluginBase() : null;
	}

	@Override
	public String getResourceString(String key) {
		return fModel.getResourceString(key);
	}

	static boolean isNotEmpty(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i)) == false) {
				return true;
			}
		}
		return false;
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		}
	}

	public void setModel(ISharedPluginModel model) {
		this.fModel = model;
		fTranslatedName = null;
	}

	@Override
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		String oldValue = this.fName;
		this.fName = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	public void setParent(IPluginObject parent) {
		this.fParent = parent;
	}

	protected void throwCoreException(String message) throws CoreException {
		throw new CoreException(Status.error(message));
	}

	@Override
	public String toString() {
		String result = null;
		if (fName != null) {
			result = fName;
		}
		if ((result == null || result.indexOf('%') >= 0) && fModel != null) {
			result = fModel.toString();
		}
		if (result != null) {
			return result;
		}
		return super.toString();
	}

	public Vector<String> addComments(Node node, Vector<String> result) {
		for (Node prev = node.getPreviousSibling(); prev != null; prev = prev.getPreviousSibling()) {
			if (prev.getNodeType() == Node.TEXT_NODE) {
				continue;
			}
			if (prev instanceof Comment) {
				String comment = prev.getNodeValue();
				if (result == null) {
					result = new Vector<>();
				}
				result.add(0, comment);
			} else {
				break;
			}
		}
		return result;
	}

	protected boolean stringEqualWithNull(String a, String b) {
		return a == null && b == null || a != null && b != null && a.equals(b);
	}

	public String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}

	@Override
	public int getStartLine() {
		return fStartLine;
	}

	@Override
	public int getStopLine() {
		return fStartLine;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IPluginModelBase.class)) {
			return (T) getPluginModel();
		}
		return super.getAdapter(adapter);
	}

	/**
	 * @param model
	 * @param parent
	 */
	public void reconnect(ISharedPluginModel model, IPluginObject parent) {
		// Transient Field:  In The Model
		fInTheModel = false;
		// Transient Field:  Model
		fModel = model;
		// Transient Field:  Parent
		fParent = parent;
		// Transient Field:  Translated Name
		fTranslatedName = null;
	}

	@Override
	public void writeDelimeter(PrintWriter writer) {
		// NO-OP
		// Child classes to override
	}

}
