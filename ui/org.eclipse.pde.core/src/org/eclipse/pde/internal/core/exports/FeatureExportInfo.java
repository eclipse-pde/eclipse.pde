/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.exports;

public class FeatureExportInfo {

	public boolean toDirectory;
	public boolean useJarFormat;
	public boolean exportSource;
	public boolean exportSourceBundle;
	public boolean exportMetadata;
	public boolean allowBinaryCycles;
	public boolean useWorkspaceCompiledClasses;
	public String destinationDirectory;
	public String zipFileName;
	public String qualifier;
	public Object[] items;
	public String[] signingInfo;
	public String[] jnlpInfo;
	public String[][] targets;
	public String categoryDefinition;

}
