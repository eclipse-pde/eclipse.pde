package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.util.*;
import org.eclipse.pde.internal.ui.model.*;

public class BuildEntry implements IBuildEntry, IDocumentKey {

	private int fLineSpan = -1;
	private int fOffset = -1;
	private IBuildModel fModel;
	private String fName;
	private ArrayList fTokens = new ArrayList();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#addToken(java.lang.String)
	 */
	public void addToken(String token) throws CoreException {
		fTokens.add(token);
		getModel().fireModelChanged(
			new ModelChangedEvent(getModel(), 
				IModelChangedEvent.CHANGE,
				new Object[] { this },
				null));
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
		getModel().fireModelChanged(
			new ModelChangedEvent(getModel(),
				IModelChangedEvent.CHANGE,
				new Object[] { this },
				null));
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#renameToken(java.lang.String, java.lang.String)
	 */
	public void renameToken(String oldToken, String newToken)
			throws CoreException {
		int index = fTokens.indexOf(oldToken);
		if (index != -1) {
			fTokens.set(index, newToken);
			getModel().fireModelChanged(
				new ModelChangedEvent(getModel(),
					IModelChangedEvent.CHANGE,
					new Object[] { this },
					null));
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setName(java.lang.String)
	 */
	public void setName(String name) {
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
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setLineSpan(int)
	 */
	public void setLineSpan(int span) {
		fLineSpan = span;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getLineSpan()
	 */
	public int getLineSpan() {
		return fLineSpan;
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
		StringTokenizer stok = new StringTokenizer(value, ",");
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
