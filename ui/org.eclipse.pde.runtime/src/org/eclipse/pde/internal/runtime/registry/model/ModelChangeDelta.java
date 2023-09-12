/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.runtime.registry.model;

/**
 * Delta model objects are of type IBundle, IService, IExtension, IExtensionPoint
 */
public class ModelChangeDelta {

	public static final int ADDED = 0;
	public static final int UPDATED = 1;
	public static final int REMOVED = 2;
	public static final int STARTING = 3;
	public static final int STARTED = 4;
	public static final int STOPPING = 5;
	public static final int STOPPED = 6;
	public static final int RESOLVED = 7;
	public static final int UNRESOLVED = 8;

	private final ModelObject fObject;
	private final int fFlag;

	public ModelChangeDelta(ModelObject object, int flag) {
		fObject = object;
		fFlag = flag;
	}

	public ModelObject getModelObject() {
		return fObject;
	}

	public int getFlag() {
		return fFlag;
	}
}
