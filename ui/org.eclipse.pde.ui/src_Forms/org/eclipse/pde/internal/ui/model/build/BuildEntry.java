package org.eclipse.pde.internal.ui.model.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.ui.model.*;

public class BuildEntry implements IBuildEntry, IDocumentKey {

	private int fLineSpan;
	private int fOffset;
	private IBuildModel fModel;
	private String fName;
	private ArrayList fTokens = new ArrayList();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#addToken(java.lang.String)
	 */
	public void addToken(String token) throws CoreException {
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
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.build.IBuildEntry#renameToken(java.lang.String, java.lang.String)
	 */
	public void renameToken(String oldToken, String newToken)
			throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setName(java.lang.String)
	 */
	public void setName(String name) {
		fName = name;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#setValue(java.lang.String)
	 */
	public void setValue(String value) {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentKey#getValue()
	 */
	public String getValue() {
		return null;
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
}
