/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
