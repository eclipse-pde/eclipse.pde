/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.templates.IFieldData;

	public class FieldData implements IFieldData {
		boolean fragment;
		String name;
		String version;
		String pluginId;
		String pluginVersion;
		int match;
		String provider;
		boolean doMain;
		String className;
		boolean thisCheck;
		boolean bundleCheck;
		boolean workspaceCheck;
		
		public boolean isFragment() {
			return fragment;
		}
		
		public String getName() {
			return name;
		}
		public String getVersion() {
			return version;
		}
		public String getProvider() {
			return provider;
		}
		public String getClassName() {
			return className;
		}
	}
