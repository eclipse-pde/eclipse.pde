package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.model.*;

public class Build implements IBuild {
	
	private BuildModel fModel;
	private ArrayList fEntries = new ArrayList();

	public Build(BuildModel model) {
		fModel = model;	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#add(org.eclipse.pde.core.build.IBuildEntry)
	 */
	public void add(IBuildEntry entry) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#getBuildEntries()
	 */
	public IBuildEntry[] getBuildEntries() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#getEntry(java.lang.String)
	 */
	public IBuildEntry getEntry(String name) {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuild#remove(org.eclipse.pde.core.build.IBuildEntry)
	 */
	public void remove(IBuildEntry entry) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
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
			fEntries.add(entry);
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
						currentKey.setLineSpan(i - document.getLineOfOffset(currentKey.getOffset()));
						currentKey = null;
					}
					continue;
				}
				
				line = line.trim();
				if (line.length() == 0)
					continue;
				
				if (currentKey != null) {
					if (!line.endsWith("\\")) {
						currentKey.setLineSpan(i - document.getLineOfOffset(currentKey.getOffset()));
						currentKey = null;
					}
				} else {
					int index = line.indexOf('=');
					if (index == -1) 
						index = line.indexOf(':');
					if (index != -1) {
						String name = line.substring(0, index).trim();
						currentKey = (IDocumentKey)getEntry(name);
						if (currentKey != null)
							currentKey.setOffset(document.getLineOffset(i));
					}
				}
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
