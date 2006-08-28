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

package org.eclipse.pde.internal.core.icheatsheet.simple;

/**
 * ISimpleCheatSheetModelFactory
 *
 */
public interface ISimpleCSModelFactory {

	/**
	 * @return
	 */
	public ISimpleCS createSimpleCS();
	
	/**
	 * @return
	 */
	public ISimpleCSAction createSimpleCSAction();
	
	/**
	 * @return
	 */
	public ISimpleCSCommand createSimpleCSCommand();
	
	/**
	 * @return
	 */
	public ISimpleCSConditionalSubItem createSimpleCSConditionalSubItem();
	
	/**
	 * @return
	 */
	public ISimpleCSIntro createSimpleCSIntro();
	
	/**
	 * @return
	 */
	public ISimpleCSItem createSimpleCSItem();
	
	/**
	 * @return
	 */
	public ISimpleCSOnCompletion createSimpleCSOnCompletion();
	
	/**
	 * @return
	 */
	public ISimpleCSPerformWhen createSimpleCSPerformWhen();
	
	/**
	 * @return
	 */
	public ISimpleCSRepeatedSubItem createSimpleCSRepeatedSubItem();
	
	/**
	 * @return
	 */
	public ISimpleCSSubItem createSimpleCSSubItem();
	
}
