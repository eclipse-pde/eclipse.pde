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
 * ISimpleCSAction
 *
 */
public interface ISimpleCSAction extends ISimpleCSRunObject {

	/**
	 * Attribute: class
	 * @return
	 */
	public String getClazz();

	/**
	 * Attribute: class
	 * @param clazz
	 */
	public void setClazz(String clazz);

	/**
	 * Attribute: pluginId
	 * @return
	 */
	public String getPluginId();

	/**
	 * Attribute: pluginId
	 * @param pluginId
	 */
	public void setPluginId(String pluginId);

	/**
	 * Attributes:  param1, param2, ..., param9
	 * @return
	 */
	public String[] getParams();

	/**
	 * Attributes:  param1, param2, ..., param9
	 * @return
	 */
	public String getParam(int index);

	/**
	 * Attributes:  param1, param2, ..., param9
	 * @param param
	 * @param index
	 */
	public void setParam(String param, int index);

}
