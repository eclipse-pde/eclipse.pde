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
import java.util.ListIterator;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;
import org.w3c.dom.Element;

public class CompCSTask extends CompCSTaskObject implements ICompCSTask {

	private List<ICompCSParam> fFieldParams;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSTask(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		reset();
	}

	@Override
	public List<ICompCSTaskObject> getChildren() {
		return new ArrayList<>();
	}

	@Override
	public String getName() {
		return fFieldName;
	}

	@Override
	public int getType() {
		return TYPE_TASK;
	}

	@Override
	public String getElement() {
		return ELEMENT_TASK;
	}

	@Override
	protected void parseElement(Element element) {
		super.parseElement(element);
		String name = element.getNodeName();
		ICompCSModelFactory factory = getModel().getFactory();

		if (name.equals(ELEMENT_PARAM)) {
			// Process param element
			ICompCSParam param = factory.createCompCSParam(this);
			fFieldParams.add(param);
			param.parse(element);
		}
	}

	@Override
	public void addFieldParam(ICompCSParam param) {
		fFieldParams.add(param);
		if (isEditable()) {
			fireStructureChanged(param, IModelChangedEvent.INSERT);
		}
	}

	@Override
	public ICompCSParam[] getFieldParams() {
		return fFieldParams
				.toArray(new ICompCSParam[fFieldParams.size()]);
	}

	@Override
	public void removeFieldParam(ICompCSParam param) {
		fFieldParams.remove(param);
		if (isEditable()) {
			fireStructureChanged(param, IModelChangedEvent.REMOVE);
		}
	}

	@Override
	public void reset() {
		super.reset();

		fFieldParams = new ArrayList<>();
	}

	@Override
	protected void writeElements(String indent, PrintWriter writer) {
		super.writeElements(indent, writer);
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print param elements
		Iterator<ICompCSParam> iterator = fFieldParams.iterator();
		while (iterator.hasNext()) {
			ICompCSParam param = iterator.next();
			param.write(newIndent, writer);
		}
	}

	@Override
	public boolean hasFieldParams() {
		if (fFieldParams.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public ICompCSParam getFieldParam(String name) {
		if (fFieldParams.isEmpty()) {
			return null;
		}
		ListIterator<ICompCSParam> iterator = fFieldParams.listIterator();
		while (iterator.hasNext()) {
			ICompCSParam parameter = iterator.next();
			if (parameter.getFieldName().equals(name)) {
				return parameter;
			}
		}
		return null;
	}
}
