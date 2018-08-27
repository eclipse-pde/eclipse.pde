/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;
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

	protected List<ICompCSDependency> fFieldDependencies;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSTaskObject(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		// Reset called by child class
	}

	@Override
	public abstract List<ICompCSTaskObject> getChildren();

	@Override
	public abstract String getName();

	@Override
	public abstract int getType();

	@Override
	public void reset() {
		fFieldId = null;
		fFieldKind = null;
		fFieldIntro = null;
		fFieldOnCompletion = null;
		fFieldName = null;
		fFieldSkip = false;
		fFieldDependencies = new ArrayList<>();
	}

	@Override
	public void addFieldDependency(ICompCSDependency dependency) {
		fFieldDependencies.add(dependency);
		if (isEditable()) {
			fireStructureChanged(dependency, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public String getFieldId() {
		return fFieldId;
	}

	@Override
	public ICompCSIntro getFieldIntro() {
		return fFieldIntro;
	}

	@Override
	public String getFieldKind() {
		return fFieldKind;
	}

	@Override
	public String getFieldName() {
		return fFieldName;
	}

	@Override
	public ICompCSOnCompletion getFieldOnCompletion() {
		return fFieldOnCompletion;
	}

	@Override
	public boolean getFieldSkip() {
		return fFieldSkip;
	}

	@Override
	public void removeFieldDepedency(ICompCSDependency dependency) {
		fFieldDependencies.remove(dependency);
		if (isEditable()) {
			fireStructureChanged(dependency, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public void setFieldId(String id) {
		String old = fFieldId;
		fFieldId = id;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_ID, old, fFieldId);
		}
	}

	@Override
	public void setFieldIntro(ICompCSIntro intro) {
		ICompCSObject old = fFieldIntro;
		fFieldIntro = intro;
		if (isEditable()) {
			fireStructureChanged(intro, old);
		}
	}

	@Override
	public void setFieldKind(String kind) {
		String old = fFieldKind;
		fFieldKind = kind;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_KIND, old, fFieldKind);
		}
	}

	@Override
	public void setFieldName(String name) {
		String old = fFieldName;
		fFieldName = name;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_NAME, old, fFieldName);
		}
	}

	@Override
	public void setFieldOnCompletion(ICompCSOnCompletion onCompletion) {
		ICompCSObject old = fFieldOnCompletion;
		fFieldOnCompletion = onCompletion;
		if (isEditable()) {
			fireStructureChanged(onCompletion, old);
		}
	}

	@Override
	public void setFieldSkip(boolean skip) {
		Boolean old = Boolean.valueOf(fFieldSkip);
		fFieldSkip = skip;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_SKIP, old, Boolean
					.valueOf(fFieldSkip));
		}
	}

	@Override
	public ICompCSDependency[] getFieldDependencies() {
		return fFieldDependencies
				.toArray(new ICompCSDependency[fFieldDependencies.size()]);
	}

	@Override
	protected void parseText(Text text) {
		// NO-OP
	}

	@Override
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

	@Override
	protected void writeAttributes(StringBuilder buffer) {
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
				Boolean.valueOf(fFieldSkip).toString()));
	}

	@Override
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

	@Override
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
		Iterator<ICompCSDependency> iterator = fFieldDependencies.iterator();
		while (iterator.hasNext()) {
			ICompCSDependency dependency = iterator.next();
			dependency.write(newIndent, writer);
		}
	}

	@Override
	public abstract String getElement();

}
