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

package org.eclipse.pde.internal.ua.core.icheatsheet.comp;

public interface ICompCSTaskObject extends ICompCSObject {

	/**
	 * Attribute: kind
	 * 
	 * @param kind
	 */
	public void setFieldKind(String kind);

	/**
	 * Attribute: kind
	 * 
	 * @return
	 */
	public String getFieldKind();

	/**
	 * Attribute: name
	 * 
	 * @param name
	 */
	public void setFieldName(String name);

	/**
	 * Attribute: name
	 * 
	 * @return
	 */
	public String getFieldName();

	/**
	 * Attribute: id
	 * 
	 * @param id
	 */
	public void setFieldId(String id);

	/**
	 * Attribute: id
	 * 
	 * @return
	 */
	public String getFieldId();

	/**
	 * Attribute: skip
	 * 
	 * @param skip
	 */
	public void setFieldSkip(boolean skip);

	/**
	 * Attribute: skip
	 * 
	 * @return
	 */
	public boolean getFieldSkip();

	/**
	 * Element: onCompletion
	 * 
	 * @param onCompletion
	 */
	public void setFieldOnCompletion(ICompCSOnCompletion onCompletion);

	/**
	 * Element: onCompletion
	 * 
	 * @return
	 */
	public ICompCSOnCompletion getFieldOnCompletion();

	/**
	 * Element: intro
	 * 
	 * @param intro
	 */
	public void setFieldIntro(ICompCSIntro intro);

	/**
	 * Element: intro
	 * 
	 * @return
	 */
	public ICompCSIntro getFieldIntro();

	/**
	 * Element: dependency
	 * 
	 * @param dependency
	 */
	public void addFieldDependency(ICompCSDependency dependency);

	/**
	 * Element: dependency
	 * 
	 * @param dependency
	 */
	public void removeFieldDepedency(ICompCSDependency dependency);

	/**
	 * Element: dependency
	 * 
	 * @return
	 */
	public ICompCSDependency[] getFieldDependencies();

}
