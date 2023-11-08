/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
*******************************************************************************/
package org.eclipse.pde.internal.ui.compare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.compare.CompareUI;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.compare.structuremergeviewer.StructureCreator;
import org.eclipse.compare.structuremergeviewer.StructureRootNode;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
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

		@Override
		public String getName() {
			return this.getId();
		}

		@Override
		public String getType() {
			return "MF2"; //$NON-NLS-1$
		}

		@Override
		public Image getImage() {
			return CompareUI.getImage(getType());
		}
	}

	@Override
	public String getName() {
		return PDEUIMessages.ManifestStructureCreator_name;
	}

	@Override
	public IStructureComparator locate(Object path, Object input) {
		return null;
	}

	@Override
	public String getContents(Object node, boolean ignoreWhitespace) {
		if (node instanceof IStreamContentAccessor sca) {
			try {
				return readString(sca);
			} catch (CoreException ex) {
			}
		}
		return null;
	}

	/**
	 * @param root
	 * @param doc
	 * @param monitor
	 * @throws IOException
	 */
	private void parseManifest(DocumentRangeNode root, IDocument doc, IProgressMonitor monitor) throws IOException {
		int lineStart = 0;
		int[] args = new int[2];
		args[0] = 0; // here we return the line number
		args[1] = 0; // and here the offset of the first character of the line

		String id = "Manifest"; //$NON-NLS-1$
		ManifestNode parent = new ManifestNode(root, 0, id, doc, 0, doc.getLength());
		SubMonitor subMonitor = SubMonitor.convert(monitor).split(1);
		StringBuilder headerBuffer = new StringBuilder();
		int headerStart = 0;
		while (true) {
			lineStart = args[1]; // start of current line
			String line = readLine(args, doc);
			if (line == null)
				return;

			if (line.length() <= 0) {
				saveNode(parent, doc, headerBuffer.toString(), headerStart); // empty
																				// line,
																				// save
																				// buffer
																				// to
																				// node
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
			subMonitor.worked(1);
		}
	}

	private void saveNode(DocumentRangeNode root, IDocument doc, String header, int start) {
		if (header.length() > 0)
			new ManifestNode(root, 1, extractKey(header), doc, start, header.length());
	}

	private String extractKey(String headerBuffer) {
		int assign = headerBuffer.indexOf(':');
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
		StringBuilder buffer = new StringBuilder();
		char[] part = new char[2048];
		int read = 0;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding))) {
			while ((read = reader.read(part)) != -1)
				buffer.append(part, 0, read);

			return buffer.toString();

		} catch (IOException ex) {
			// NeedWork
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

	@Override
	protected IDocumentPartitioner getDocumentPartitioner() {
		return new FastPartitioner(new ManifestPartitionScanner(), ManifestPartitionScanner.PARTITIONS);
	}

	@Override
	protected String getDocumentPartitioning() {
		return ManifestPartitionScanner.MANIFEST_FILE_PARTITIONING;
	}

	@Override
	protected IStructureComparator createStructureComparator(Object input, IDocument document, ISharedDocumentAdapter adapter, IProgressMonitor monitor) throws CoreException {

		final boolean isEditable;
		if (input instanceof IEditableContent)
			isEditable = ((IEditableContent) input).isEditable();
		else
			isEditable = false;

		DocumentRangeNode rootNode = new StructureRootNode(document, input, this, adapter) {
			@Override
			public boolean isEditable() {
				return isEditable;
			}
		};
		try {
			parseManifest(rootNode, document, monitor);
		} catch (IOException ex) {
			if (adapter != null)
				adapter.disconnect(input);
			throw new CoreException(Status.error(PDEUIMessages.ManifestStructureCreator_errorMessage, ex));
		}

		return rootNode;
	}

}
