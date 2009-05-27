/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jacek Pospychala <jacek.pospychala@pl.ibm.com> - bugs 202583, 207344
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 218648 
 *******************************************************************************/
package org.eclipse.ui.internal.views.log;

import java.io.PrintWriter;

/**
 * Groups other Abstract Entries under given name.
 */
public class Group extends AbstractEntry {

	private String name;

	public Group(String name) {
		this.name = name;
	}

	public void write(PrintWriter writer) {
		Object[] children = getChildren(null);
		for (int i = 0; i < children.length; i++) {
			AbstractEntry entry = (AbstractEntry) children[i];
			entry.write(writer);
			writer.println();
		}
	}

	public String toString() {
		return name;
	}

}