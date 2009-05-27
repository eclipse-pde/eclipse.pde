/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Vector;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
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

	public boolean isValid() {
		return true;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!fModel.isEditable()) {
			throwCoreException(PDECoreMessages.PluginObject_readOnlyChange);
		}
	}

	public void setInTheModel(boolean value) {
		fInTheModel = value;
	}

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
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(e);
		}
	}

	public ISharedPluginModel getModel() {
		return fModel;
	}

	public IPluginModelBase getPluginModel() {
		if (fModel instanceof IBundlePluginModelProvider)
			return ((IBundlePluginModelProvider) fModel).getBundlePluginModel();

		return fModel instanceof IPluginModelBase ? (IPluginModelBase) fModel : null;
	}

	public String getName() {
		return fName;
	}

	public String getTranslatedName() {
		if (fTranslatedName != null && !fModel.isEditable())
			return fTranslatedName;
		if (fTranslatedName == null && fName != null && fModel != null) {
			fTranslatedName = fModel.getResourceString(fName);
		}
		return fTranslatedName;
	}

	String getNodeAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null)
			return attribute.getNodeValue();
		return null;
	}

	public IPluginObject getParent() {
		return fParent;
	}

	public IPluginBase getPluginBase() {
		IPluginModelBase pluginModel = getPluginModel();
		return pluginModel != null ? pluginModel.getPluginBase() : null;
	}

	public String getResourceString(String key) {
		return fModel.getResourceString(key);
	}

	static boolean isNotEmpty(String text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i)) == false)
				return true;
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
		Status status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, message, null);
		CoreException ce = new CoreException(status);
		ce.fillInStackTrace();
		throw ce;
	}

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

	public Vector addComments(Node node, Vector result) {
		for (Node prev = node.getPreviousSibling(); prev != null; prev = prev.getPreviousSibling()) {
			if (prev.getNodeType() == Node.TEXT_NODE)
				continue;
			if (prev instanceof Comment) {
				String comment = prev.getNodeValue();
				if (result == null)
					result = new Vector();
				result.add(0, comment);
			} else
				break;
		}
		return result;
	}

	void writeComments(PrintWriter writer, Vector source) {
		if (source == null)
			return;
		for (int i = 0; i < source.size(); i++) {
			String comment = (String) source.elementAt(i);
			writer.println("<!--" + comment + "-->"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	protected boolean stringEqualWithNull(String a, String b) {
		return a == null && b == null || a != null && b != null && a.equals(b);
	}

	public String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}

	public int getStartLine() {
		return fStartLine;
	}

	public int getStopLine() {
		return fStartLine;
	}

	public Object getAdapter(Class adapter) {
		if (adapter.equals(IPluginModelBase.class)) {
			return getPluginModel();
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// NO-OP
		// Child classes to override
	}

}
