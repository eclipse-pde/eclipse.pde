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

import org.eclipse.pde.internal.build.tasks.ManifestModifier;

public class Test2 {
	public static void main(String[] args) {
		ManifestModifier replacer = new ManifestModifier();
		replacer.setManifestLocation("d:/tmp/META-INF/manifest.mf");
		replacer.setKeyValue("Bundle-Version|3.0.0#Generated-from|null");
		replacer.execute();
	}
}