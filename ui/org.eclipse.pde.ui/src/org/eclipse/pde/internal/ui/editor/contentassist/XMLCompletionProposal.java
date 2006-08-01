/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.HashSet;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class XMLCompletionProposal implements ICompletionProposal, ICompletionProposalExtension5 {
	
	private static final String F_DEF_ATTR_INDENT = "      "; //$NON-NLS-1$
	
	private ISchemaObject fSchemaObject;
	private IDocumentRange fRange;
	private int fOffset; 
	private int fLen;
	private int fSelOffset; 
	private int fSelLen;
	private XMLContentAssistProcessor fProcessor;
	
	public XMLCompletionProposal(IDocumentRange node, ISchemaObject object, int offset, XMLContentAssistProcessor processor) {
		fLen = -1;
		fSelOffset = -1;
		fSelLen = 0;
		fRange = node;
		fSchemaObject = object;
		fOffset = offset;
		fProcessor = processor;
	}

	public void apply(IDocument document) {
		ITextSelection sel = fProcessor.getCurrentSelection();
		if (sel == null) {
			return;
		}
		fLen = sel.getLength() + sel.getOffset() - fOffset;
		String delim = TextUtilities.getDefaultLineDelimiter(document);
		StringBuffer documentInsertBuffer = new StringBuffer();
		boolean doInternalWork = false;
		// Generate the text to apply depending on the proposal type
		if (fSchemaObject instanceof ISchemaAttribute) {
			applyAttribute(documentInsertBuffer);
		} else if (fSchemaObject instanceof ISchemaElement) {
			applyElement(getIndent(document, fOffset), delim, documentInsertBuffer);
			doInternalWork = true;
		} else if (fSchemaObject instanceof VirtualSchemaObject) {
			doInternalWork = applyVirtual(document, sel, delim, documentInsertBuffer, doInternalWork);
		}
		// Check if there is anything to apply
		if (documentInsertBuffer.length() == 0) {
			return;
		}
		// Apply the proposal to the document
		try {
			document.replace(fOffset, fLen, documentInsertBuffer.toString());
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
		}
		// Update the model if necessary
		if (doInternalWork) {
			modifyModel(document);
		}
	}

	/**
	 * @param document
	 * @param sel
	 * @param delim
	 * @param documentInsertBuffer
	 * @param doInternalWork
	 * @return
	 */
	private boolean applyVirtual(IDocument document, ITextSelection sel, String delim, StringBuffer documentInsertBuffer, boolean doInternalWork) {
		int type = ((VirtualSchemaObject)fSchemaObject).getVType();
		switch (type) {
		case XMLContentAssistProcessor.F_ATTRIBUTE:
			applyAttribute(documentInsertBuffer);
			break;
		case XMLContentAssistProcessor.F_CLOSE_TAG:
			fOffset = sel.getOffset();
			fLen = 0;
			documentInsertBuffer.append(" />"); //$NON-NLS-1$
			break;
		case XMLContentAssistProcessor.F_EXTENSION:
			applyExtension(document, delim, documentInsertBuffer);
			break;
		case XMLContentAssistProcessor.F_EXTENSION_POINT:
			applyExtensionPoint(documentInsertBuffer);
			break;
		case XMLContentAssistProcessor.F_EXTENSION_ATTRIBUTE_POINT_VALUE:
			doInternalWork = true; // we will want to add required child nodes/attributes
		case XMLContentAssistProcessor.F_ATTRIBUTE_VALUE:
			applyAttributeValue(document, documentInsertBuffer);
			break;
		}
		return doInternalWork;
	}

	/**
	 * @param document
	 * @param documentInsertBuffer
	 */
	private void applyAttributeValue(IDocument document, StringBuffer documentInsertBuffer) {
		if (fRange instanceof IDocumentAttribute) {
			fOffset = ((IDocumentAttribute)fRange).getValueOffset();
			String value = fSchemaObject.getName();
			try {
				// add indentation
				int off = fOffset;
				int docLen = document.getLength();
				fLen = 0;
				while (off < docLen) {
					char c = document.getChar(off++);
					if (c == '"')
						break;
					fLen += 1;
				}
			} catch (BadLocationException e) {
			}
			documentInsertBuffer.append(value);
			fSelOffset = fOffset + value.length();
		}
	}

	/**
	 * @param documentInsertBuffer
	 */
	private void applyExtensionPoint(StringBuffer documentInsertBuffer) {
		// TODO: Generate XML representation using model objects rather than
		// hardcodings at a later date
		String id = "id"; //$NON-NLS-1$
		documentInsertBuffer.append("<extension-point id=\""); //$NON-NLS-1$
		fSelOffset = fOffset + documentInsertBuffer.length();
		fSelLen = id.length();
		documentInsertBuffer.append(id);
		documentInsertBuffer.append("\" name=\"name\" />"); //$NON-NLS-1$
	}

	/**
	 * @param document
	 * @param delim
	 * @param documentInsertBuffer
	 */
	private void applyExtension(IDocument document, String delim, StringBuffer documentInsertBuffer) {
		// TODO: Generate XML representation using model objects rather than
		// hardcodings at a later date
		documentInsertBuffer.append("<extension"); //$NON-NLS-1$
		documentInsertBuffer.append(delim);
		String indent = getIndent(document, fOffset);
		documentInsertBuffer.append(indent);
		documentInsertBuffer.append(F_DEF_ATTR_INDENT);
		documentInsertBuffer.append("point=\"\">"); //$NON-NLS-1$
		fSelOffset = fOffset + documentInsertBuffer.length() - 2; // position rigth inside new point="" attribute
		documentInsertBuffer.append(delim);
		documentInsertBuffer.append(indent);
		documentInsertBuffer.append("</extension>"); //$NON-NLS-1$
	}

	/**
	 * @param document
	 * @param delim
	 * @param sb
	 */
	private void applyElement(String indent, String delim, StringBuffer documentInsertBuffer) {
		// TODO: Generate XML representation using model objects rather than
		// hardcodings at a later date
		documentInsertBuffer.append('<');
		documentInsertBuffer.append(((ISchemaElement)fSchemaObject).getName());
		documentInsertBuffer.append('>'); 
		documentInsertBuffer.append(delim);
		documentInsertBuffer.append(indent);
		documentInsertBuffer.append('<');
		documentInsertBuffer.append('/');
		documentInsertBuffer.append(((ISchemaElement)fSchemaObject).getName());
		documentInsertBuffer.append('>');
	}

	/**
	 * @param sb
	 */
	private void applyAttribute(StringBuffer documentInsertBuffer) {
		// TODO: Generate XML representation using model objects rather than
		// hardcodings at a later date
		if (fRange == null) {
			// Model is broken
			// Manually adjust offsets
			fLen -= 1;
			fOffset += 1;
		}
		String attName = fSchemaObject.getName();
		documentInsertBuffer.append(attName);
		documentInsertBuffer.append("=\""); //$NON-NLS-1$
		fSelOffset = fOffset + documentInsertBuffer.length();
		String value = ""; //$NON-NLS-1$
		if (fSchemaObject instanceof ISchemaAttribute) {
			value = XMLInsertionComputer.generateAttributeValue((ISchemaAttribute)fSchemaObject, fProcessor.getModel());
		}
		documentInsertBuffer.append(value);
		fSelLen = value.length();
		documentInsertBuffer.append('"');
	}
	
	private void modifyModel(IDocument document) {
		// TODO requires
		//  - refactoring
		//  - better grouping of cases (if statements)
		IBaseModel model = fProcessor.getModel();
		if (model instanceof IReconcilingParticipant)
			((IReconcilingParticipant)model).reconciled(document);
		
		if (model instanceof IPluginModelBase) {
			IPluginBase base = ((IPluginModelBase)model).getPluginBase();
			
			IPluginParent pluginParent = null;
			ISchemaElement schemaElement = null;
			
			if (fSchemaObject instanceof VirtualSchemaObject) {
				switch (((VirtualSchemaObject)fSchemaObject).getVType()) {
				case XMLContentAssistProcessor.F_EXTENSION_ATTRIBUTE_POINT_VALUE:
					if (!(fRange instanceof IDocumentAttribute))
						break;
					int offset = ((IDocumentAttribute)fRange).getEnclosingElement().getOffset();
					IPluginExtension[] extensions = base.getExtensions();
					for (int i = 0; i < extensions.length; i++) {
						if (((IDocumentNode)extensions[i]).getOffset() == offset) {
							if (extensions[i].getChildCount() != 0)
								break; // don't modify existing extensions
							pluginParent = extensions[i];
							schemaElement = XMLUtil.getSchemaElement(
									(IDocumentNode)extensions[i],
									extensions[i].getPoint());
							break;
						}
					}
					break;
				}
			} else if (fRange instanceof IDocumentNode && base instanceof IDocumentNode) {
				Stack s = new Stack();
				IDocumentNode node = (IDocumentNode)fRange;
				IDocumentNode newSearch = (IDocumentNode)base;
				// traverse up old model, pushing all nodes onto the stack along the way
				while (node != null && !(node instanceof IPluginBase)) {
					s.push(node);
					node = node.getParentNode();
				}
				
				// traverse down new model to find new node, using stack as a guideline
				while (!s.isEmpty()) {
					node = (IDocumentNode)s.pop();
					int nodeIndex = 0;
					while ((node = node.getPreviousSibling()) != null)
						nodeIndex += 1;
					newSearch = newSearch.getChildAt(nodeIndex);
				}
				if (newSearch != null) {
					IDocumentNode[] children = newSearch.getChildNodes();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getOffset() == fOffset && 
								children[i] instanceof IPluginElement) {
							pluginParent = (IPluginElement)children[i];
							schemaElement = (ISchemaElement)fSchemaObject; 
							break;
						}
					}
				}
			}
			
			if (pluginParent != null && schemaElement != null) {
				XMLInsertionComputer.computeInsertion(schemaElement, pluginParent);
				fProcessor.flushDocument();
				if (model instanceof AbstractEditingModel) {
					try {
						((AbstractEditingModel)model).adjustOffsets(document);
					} catch (CoreException e) {
					}
					setSelectionOffsets(document, schemaElement, pluginParent);
				}
			}
		}
	}
	
	private void setSelectionOffsets(IDocument document, ISchemaElement schemaElement, IPluginParent pluginParent) {
		if (pluginParent instanceof IPluginExtension) {
			String point = ((IPluginExtension)pluginParent).getPoint();
			IPluginObject[] children = ((IPluginExtension)pluginParent).getChildren();
			if (children != null && children.length > 0 && children[0] instanceof IPluginParent) {
				pluginParent = (IPluginParent)children[0];
				schemaElement = XMLUtil.getSchemaElement((IDocumentNode)pluginParent, point);
			}
		}
		
		if (pluginParent instanceof IPluginElement) {
			int offset = ((IDocumentNode)pluginParent).getOffset();
			int len = ((IDocumentNode)pluginParent).getLength();
			String value = null;
			try {
				value = document.get(offset, len);
			} catch (BadLocationException e) {
			}
			if (((IPluginElement)pluginParent).getAttributeCount() > 0) {
				// Select value of first required attribute
				IPluginAttribute att = ((IPluginElement)pluginParent).getAttributes()[0];
				if (att instanceof PluginAttribute) {
					fSelOffset = ((PluginAttribute)att).getValueOffset();
					fSelLen = ((PluginAttribute)att).getValueLength();
				}
			} else if (XMLInsertionComputer.hasOptionalChildren(schemaElement, false, new HashSet()) && value != null) {
				// TODO: MP:  PERFORMANCE IMPROVEMENT MARKER
				// position caret for element insertion
				int ind = value.indexOf('>');
				if (ind > 0) {
					fSelOffset = offset + ind + 1;
					fSelLen = 0;
				}
			} else if (XMLInsertionComputer.hasOptionalAttributes(schemaElement) && value != null) {
				// TODO: MP:  PERFORMANCE IMPROVEMENT MARKER
				// position caret for attribute insertion
				int ind = value.indexOf('>');
				if (ind != -1) {
					fSelOffset = offset + ind;
					fSelLen = 0;
				}
			} else {
				// position caret after element
				fSelOffset = offset + len;
				fSelLen = 0;
			}
		}
	}
	
	private String getIndent(IDocument document, int offset) {
		StringBuffer indBuff = new StringBuffer();
		try {
			// add indentation
			int line = document.getLineOfOffset(offset);
			int lineOffset = document.getLineOffset(line); 
			int indent = offset - lineOffset;
			char[] indentChars = document.get(lineOffset, indent).toCharArray();
			// for every tab append a tab, for anything else append a space
			for (int i = 0; i < indentChars.length; i++)
				indBuff.append(indentChars[i] == '\t' ? '\t' : ' ');
		} catch (BadLocationException e) {
		}
		return indBuff.toString();
	}
	
	public String getAdditionalProposalInfo() {
		if (fSchemaObject == null) {
			return null;
		}
		return fSchemaObject.getDescription();
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public String getDisplayString() {
		if (fSchemaObject instanceof VirtualSchemaObject) {
			switch (((VirtualSchemaObject)fSchemaObject).getVType()) {
			case XMLContentAssistProcessor.F_CLOSE_TAG:
				return "... />"; //$NON-NLS-1$
			case XMLContentAssistProcessor.F_EXTENSION_ATTRIBUTE_POINT_VALUE:
			case XMLContentAssistProcessor.F_ATTRIBUTE_VALUE:
				return fSchemaObject.getName();
			}
		}
		if (fSchemaObject instanceof ISchemaAttribute)
			return fSchemaObject.getName();
		if (fSchemaObject != null)
			return fSchemaObject.getName();
		if (fRange instanceof IDocumentNode)
			return "...> </" + ((IDocumentNode)fRange).getXMLTagName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	public Image getImage() {
		if (fSchemaObject instanceof VirtualSchemaObject)
			return fProcessor.getImage(((VirtualSchemaObject)fSchemaObject).getVType());
		if (fSchemaObject instanceof ISchemaAttribute)
			return fProcessor.getImage(XMLContentAssistProcessor.F_ATTRIBUTE);
		if (fSchemaObject instanceof ISchemaElement || fSchemaObject == null)
			return fProcessor.getImage(XMLContentAssistProcessor.F_ELEMENT);
		return null;
	}

	public Point getSelection(IDocument document) {
		if (fSelOffset == -1)
			return null;
		return new Point(fSelOffset, fSelLen);
	}

	public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
		return getAdditionalProposalInfo();
	}
	
}
