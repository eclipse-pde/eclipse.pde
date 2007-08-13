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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

/**
 * SimpleCSModelFactory
 *
 */
public class SimpleCSModelFactory implements ISimpleCSModelFactory {

	private ISimpleCSModel fModel;

	
	/**
	 * 
	 */
	public SimpleCSModelFactory(ISimpleCSModel model) {
		fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCS()
	 */
	public ISimpleCS createSimpleCS() {
		return new SimpleCS(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSAction()
	 */
	public ISimpleCSAction createSimpleCSAction(ISimpleCSObject parent) {
		return new SimpleCSAction(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSCommand()
	 */
	public ISimpleCSCommand createSimpleCSCommand(ISimpleCSObject parent) {
		return new SimpleCSCommand(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSConditionalSubItem()
	 */
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem(ISimpleCSObject parent) {
		return new SimpleCSConditionalSubItem(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSIntro()
	 */
	public ISimpleCSIntro createSimpleCSIntro(ISimpleCSObject parent) {
		return new SimpleCSIntro(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSItem()
	 */
	public ISimpleCSItem createSimpleCSItem(ISimpleCSObject parent) {
		return new SimpleCSItem(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSOnCompletion()
	 */
	public ISimpleCSOnCompletion createSimpleCSOnCompletion(ISimpleCSObject parent) {
		return new SimpleCSOnCompletion(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSPerformWhen()
	 */
	public ISimpleCSPerformWhen createSimpleCSPerformWhen(ISimpleCSObject parent) {
		return new SimpleCSPerformWhen(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSRepeatedSubItem()
	 */
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem(ISimpleCSObject parent) {
		return new SimpleCSRepeatedSubItem(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSSubItem()
	 */
	public ISimpleCSSubItem createSimpleCSSubItem(ISimpleCSObject parent) {
		return new SimpleCSSubItem(fModel, parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSDescription(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject)
	 */
	public ISimpleCSDescription createSimpleCSDescription(ISimpleCSObject parent) {
		return new SimpleCSDescription(fModel, parent);
	}

	
	public IDocumentAttributeNode createAttribute(String name, String value,
			IDocumentNode enclosingElement) {
		// TODO: MP: TEO: LOW: Not used - Delete class when done
		return null;
	}

	public IDocumentNode createDocumentNode(String name, IDocumentNode parent) {
		// TODO: MP: TEO: LOW: Not used - Delete class when done
		return null;
	}

	public IDocumentTextNode createDocumentTextNode(String content,
			IDocumentNode parent) {
		// TODO: MP: TEO: LOW: Not used - Delete class when done
		return null;
	}
	
}
