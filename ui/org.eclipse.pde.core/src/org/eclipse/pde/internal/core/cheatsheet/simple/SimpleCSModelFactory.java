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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;

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
	public ISimpleCSAction createSimpleCSAction() {
		return new SimpleCSAction(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSCommand()
	 */
	public ISimpleCSCommand createSimpleCSCommand() {
		return new SimpleCSCommand(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSConditionalSubItem()
	 */
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem() {
		return new SimpleCSConditionalSubItem(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSIntro()
	 */
	public ISimpleCSIntro createSimpleCSIntro() {
		return new SimpleCSIntro(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSItem()
	 */
	public ISimpleCSItem createSimpleCSItem() {
		return new SimpleCSItem(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSOnCompletion()
	 */
	public ISimpleCSOnCompletion createSimpleCSOnCompletion() {
		return new SimpleCSOnCompletion(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSPerformWhen()
	 */
	public ISimpleCSPerformWhen createSimpleCSPerformWhen() {
		return new SimpleCSPerformWhen(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSRepeatedSubItem()
	 */
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem() {
		return new SimpleCSRepeatedSubItem(fModel);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory#createSimpleCSSubItem()
	 */
	public ISimpleCSSubItem createSimpleCSSubItem() {
		return new SimpleCSSubItem(fModel);
	}

}
