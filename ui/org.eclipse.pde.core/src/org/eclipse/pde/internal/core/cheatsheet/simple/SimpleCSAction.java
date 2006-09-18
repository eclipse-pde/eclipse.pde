/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;

/**
 * SimpleCSAction
 *
 */
public class SimpleCSAction extends SimpleCSObject implements ISimpleCSAction {

	/**
	 * Attribute:  class
	 */
	private String fClazz;	
	
	/**
	 * Attribute:  pluginId
	 */
	private String fPluginId;		
	
	/**
	 * Attribute:  confirm
	 */
	private boolean fConfirm;		

	/**
	 * Attribute:  when
	 */
	private String fWhen;		

	/**
	 * Attributes:  param1, param2, ..., param9
	 */
	private ArrayList fParams;
	
	private static final int F_MAX_PARAMS = 9;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSAction(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getClazz()
	 */
	public String getClazz() {
		return fClazz;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getConfirm()
	 */
	public boolean getConfirm() {
		return fConfirm;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParams()
	 */
	public String[] getParams() {
		return (String[])fParams.toArray(new String[fParams.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getPluginId()
	 */
	public String getPluginId() {
		return fPluginId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setClazz(java.lang.String)
	 */
	public void setClazz(String clazz) {
		String old = fClazz;
		fClazz = clazz;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_CLASS, old, fClazz);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		Boolean old =  Boolean.valueOf(fConfirm);
		fConfirm = confirm;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_CONFIRM, old, Boolean.valueOf(fConfirm));
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setPluginId(java.lang.String)
	 */
	public void setPluginId(String pluginId) {
		String old = fPluginId;
		fPluginId = pluginId;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_PLUGINID, old, fPluginId);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// Process class attribute
		fClazz = PDETextHelper.translateReadText(element.getAttribute(ATTRIBUTE_CLASS));
		// Process pluginId attribute
		fPluginId = PDETextHelper.translateReadText(element.getAttribute(ATTRIBUTE_PLUGINID));
		// Process confirm attribute
		if (element.getAttribute(ATTRIBUTE_CONFIRM).compareTo(
				ATTRIBUTE_VALUE_TRUE) == 0) {
			fConfirm = true;
		}		
		// Process when attribute
		// Read as is.  Do not translate
		fWhen = element.getAttribute(ATTRIBUTE_WHEN);

		// Process attributes:  param1, param2, ..., param9
		for (int i = 0; i < F_MAX_PARAMS; i++) {
			int adjustedIndex = i + 1;
			String parameter = ATTRIBUTE_PARAM + adjustedIndex;
			// Read as is.  Do not translate
			String value = element.getAttribute(parameter);
			fParams.add(i, value);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		StringBuffer buffer = new StringBuffer();
		
		try {
			// Print action element
			buffer.append(ELEMENT_ACTION); 
			// Print class attribute
			if ((fClazz != null) && 
					(fClazz.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_CLASS, PDETextHelper.translateWriteText(fClazz)));
			}
			// Print pluginId attribute
			if ((fPluginId != null) && 
					(fPluginId.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_PLUGINID, PDETextHelper.translateWriteText(fPluginId)));
			}
			// Print confirm attribute
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_CONFIRM, new Boolean(fConfirm).toString()));
			// Print when attribute
			if ((fWhen != null) && 
					(fWhen.length() > 0)) {
				// Write as is.  Do not translate
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_WHEN, fWhen));
			}
			// Print attributes:  param1, param2, ..., param9
			for (int i = 0; i < F_MAX_PARAMS; i++) {
				int adjustedIndex = i + 1;
				String parameter = ATTRIBUTE_PARAM + adjustedIndex;
				// Write as is.  Do not translate
				String value = (String)fParams.get(i);
				// Preserve cheat sheet validity
				// Ignore Semantic Rule:  Only contiguously defined parameters allowed
				// Write only if defined
				if (PDETextHelper.isDefined(value)) {
					buffer.append(XMLPrintHandler.wrapAttribute(
							parameter, value));
				}
			}
			
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_ACTION, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fClazz = null;
		fPluginId = null;
		fConfirm = false;
		fWhen = null;
		fParams = new ArrayList(F_MAX_PARAMS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_ACTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getWhen()
	 */
	public String getWhen() {
		return fWhen;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		String old = fWhen;
		fWhen = when;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_WHEN, old, fWhen);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not a separate node in tree view
		return ELEMENT_ACTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParam(int)
	 */
	public String getParam(int index) {
		// Note:  index is mapped to ID value rather than to actual storage
		// i.e.  getParam(1) returns index 0
		if ((index < 1) ||
				(index > F_MAX_PARAMS)) {
			return null;
		}
		int actualIndex = index - 1;
		return (String)fParams.get(actualIndex);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setParam(java.lang.String, int)
	 */
	public void setParam(String param, int index) {
		// Note:  index is mapped to ID value rather than to actual storage
		// i.e.  getParam(1) returns index 0
		if ((index < 1) ||
				(index > F_MAX_PARAMS)) {
			return;
		}
		int actualIndex = index - 1;		
		String old = (String)fParams.get(actualIndex);
		fParams.set(actualIndex, param);

		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_PARAM, old, param);
		}
	}

}
