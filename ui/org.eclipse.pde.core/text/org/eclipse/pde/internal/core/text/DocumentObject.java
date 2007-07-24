/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text;

import java.io.PrintWriter;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode;

/**
 * DocumentObject
 *
 */
public abstract class DocumentObject extends PluginDocumentNode implements
		IDocumentObject {

	// TODO: MP: TEO: Consider renaming class
	
	// TODO: MP: TEO: Integrate with plugin model?
	
	// TODO: MP: TEO: Investigate document node to see if any methods to pull down
	
	private transient IModel fModel;
	
	private transient boolean fInTheModel;
	
	/**
	 * 
	 */
	public DocumentObject(IModel model) {
		super();
		
		fModel = model;
		fInTheModel = false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#setSharedModel(org.eclipse.pde.core.IModel)
	 */
	public void setSharedModel(IModel model) {
		fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#getSharedModel()
	 */
	public IModel getSharedModel() {
		return fModel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#reset()
	 */
	public void reset() {
		// TODO: MP: TEO: reset parent fields? or super.reset?
		fModel = null;
		fInTheModel = false;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#isEditable()
	 */
	public boolean isEditable() {
		// Convenience method
		return fModel.isEditable();
	}
	
	/**
	 * @return
	 */
	protected boolean shouldFireEvent() {
		if (isInTheModel() && isEditable()) {
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentObject#getLineDelimiter()
	 */
	protected String getLineDelimiter() {
		if (fModel instanceof IEditingModel) {
			IDocument document = ((IEditingModel)fModel).getDocument();
			return TextUtilities.getDefaultLineDelimiter(document);
		}
		return "\n"; //$NON-NLS-1$
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, org.eclipse.pde.internal.core.ischema.ISchema, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void reconnect(IDocumentNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient field:  In The Model
		// Value set to true when added to the parent
		fInTheModel = false;
		// Transient field:  Model
		fModel = model;
	}	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return write(false);
	}	
	
	/**
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}

	/**
	 * @param object
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	private void firePropertyChanged(Object object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable() && 
				(fModel instanceof IModelChangeProvider)) {
			IModelChangeProvider provider = (IModelChangeProvider)fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}	
	
	/**
	 * @param newValue
	 * @param oldValue
	 */
	protected void fireStructureChanged(Object newValue,
			Object oldValue) {

		int changeType = -1;
		Object object = null;
		if (newValue == null) {
			changeType = IModelChangedEvent.REMOVE;
			object = oldValue;
		} else {
			changeType = IModelChangedEvent.INSERT;
			object = newValue;
		}
		fireStructureChanged(object, changeType);
	}	
	
	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(Object child, int changeType) {
		fireStructureChanged(new Object[] { child }, changeType);
	}	
	
	/**
	 * @param children
	 * @param changeType
	 */
	protected void fireStructureChanged(Object[] children, int changeType) {
		if (fModel.isEditable() && 
				(fModel instanceof IModelChangeProvider)) {
			IModelChangeProvider provider = (IModelChangeProvider)fModel;
			IModelChangedEvent event = new ModelChangedEvent(provider, changeType,
					children, null);
			provider.fireModelChanged(event);
		}
	}		
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#addChildNode(org.eclipse.pde.internal.core.text.IDocumentNode, int)
	 */
	public void addChildNode(IDocumentNode child, int position) {
		super.addChildNode(child, position);
		// TODO: MP: TEO: Investigate if this is really needed.  Why not other add methods
		if (child instanceof DocumentObject) {
			((DocumentObject)child).setInTheModel(true);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#addChildNode(org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void addChildNode(IDocumentNode child) {
		super.addChildNode(child);
		// TODO: MP: TEO:  Not sure if this is needed
		if (child instanceof DocumentObject) {
			((DocumentObject)child).setInTheModel(true);
		}	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.cheatsheet.simple.SimpleCSObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write(true));
	}	
	
}
