package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.model.*;

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
		fModel.fireModelChanged(new ModelChangedEvent(fModel,
				IModelChangedEvent.INSERT, new Object[]{entry}, null));
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#getBuildEntries()
	 */
	public IBuildEntry[] getBuildEntries() {
		return (IBuildEntry[])fEntries.values().toArray(new IBuildEntry[fEntries.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#getEntry(java.lang.String)
	 */
	public IBuildEntry getEntry(String name) {
		return (IBuildEntry)fEntries.get(name);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#remove(org.eclipse.pde.core.build.IBuildEntry)
	 */
	public void remove(IBuildEntry entry) throws CoreException {
		fEntries.remove(entry.getName());
		fModel.fireModelChanged(new ModelChangedEvent(fModel,
				IModelChangedEvent.REMOVE, new Object[]{entry}, null));
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
	
	public void load(Properties properties) {
		fEntries.clear();
		Enumeration enum = properties.keys();
		while (enum.hasMoreElements()) {
			String name = enum.nextElement().toString();
			BuildEntry entry = (BuildEntry)fModel.getFactory().createEntry(name);
			entry.processEntry(properties.get(name).toString());
			fEntries.put(name, entry);
		}
		addOffsets();
	}
	
	private void addOffsets() {
		IDocument document = fModel.getDocument();
		int lines = document.getNumberOfLines();
		try {
			IDocumentKey currentKey = null;
			for (int i = 0; i < lines; i++) {
				int offset = document.getLineOffset(i);
				int length = document.getLineLength(i);
				String line = document.get(offset, length);
				if (line.startsWith("#") | line.startsWith("!")) {
					if (currentKey != null) {
						currentKey.setLength(offset - 1 - currentKey.getOffset());
						currentKey = null;
					}
					continue;
				}
				
				line = line.trim();
				if (line.length() == 0)
					continue;
				
				if (currentKey != null) {
					if (!line.endsWith("\\")) {
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
					currentKey = (IDocumentKey)getEntry(name);
					if (currentKey != null) {
						while (Character.isSpaceChar(document.getChar(offset))) {
							offset += 1;
						}
						currentKey.setOffset(offset);
						if (!line.endsWith("\\")) {
							currentKey.setLength(document.getLineOffset(i) + document.getLineLength(i) - currentKey.getOffset());
							currentKey = null;
						}
					}
				}
			}
		} catch (BadLocationException e) {
		}
	}

}
