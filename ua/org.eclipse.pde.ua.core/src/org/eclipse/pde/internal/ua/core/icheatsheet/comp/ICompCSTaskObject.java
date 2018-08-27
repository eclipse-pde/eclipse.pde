/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.core.icheatsheet.comp;

public interface ICompCSTaskObject extends ICompCSObject {

	/**
	 * Attribute: kind
	 */
	public void setFieldKind(String kind);

	/**
	 * Attribute: kind
	 */
	public String getFieldKind();

	/**
	 * Attribute: name
	 */
	public void setFieldName(String name);

	/**
	 * Attribute: name
	 */
	public String getFieldName();

	/**
	 * Attribute: id
	 */
	public void setFieldId(String id);

	/**
	 * Attribute: id
	 */
	public String getFieldId();

	/**
	 * Attribute: skip
	 */
	public void setFieldSkip(boolean skip);

	/**
	 * Attribute: skip
	 */
	public boolean getFieldSkip();

	/**
	 * Element: onCompletion
	 */
	public void setFieldOnCompletion(ICompCSOnCompletion onCompletion);

	/**
	 * Element: onCompletion
	 */
	public ICompCSOnCompletion getFieldOnCompletion();

	/**
	 * Element: intro
	 */
	public void setFieldIntro(ICompCSIntro intro);

	/**
	 * Element: intro
	 */
	public ICompCSIntro getFieldIntro();

	/**
	 * Element: dependency
	 */
	public void addFieldDependency(ICompCSDependency dependency);

	/**
	 * Element: dependency
	 */
	public void removeFieldDepedency(ICompCSDependency dependency);

	/**
	 * Element: dependency
	 */
	public ICompCSDependency[] getFieldDependencies();

}
