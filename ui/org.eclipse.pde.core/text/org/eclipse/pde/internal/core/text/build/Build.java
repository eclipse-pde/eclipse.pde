/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.build;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.text.IDocumentKey;
import org.eclipse.pde.internal.core.util.PropertiesUtil;

public class Build implements IBuild {

	private BuildModel fModel;
	private HashMap fEntries = new HashMap();

	public Build(BuildModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#add(org.eclipse.pde.core.build.IBuildEntry)
	 */
	public void add(IBuildEntry entry) throws CoreException {
		fEntries.put(entry.getName(), entry);
		fModel.fireModelChanged(new ModelChangedEvent(fModel, IModelChangedEvent.INSERT, new Object[] {entry}, null));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#getBuildEntries()
	 */
	public IBuildEntry[] getBuildEntries() {
		return (IBuildEntry[]) fEntries.values().toArray(new IBuildEntry[fEntries.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#getEntry(java.lang.String)
	 */
	public IBuildEntry getEntry(String name) {
		return (IBuildEntry) fEntries.get(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#remove(org.eclipse.pde.core.build.IBuildEntry)
	 */
	public void remove(IBuildEntry entry) throws CoreException {
		if (fEntries.remove(entry.getName()) != null)
			fModel.fireModelChanged(new ModelChangedEvent(fModel, IModelChangedEvent.REMOVE, new Object[] {entry}, null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}

	public void load(InputStream source) throws IOException {
		fEntries.clear();
		Properties properties = new Properties();
		properties.load(source);
		Enumeration keys = properties.keys();
		while (keys.hasMoreElements()) {
			String name = keys.nextElement().toString();
			BuildEntry entry = (BuildEntry) fModel.getFactory().createEntry(name);
			entry.processEntry(properties.get(name).toString());
			fEntries.put(name, entry);
		}
		adjustOffsets(fModel.getDocument());
	}

	public void adjustOffsets(IDocument document) {
		int lines = document.getNumberOfLines();
		try {
			IDocumentKey currentKey = null;
			for (int i = 0; i < lines; i++) {
				int offset = document.getLineOffset(i);
				int length = document.getLineLength(i);
				String line = document.get(offset, length);
				if (line.startsWith("#") | line.startsWith("!")) { //$NON-NLS-1$ //$NON-NLS-2$
					if (currentKey != null) {
						currentKey.setLength(offset - 1 - currentKey.getOffset());
						currentKey = null;
					}
					continue;
				}

				line = line.trim();
				if (line.length() == 0) {
					// we are at last line, set the length of the last key.
					// we are here because the properties file ends with a trailing unnecessary backslash
					if (currentKey != null && i == lines - 1) {
						currentKey.setLength(offset - 1 - currentKey.getOffset());
						currentKey = null;
					}
					continue;
				}

				if (currentKey != null) {
					if (!line.endsWith("\\")) { //$NON-NLS-1$
						currentKey.setLength(offset + document.getLineLength(i) - currentKey.getOffset());
						currentKey = null;
					}
				} else {
					int index = line.indexOf('=');
					if (index == -1)
						index = line.indexOf(':');
					if (index == -1)
						index = line.indexOf(' ');
					if (index == -1)
						index = line.indexOf('\t');
					String name = (index != -1) ? line.substring(0, index).trim() : line;
					String propertyKey;
					try {
						propertyKey = PropertiesUtil.windEscapeChars(name);
					} catch (IllegalArgumentException iae) {
						propertyKey = name;
					}
					currentKey = (IDocumentKey) getEntry(propertyKey);
					if (currentKey != null) {
						while (Character.isSpaceChar(document.getChar(offset))) {
							offset += 1;
						}
						currentKey.setOffset(offset);
						if (!line.endsWith("\\")) { //$NON-NLS-1$
							currentKey.setLength(document.getLineOffset(i) + document.getLineLength(i) - currentKey.getOffset());
							currentKey = null;
						}
					}
				}
			}
		} catch (BadLocationException e) {
		}
	}

	public BuildModel getModel() {
		return fModel;
	}
}
