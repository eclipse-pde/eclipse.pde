/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.build;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.PropertiesUtil;

public class BuildEntry extends BuildObject implements IBuildEntry {
	private final Vector<String> tokens = new Vector<>();
	private String name;

	public BuildEntry(String name) {
		this.name = name;
	}

	@Override
	public void addToken(String token) throws CoreException {
		ensureModelEditable();
		tokens.add(token);
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.INSERT, new Object[] {token}, null));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String[] getTokens() {
		String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}

	@Override
	public boolean contains(String token) {
		return tokens.contains(token);
	}

	void processEntry(String value) {
		IPath rootPath = getRootPath();
		StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			token = token.trim();
			token = fromRelative(token, rootPath);
			tokens.add(token);
		}
	}

	@Override
	public void removeToken(String token) throws CoreException {
		ensureModelEditable();
		tokens.remove(token);
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.REMOVE, new Object[] {token}, null));
	}

	@Override
	public void renameToken(String oldName, String newName) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.elementAt(i).toString().equals(oldName)) {
				tokens.setElementAt(newName, i);
				break;
			}
		}
		getModel().fireModelChanged(new ModelChangedEvent(getModel(), IModelChangedEvent.CHANGE, new Object[] {oldName}, null));
	}

	@Override
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		String oldValue = this.name;
		this.name = name;
		getModel().fireModelObjectChanged(this, P_NAME, oldValue, name);
	}

	@Override
	public String toString() {
		return name + " = " + tokens; //$NON-NLS-1$
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		Enumeration<String> elements = tokens.elements();
		IPath rootPath = getRootPath();
		if (rootPath != null) {
			// translation required for source. and output. entries
			Vector<String> vector = new Vector<>();
			while (elements.hasMoreElements()) {
				String e = elements.nextElement();
				vector.add(toRelative(e, rootPath));
			}
			elements = vector.elements();
		}
		PropertiesUtil.writeKeyValuePair(indent, name, elements, writer);
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		}
	}

	/**
	 * Returns the path that this entries tokens are relative to, or <code>null</code> if none.
	 *
	 * @return relative root path, or <code>null</code>
	 */
	IPath getRootPath() {
		if (name.startsWith(IBuildEntry.JAR_PREFIX) || name.startsWith(IBuildEntry.OUTPUT_PREFIX)) {
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
	 * @param root bundle root path or <code>null</code>
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
	 * @param root bundle root path or <code>null</code>
	 * @return project relative token
	 */
	String fromRelative(String token, IPath root) {
		if (root == null) {
			return token;
		}
		return root.append(new Path(token)).toPortableString();
	}
}
