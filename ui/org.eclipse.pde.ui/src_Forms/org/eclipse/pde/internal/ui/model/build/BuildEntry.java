/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.util.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.model.*;

public class BuildEntry implements IBuildEntry, IDocumentKey {

	private int fLength = -1;
	private int fOffset = -1;
	private IBuildModel fModel;
	private String fName;
	private ArrayList fTokens = new ArrayList();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#addToken(java.lang.String)
	 */
	public void addToken(String token) throws CoreException {
		fTokens.add(token);
		getModel().fireModelObjectChanged(this, getName(), null, token);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getName()
	 */
	public String getName() {
		return fName;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#getTokens()
	 */
	public String[] getTokens() {
		return (String[])fTokens.toArray(new String[fTokens.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#contains(java.lang.String)
	 */
	public boolean contains(String token) {
		return fTokens.contains(token);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#removeToken(java.lang.String)
	 */
	public void removeToken(String token) throws CoreException {
		fTokens.remove(token);
		getModel().fireModelObjectChanged(this, getName(), token, null);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#renameToken(java.lang.String, java.lang.String)
	 */
	public void renameToken(String oldToken, String newToken)
			throws CoreException {
		int index = fTokens.indexOf(oldToken);
		if (index != -1) {
			fTokens.set(index, newToken);
			getModel().fireModelObjectChanged(this, getName(), oldToken, newToken);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setName(java.lang.String)
	 */
	public void setName(String name) {
		String oldName = fName;
		fName = name;
		if (getModel() != null){
			try {
				IBuild build = getModel().getBuild();
				IBuildEntry entry = build.getEntry(oldName);
				build.remove(entry);
				build.add(entry);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			getModel().fireModelObjectChanged(this, getName(), oldName, name);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getOffset()
	 */
	public int getOffset() {
		return fOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setOffset(int)
	 */
	public void setOffset(int offset) {
		fOffset = offset;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getLength()
	 */
	public int getLength() {
		return fLength;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setLength(int)
	 */
	public void setLength(int length) {
		fLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
	
	public void setModel(IBuildModel model) {
		fModel = model;
	}
	
	public IBuildModel getModel() {
		return fModel;
	}
	
	public void processEntry(String value) {
		StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			fTokens.add(stok.nextToken().trim());
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#write()
	 */
	public String write() {
		return PropertiesUtil.writeKeyValuePair(getName(), getTokens());
	}
}
