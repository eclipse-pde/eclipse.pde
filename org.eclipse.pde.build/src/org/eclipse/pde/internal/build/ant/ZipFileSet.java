/*******************************************************************************
 *  Copyright (c) 2004, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.ant;

import org.eclipse.core.runtime.Path;

public class ZipFileSet extends FileSet {

	String prefix;
	boolean file;
	String permission;

	/**
	 * @param dir
	 * @param defaultexcludes
	 * @param includes
	 * @param includesfile
	 * @param excludes
	 * @param excludesfile
	 * @param casesensitive
	 * @param permission
	 */
	public ZipFileSet(String dir, boolean file, String defaultexcludes, String includes, String includesfile, String excludes, String excludesfile, String prefix, String casesensitive, String permission) {
		super(dir, defaultexcludes, includes, includesfile, excludes, excludesfile, casesensitive);
		this.prefix = prefix;
		this.file = file;
		this.permission = permission;
	}

	protected void print(AntScript script) {
		script.printTab();
		script.print("<zipfileset"); //$NON-NLS-1$
		if (file)
			script.printAttribute("file", dir, false); //$NON-NLS-1$
		else
			script.printAttribute("dir", dir, false); //$NON-NLS-1$
		script.printAttribute("defaultexcludes", defaultexcludes, false); //$NON-NLS-1$
		script.printAttribute("includes", includes, false); //$NON-NLS-1$
		script.printAttribute("includesfile", includesfile, false); //$NON-NLS-1$
		script.printAttribute("excludes", excludes, false); //$NON-NLS-1$
		script.printAttribute("excludesfile", excludesfile, false); //$NON-NLS-1$
		script.printAttribute("casesensitive", casesensitive, false); //$NON-NLS-1$
		if (prefixHasWildcards()) {
			String pre = new Path(prefix).removeLastSegments(1).toString();
			script.printAttribute("prefix", pre, false); //$NON-NLS-1$
		} else if (file) {
			script.printAttribute("fullpath", prefix, false); //$NON-NLS-1$
		} else {
			script.printAttribute("prefix", prefix, false); //$NON-NLS-1$
		}

		if (file)
			script.printAttribute("filemode", permission, false); //$NON-NLS-1$
		else
			script.printAttribute("dirmode", permission, false); //$NON-NLS-1$

		script.println("/>"); //$NON-NLS-1$
	}

	private boolean prefixHasWildcards() {
		if (prefix == null)
			return false;
		return (prefix.indexOf('*') != -1 || prefix.indexOf('?') != -1);
	}
}
