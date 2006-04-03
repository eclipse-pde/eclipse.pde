/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.IStructureCreator;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ManifestPartitionScanner;
import org.eclipse.swt.graphics.Image;

public class ManifestStructureCreator implements IStructureCreator {

	private ManifestNode fRootNode;
	private Document fDocument;
	
	static class ManifestNode extends DocumentRangeNode implements ITypedElement {
		
		private boolean fIsEditable;
		private ManifestNode fParent;
		
		public ManifestNode(ManifestNode parent, int type, String id , IDocument doc, int start, int length) {
			super(type, id, doc, start, length);
			fParent = parent;
			if (parent != null) {
				parent.addChild(ManifestNode.this);
				fIsEditable = parent.isEditable();
			}
		}
						
		public ManifestNode(IDocument doc, boolean editable) {
			super(0, "root", doc, 0, doc.getLength()); //$NON-NLS-1$
			fIsEditable = editable;
		}
		
		public String getName() {
			return this.getId();
		}

		public String getType() {
			return "MF"; //$NON-NLS-1$
		}
		
		public Image getImage() {
			return CompareUI.getImage(getType());
		}
		
		public boolean isEditable() {
			return fIsEditable;
		}
		
		public void setContent(byte[] content) {
			super.setContent(content);
			nodeChanged(this);
		}
		
		public ITypedElement replace(ITypedElement child, ITypedElement other) {
			nodeChanged(this);
			return child;
		}

		void nodeChanged(ManifestNode node) {
			if (fParent != null)
				fParent.nodeChanged(node);
		}
	}
	
	public String getName() {
		return PDEUIMessages.ManifestStructureCreator_name;
	}

	public IStructureComparator getStructure(final Object input) {
		String content= null;
		if (input instanceof IStreamContentAccessor) {
			try {
				content = readString(((IStreamContentAccessor) input));
			} catch(CoreException ex) {
				return null;
			}
		}
		fDocument = new Document(content != null ? content : ""); //$NON-NLS-1$
		setupManifestDocument(fDocument);
				
		boolean isEditable= false;
		if (input instanceof IEditableContent)
			isEditable= ((IEditableContent) input).isEditable();

		fRootNode = new ManifestNode(fDocument, isEditable) {
			void nodeChanged(ManifestNode node) {
				save(this, input);
			}
		};
				
		try {
			parseManifest(fRootNode, fDocument);
		} catch (IOException ex) {
		}
		
		return fRootNode;
	}

	public IStructureComparator locate(Object path, Object input) {
		return null;
	}

	public String getContents(Object node, boolean ignoreWhitespace) {
		if (node instanceof IStreamContentAccessor) {
			IStreamContentAccessor sca = (IStreamContentAccessor) node;
			try {
				return readString(sca);
			} catch (CoreException ex) {
			}
		}
		return null;
	}

	public void save(IStructureComparator node, Object input) {
		if (input instanceof IEditableContent && node instanceof ManifestNode) {
			IDocument doc = ((ManifestNode)node).getDocument();
			IEditableContent bca = (IEditableContent) input;
			String content = doc.get();
			bca.setContent(content.getBytes());
		}
	}
	private void parseManifest(ManifestNode root, IDocument doc) throws IOException {
		int lineStart = 0;
		int[] args = new int[2];
		args[0] = 0;	// here we return the line number
		args[1] = 0;	// and here the offset of the first character of the line 
		
		StringBuffer headerBuffer = new StringBuffer();
		int headerStart = 0;
		while (true) {
			lineStart = args[1];		// start of current line
            String line = readLine(args, doc);
			if (line == null)
				return;
				
			if (line.length() <= 0) {
				saveNode(headerBuffer.toString(), headerStart); // empty line, save buffer to node
				continue;
			}
			if (line.charAt(0) == ' ') {
				if (headerBuffer.length() > 0)
					headerBuffer.append(line);
				continue;
			}
			
			// save old buffer and start loading again
			saveNode(headerBuffer.toString(), headerStart);
			
			headerStart = lineStart;
			headerBuffer.replace(0, headerBuffer.length(), line);
		}
	}

	private void saveNode(String header, int start) {
		if (header.length() > 0)
			new ManifestNode(
				fRootNode, 0, extractKey(header),
				fDocument, start, header.length());
	}

	private String extractKey(String headerBuffer) {
		int assign = headerBuffer.indexOf(":"); //$NON-NLS-1$
		if (assign != -1)
			return headerBuffer.substring(0, assign);
		return headerBuffer;
	}

	private String readLine(int[] args, IDocument doc) {
		int line = args[0]++;
		try {
			if (line >= doc.getNumberOfLines())
				return null;
			int start = doc.getLineOffset(line);
			int length = doc.getLineLength(line);
			try {
				args[1] = doc.getLineOffset(line+1);
			} catch (BadLocationException ex) {
				args[1] = doc.getLength();
			}
			
			return doc.get(start, length);
		} catch (BadLocationException ex) {
		}
		return null;
	}
	
	private static String readString(InputStream is, String encoding) {
		if (is == null)
			return null;
		BufferedReader reader= null;
		try {
			StringBuffer buffer= new StringBuffer();
			char[] part= new char[2048];
			int read= 0;
			reader= new BufferedReader(new InputStreamReader(is, encoding));

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
			// NeedWork
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ex) {
					// silently ignored
				}
			}
		}
		return null;
	}
	
	private static String readString(IStreamContentAccessor sa) throws CoreException {
		InputStream is= sa.getContents();
		if (is != null) {
			String encoding= null;
			if (sa instanceof IEncodedStreamContentAccessor) {
				try {
					encoding= ((IEncodedStreamContentAccessor) sa).getCharset();
				} catch (Exception e) {
				}
			}
			if (encoding == null)
				encoding= ResourcesPlugin.getEncoding();
			return readString(is, encoding);
		}
		return null;
	}
	
	private void setupManifestDocument(IDocument document) {
		IDocumentPartitioner partitioner= new FastPartitioner(new ManifestPartitionScanner(), ManifestPartitionScanner.PARTITIONS);
		document.setDocumentPartitioner(partitioner);
		partitioner.connect(document);
	}
}
