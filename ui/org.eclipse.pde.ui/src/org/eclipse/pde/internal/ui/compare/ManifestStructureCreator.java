/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import java.io.*;
import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.*;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ManifestPartitionScanner;
import org.eclipse.swt.graphics.Image;

public class ManifestStructureCreator extends StructureCreator {

	static class ManifestNode extends DocumentRangeNode implements ITypedElement {

		public ManifestNode(DocumentRangeNode parent, int type, String id, IDocument doc, int start, int length) {
			super(parent, type, id, doc, start, length);
			if (parent != null) {
				parent.addChild(ManifestNode.this);
			}
		}

		public String getName() {
			return this.getId();
		}

		public String getType() {
			return "MF2"; //$NON-NLS-1$
		}

		public Image getImage() {
			return CompareUI.getImage(getType());
		}
	}

	public String getName() {
		return PDEUIMessages.ManifestStructureCreator_name;
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

	private void parseManifest(DocumentRangeNode root, IDocument doc, IProgressMonitor monitor) throws IOException {
		int lineStart = 0;
		int[] args = new int[2];
		args[0] = 0; // here we return the line number
		args[1] = 0; // and here the offset of the first character of the line 

		try {
			String id = "Manifest"; //$NON-NLS-1$
			ManifestNode parent = new ManifestNode(root, 0, id, doc, 0, doc.getLength());
			monitor = beginWork(monitor);
			StringBuffer headerBuffer = new StringBuffer();
			int headerStart = 0;
			while (true) {
				lineStart = args[1]; // start of current line
				String line = readLine(args, doc);
				if (line == null)
					return;

				if (line.length() <= 0) {
					saveNode(parent, doc, headerBuffer.toString(), headerStart); // empty line, save buffer to node
					continue;
				}
				if (line.charAt(0) == ' ') {
					if (headerBuffer.length() > 0)
						headerBuffer.append(line);
					continue;
				}

				// save old buffer and start loading again
				saveNode(parent, doc, headerBuffer.toString(), headerStart);

				headerStart = lineStart;
				headerBuffer.replace(0, headerBuffer.length(), line);
				worked(monitor);
			}
		} finally {
			monitor.done();
		}
	}

	private void worked(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			throw new OperationCanceledException();
		monitor.worked(1);
	}

	private IProgressMonitor beginWork(IProgressMonitor monitor) {
		if (monitor == null)
			return new NullProgressMonitor();
		return new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN);
	}

	private void saveNode(DocumentRangeNode root, IDocument doc, String header, int start) {
		if (header.length() > 0)
			new ManifestNode(root, 1, extractKey(header), doc, start, header.length());
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
				args[1] = doc.getLineOffset(line + 1);
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
		BufferedReader reader = null;
		try {
			StringBuffer buffer = new StringBuffer();
			char[] part = new char[2048];
			int read = 0;
			reader = new BufferedReader(new InputStreamReader(is, encoding));

			while ((read = reader.read(part)) != -1)
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

	public static String readString(IStreamContentAccessor sa) throws CoreException {
		InputStream is = sa.getContents();
		if (is != null) {
			String encoding = null;
			if (sa instanceof IEncodedStreamContentAccessor) {
				try {
					encoding = ((IEncodedStreamContentAccessor) sa).getCharset();
				} catch (Exception e) {
				}
			}
			if (encoding == null)
				encoding = ResourcesPlugin.getEncoding();
			return readString(is, encoding);
		}
		return null;
	}

	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new ManifestPartitionScanner(), ManifestPartitionScanner.PARTITIONS);
	}

	protected String getDocumentPartitioning() {
		return ManifestPartitionScanner.MANIFEST_FILE_PARTITIONING;
	}

	protected IStructureComparator createStructureComparator(Object input, IDocument document, ISharedDocumentAdapter adapter, IProgressMonitor monitor) throws CoreException {

		final boolean isEditable;
		if (input instanceof IEditableContent)
			isEditable = ((IEditableContent) input).isEditable();
		else
			isEditable = false;

		DocumentRangeNode rootNode = new StructureRootNode(document, input, this, adapter) {
			public boolean isEditable() {
				return isEditable;
			}
		};
		try {
			parseManifest(rootNode, document, monitor);
		} catch (IOException ex) {
			if (adapter != null)
				adapter.disconnect(input);
			throw new CoreException(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), 0, PDEUIMessages.ManifestStructureCreator_errorMessage, ex));
		}

		return rootNode;
	}

}
