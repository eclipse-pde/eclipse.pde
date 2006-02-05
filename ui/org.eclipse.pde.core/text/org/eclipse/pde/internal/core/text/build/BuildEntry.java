/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.build;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.util.PropertiesUtil;

public class BuildEntry implements IBuildEntry, IDocumentKey {

	private int fLength = -1;
	private int fOffset = -1;
	private IBuildModel fModel;
	private String fName;
	private ArrayList fTokens = new ArrayList();
	private String fLineDelimiter;
	
	public BuildEntry(String name, IBuildModel model) {
		fName = name;
		fModel = model;
		setLineDelimiter();
	}
	
	private void setLineDelimiter() {
		if (fModel instanceof IEditingModel) {
			IDocument document = ((IEditingModel)fModel).getDocument();
			fLineDelimiter = TextUtilities.getDefaultLineDelimiter(document);
		} else {
			fLineDelimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		}	
	}

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
		if (getModel() != null){
			try {
				IBuild build = getModel().getBuild();
				build.remove(this);
				fName = name;
				build.add(this);
			} catch (CoreException e) {
				PDECore.logException(e);
			}
			getModel().fireModelObjectChanged(this, getName(), oldName, name);
		} else
			fName = name;
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
		StringBuffer buffer = new StringBuffer();
		buffer.append(PropertiesUtil.createWritableName(fName));
		buffer.append(" = "); //$NON-NLS-1$
		int indentLength = fName.length() + 3;
		for (int i = 0; i < fTokens.size(); i++) {
			buffer.append(PropertiesUtil.createEscapedValue(fTokens.get(i).toString()));
			if (i < fTokens.size() - 1) {
				buffer.append(",\\"); //$NON-NLS-1$
				buffer.append(fLineDelimiter);
				for (int j = 0; j < indentLength; j++) {
					buffer.append(" "); //$NON-NLS-1$
				}
			}
		}	
		buffer.append(fLineDelimiter); //$NON-NLS-1$
		return buffer.toString();
	}

}
