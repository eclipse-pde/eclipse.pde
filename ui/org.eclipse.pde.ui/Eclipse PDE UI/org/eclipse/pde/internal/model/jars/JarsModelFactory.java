package org.eclipse.pde.internal.model.jars;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.plugin.*;

public class JarsModelFactory implements IJarsModelFactory {
	private IJarsModel model;

public JarsModelFactory(IJarsModel model) {
	this.model = model;
}
public IJarEntry createEntry(String name) {
	JarEntry entry = new JarEntry(name);
	entry.setModel(model);
	return entry;
}
}
