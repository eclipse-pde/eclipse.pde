/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/

import org.eclipse.pde.internal.build.tasks.PluginVersionReplaceTask;

public class Test1 {
	public static void main(String[] args) {
		PluginVersionReplaceTask replacer = new PluginVersionReplaceTask();
		replacer.setPluginFilePath("d:/tmp/plugin.xml");
		replacer.setVersionNumber("foo");
		replacer.execute();
	}
}