/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.project.PDEProject;
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
			IDocument document = ((IEditingModel) fModel).getDocument();
			fLineDelimiter = TextUtilities.getDefaultLineDelimiter(document);
		} else {
			fLineDelimiter = System.getProperty("line.separator"); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#addToken(java.lang.String)
	 */
	public void addToken(String token) throws CoreException {
		if (fTokens.contains(token))
			return;
		if (fTokens.add(token))
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
		return (String[]) fTokens.toArray(new String[fTokens.size()]);
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
		if (fTokens.remove(token))
			getModel().fireModelObjectChanged(this, getName(), token, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#renameToken(java.lang.String, java.lang.String)
	 */
	public void renameToken(String oldToken, String newToken) throws CoreException {
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
		if (getModel() != null) {
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
		IPath root = getRootPath();
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken().trim();
			token = fromRelative(token, root);
			fTokens.add(token);
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
		IPath rootPath = getRootPath();
		for (int i = 0; i < fTokens.size(); i++) {
			String token = fTokens.get(i).toString();
			token = toRelative(token, rootPath);
			buffer.append(PropertiesUtil.createEscapedValue(token));
			if (i < fTokens.size() - 1) {
				buffer.append(",\\"); //$NON-NLS-1$
				buffer.append(fLineDelimiter);
				for (int j = 0; j < indentLength; j++) {
					buffer.append(" "); //$NON-NLS-1$
				}
			}
		}
		buffer.append(fLineDelimiter);
		return buffer.toString();
	}

	public void swap(int index1, int index2) {
		Object obj1 = fTokens.get(index1);
		Object obj2 = fTokens.set(index2, obj1);
		fTokens.set(index1, obj2);
		getModel().fireModelObjectChanged(this, getName(), new Object[] {obj1, obj2}, new Object[] {obj2, obj1});
	}

	public String getPreviousToken(String targetToken) {
		// Ensure we have tokens
		if (fTokens.size() <= 1) {
			return null;
		}
		// Get the index of the target token
		int targetIndex = fTokens.indexOf(targetToken);
		// Validate index
		if (targetIndex < 0) {
			// Target token does not exist
			return null;
		} else if (targetIndex == 0) {
			// Target token has no previous token
			return null;
		}
		// 1 <= index < size()
		// Get the previous token
		String previousToken = (String) fTokens.get(targetIndex - 1);

		return previousToken;
	}

	public String getNextToken(String targetToken) {
		// Ensure we have tokens
		if (fTokens.size() <= 1) {
			return null;
		}
		// Get the index of the target token
		int targetIndex = fTokens.indexOf(targetToken);
		// Get the index of the last token
		int lastIndex = fTokens.size() - 1;
		// Validate index
		if (targetIndex < 0) {
			// Target token does not exist
			return null;
		} else if (targetIndex >= lastIndex) {
			// Target token has no next token
			return null;
		}
		// 0 <= index < last token < size()
		// Get the next token
		String nextToken = (String) fTokens.get(targetIndex + 1);

		return nextToken;
	}

	public int getIndexOf(String targetToken) {
		return fTokens.indexOf(targetToken);
	}

	public void addToken(String token, int position) {
		// Validate position
		if (position < 0) {
			return;
		} else if (position > fTokens.size()) {
			return;
		}
		// Ensure no duplicates
		if (fTokens.contains(token)) {
			return;
		}
		// Add the token at the specified position
		fTokens.add(position, token);
		// Fire event
		getModel().fireModelObjectChanged(this, getName(), null, token);
	}

	/**
	 * Returns the path that this entries tokens are relative to, or <code>null</code> if none.
	 * 
	 * @return relative root path, or <code>null</code>
	 */
	IPath getRootPath() {
		if (fName.startsWith(IBuildEntry.JAR_PREFIX) || fName.startsWith(IBuildEntry.OUTPUT_PREFIX)) {
			IResource resource = getModel().getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (project != null) {
					IContainer root = PDEProject.getBundleRoot(project);
					if (root != null && !root.equals(project)) {
						// translation required for source. and output. entries
						return root.getProjectRelativePath();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Makes the token a bundle root relative path
	 * 
	 * @param token token
	 * @param root bundle root path
	 * @return bundle relative token
	 */
	String toRelative(String token, IPath root) {
		if (root == null) {
			return token;
		}
		return (new Path(token)).makeRelativeTo(root).toPortableString();
	}

	/**
	 * Makes the token a project relative path
	 * 
	 * @param token token
	 * @param root bundle root path
	 * @return project relative token
	 */
	String fromRelative(String token, IPath root) {
		if (root == null) {
			return token;
		}
		return root.append(new Path(token)).toPortableString();
	}

}
