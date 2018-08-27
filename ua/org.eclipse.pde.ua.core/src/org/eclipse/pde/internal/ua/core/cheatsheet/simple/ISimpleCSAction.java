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

package org.eclipse.pde.internal.ua.core.cheatsheet.simple;

public interface ISimpleCSAction extends ISimpleCSRunObject {

	/**
	 * Attribute: class
	 */
	public String getClazz();

	/**
	 * Attribute: class
	 */
	public void setClazz(String clazz);

	/**
	 * Attribute: pluginId
	 */
	public String getPluginId();

	/**
	 * Attribute: pluginId
	 */
	public void setPluginId(String pluginId);

	/**
	 * Attributes: param1, param2, ..., param9
	 */
	public String[] getParams();

	/**
	 * Attributes: param1, param2, ..., param9
	 */
	public String getParam(int index);

	/**
	 * Attributes: param1, param2, ..., param9
	 */
	public void setParam(String param, int index);

}
