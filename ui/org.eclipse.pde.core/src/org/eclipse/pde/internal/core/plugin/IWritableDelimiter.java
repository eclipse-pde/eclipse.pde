/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.plugin;

import java.io.PrintWriter;

/**
 * IWritableDelimeter
 *
 */
public interface IWritableDelimiter {

	/**
	 * Write a customized delimiter to the serialized stream that delimits the
	 * text representation of multiple model objects selected.
	 * This is applicable when serializing multiple objects for copy, cut and
	 * paste operations.
	 * 
	 * @param writer the print writer
	 */
	public void writeDelimeter(PrintWriter writer);

}
