/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public abstract class CompCSTaskObject extends CompCSObject implements
		ICompCSTaskObject {

	private static final long serialVersionUID = 1L;

	protected String fFieldId;

	protected String fFieldKind;

	protected ICompCSIntro fFieldIntro;

	protected ICompCSOnCompletion fFieldOnCompletion;

	protected String fFieldName;

	protected boolean fFieldSkip;

	protected ArrayList fFieldDependencies;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSTaskObject(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		// Reset called by child class
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getChildren()
	 */
	public abstract List getChildren();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getName()
	 */
	public abstract String getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getType()
	 */
	public abstract int getType();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#reset()
	 */
	public void reset() {
		fFieldId = null;
		fFieldKind = null;
		fFieldIntro = null;
		fFieldOnCompletion = null;
		fFieldName = null;
		fFieldSkip = false;
		fFieldDependencies = new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * addFieldDependency
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency)
	 */
	public void addFieldDependency(ICompCSDependency dependency) {
		fFieldDependencies.add(dependency);
		if (isEditable()) {
			fireStructureChanged(dependency, IModelChangedEvent.INSERT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#getFieldId
	 * ()
	 */
	public String getFieldId() {
		return fFieldId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * getFieldIntro()
	 */
	public ICompCSIntro getFieldIntro() {
		return fFieldIntro;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#getFieldKind
	 * ()
	 */
	public String getFieldKind() {
		return fFieldKind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#getFieldName
	 * ()
	 */
	public String getFieldName() {
		return fFieldName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * getFieldOnCompletion()
	 */
	public ICompCSOnCompletion getFieldOnCompletion() {
		return fFieldOnCompletion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#getFieldSkip
	 * ()
	 */
	public boolean getFieldSkip() {
		return fFieldSkip;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * removeFieldDepedency
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSDependency)
	 */
	public void removeFieldDepedency(ICompCSDependency dependency) {
		fFieldDependencies.remove(dependency);
		if (isEditable()) {
			fireStructureChanged(dependency, IModelChangedEvent.REMOVE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#setFieldId
	 * (java.lang.String)
	 */
	public void setFieldId(String id) {
		String old = fFieldId;
		fFieldId = id;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_ID, old, fFieldId);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * setFieldIntro
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSIntro)
	 */
	public void setFieldIntro(ICompCSIntro intro) {
		ICompCSObject old = fFieldIntro;
		fFieldIntro = intro;
		if (isEditable()) {
			fireStructureChanged(intro, old);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#setFieldKind
	 * (java.lang.String)
	 */
	public void setFieldKind(String kind) {
		String old = fFieldKind;
		fFieldKind = kind;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_KIND, old, fFieldKind);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#setFieldName
	 * (java.lang.String)
	 */
	public void setFieldName(String name) {
		String old = fFieldName;
		fFieldName = name;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_NAME, old, fFieldName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * setFieldOnCompletion
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSOnCompletion)
	 */
	public void setFieldOnCompletion(ICompCSOnCompletion onCompletion) {
		ICompCSObject old = fFieldOnCompletion;
		fFieldOnCompletion = onCompletion;
		if (isEditable()) {
			fireStructureChanged(onCompletion, old);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#setFieldSkip
	 * (boolean)
	 */
	public void setFieldSkip(boolean skip) {
		Boolean old = Boolean.valueOf(fFieldSkip);
		fFieldSkip = skip;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_SKIP, old, Boolean
					.valueOf(fFieldSkip));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject#
	 * getFieldDependencies()
	 */
	public ICompCSDependency[] getFieldDependencies() {
		return (ICompCSDependency[]) fFieldDependencies
				.toArray(new ICompCSDependency[fFieldDependencies.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseText(
	 * org.w3c.dom.Text)
	 */
	protected void parseText(Text text) {
		// NO-OP
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseAttributes
	 * (org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		// Process id attribute
		// Trim leading and trailing whitespace
		fFieldId = element.getAttribute(ATTRIBUTE_ID).trim();
		// Process kind attribute
		// Trim leading and trailing whitespace
		fFieldKind = element.getAttribute(ATTRIBUTE_KIND).trim();
		// Process name attribute
		// Trim leading and trailing whitespace
		fFieldName = element.getAttribute(ATTRIBUTE_NAME).trim();
		// Process skip attribute
		if (element.getAttribute(ATTRIBUTE_SKIP)
				.compareTo(ATTRIBUTE_VALUE_TRUE) == 0) {
			fFieldSkip = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#writeAttributes
	 * (java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		// Print id attribute
		if ((fFieldId != null) && (fFieldId.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_ID,
					PDETextHelper.translateWriteText(fFieldId.trim(),
							DEFAULT_SUBSTITUTE_CHARS)));
		}
		// Print kind attribute
		if ((fFieldKind != null) && (fFieldKind.length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_KIND,
					fFieldKind));
		}
		// Print name attribute
		if ((fFieldName != null) && (fFieldName.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_NAME,
					PDETextHelper.translateWriteText(fFieldName.trim(),
							DEFAULT_SUBSTITUTE_CHARS)));
		}
		// Print skip attribute
		buffer.append(XMLPrintHandler.wrapAttribute(ATTRIBUTE_SKIP,
				new Boolean(fFieldSkip).toString()));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseElement
	 * (org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		String name = element.getNodeName();
		ICompCSModelFactory factory = getModel().getFactory();

		if (name.equals(ELEMENT_INTRO)) {
			// Process intro element
			fFieldIntro = factory.createCompCSIntro(this);
			fFieldIntro.parse(element);
		} else if (name.equals(ELEMENT_ONCOMPLETION)) {
			// Process onCompletion element
			fFieldOnCompletion = factory.createCompCSOnCompletion(this);
			fFieldOnCompletion.parse(element);
		} else if (name.equals(ELEMENT_DEPENDENCY)) {
			// Process dependency element
			ICompCSDependency dependency = factory.createCompCSDependency(this);
			fFieldDependencies.add(dependency);
			dependency.parse(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#writeElements
	 * (java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print intro element
		if (fFieldIntro != null) {
			fFieldIntro.write(newIndent, writer);
		}
		// Print onCompletion element
		if (fFieldOnCompletion != null) {
			fFieldOnCompletion.write(newIndent, writer);
		}
		// Print dependency elements
		Iterator iterator = fFieldDependencies.iterator();
		while (iterator.hasNext()) {
			ICompCSDependency dependency = (ICompCSDependency) iterator.next();
			dependency.write(newIndent, writer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getElement()
	 */
	public abstract String getElement();

}
