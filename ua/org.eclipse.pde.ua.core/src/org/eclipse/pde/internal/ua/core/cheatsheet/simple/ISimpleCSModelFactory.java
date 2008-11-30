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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;

public interface ISimpleCSModelFactory extends IDocumentNodeFactory {

	/**
	 * @return
	 */
	public ISimpleCS createSimpleCS();

	/**
	 * @return
	 */
	public ISimpleCSAction createSimpleCSAction(ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSCommand createSimpleCSCommand(ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem(
			ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSIntro createSimpleCSIntro(ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSItem createSimpleCSItem(ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSOnCompletion createSimpleCSOnCompletion(
			ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSPerformWhen createSimpleCSPerformWhen(ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem(
			ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSSubItem createSimpleCSSubItem(ISimpleCSObject parent);

	/**
	 * @return
	 */
	public ISimpleCSDescription createSimpleCSDescription(ISimpleCSObject parent);
}
