/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional;

public interface ITreeNode {
	public static final int CLASS = 1;
	public static final int INTERFACE = 2;
	public static final int ANNOTATION = 3;
	public static final int ENUM = 4;
	public static final int PACKAGE = 5;

	Object[] getChildren();
	boolean hasChildren();
	int getId();
	Object getData();
}
