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
import org.eclipse.pde.internal.ui.editor.contentassist.XMLContentAssistProcessor.VSchemaObject;
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
		if (sel == null)
			return;
		
		fLen = sel.getLength() + sel.getOffset() - fOffset;
		String delim = TextUtilities.getDefaultLineDelimiter(document);
		StringBuffer sb = new StringBuffer();
		boolean doInternalWork = false;
		if (fSchemaObject == null && fRange instanceof IDocumentNode) {
			// we are opening up an element
			fSelOffset = fOffset;
			fOffset -= 1;
			fLen = 2;
			sb.append("></"); //$NON-NLS-1$
			sb.append(((IDocumentNode)fRange).getXMLTagName());
			sb.append('>');
		} else if (fSchemaObject instanceof ISchemaAttribute) {
			if (fRange == null) {
				// hacky hacky need to modify offsets
				fLen -= 1;
				fOffset += 1;
			}
			String attName = ((ISchemaAttribute)fSchemaObject).getName();
			sb.append(attName);
			sb.append("=\""); //$NON-NLS-1$
			fSelOffset = fOffset + sb.length();
			String value = XMLInsertionComputer.generateAttributeValue((ISchemaAttribute)fSchemaObject, fProcessor.getModel());
			sb.append(value);
			fSelLen = value.length();
			sb.append('"');
		} else if (fSchemaObject instanceof ISchemaElement) {
			sb.append('<');
			sb.append(((ISchemaElement)fSchemaObject).getName());
			sb.append('>'); //$NON-NLS-1$
			sb.append(delim);
			sb.append(getIndent(document, fOffset));
			sb.append('<');
			sb.append('/');
			sb.append(((ISchemaElement)fSchemaObject).getName());
			sb.append('>');
			doInternalWork = true;
		} else if (fSchemaObject instanceof VSchemaObject) {
			int type = ((VSchemaObject)fSchemaObject).getVType();
			switch (type) {
			case XMLContentAssistProcessor.F_AT:
				break;
			case XMLContentAssistProcessor.F_CL:
				fOffset = sel.getOffset();
				fLen = 0;
				sb.append(" />"); //$NON-NLS-1$
				break;
			case XMLContentAssistProcessor.F_EX:
				sb.append("<extension"); //$NON-NLS-1$
				sb.append(delim);
				String indent = getIndent(document, fOffset);
				sb.append(indent);
				sb.append(F_DEF_ATTR_INDENT);
				sb.append("point=\"\">"); //$NON-NLS-1$
				fSelOffset = fOffset + sb.length() - 2; // position rigth inside new point="" attribute
				sb.append(delim);
				sb.append(indent);
				sb.append("</extension>"); //$NON-NLS-1$
				break;
			case XMLContentAssistProcessor.F_EP:
				String id = "id"; //$NON-NLS-1$
				sb.append("<extension-point id=\""); //$NON-NLS-1$
				fSelOffset = fOffset + sb.length();
				fSelLen = id.length();
				sb.append(id);
				sb.append("\" name=\"name\" />"); //$NON-NLS-1$
				break;
			case XMLContentAssistProcessor.F_AT_EP:
				doInternalWork = true; // we will want to add required child nodes/attributes
			case XMLContentAssistProcessor.F_AT_VAL:
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
					sb.append(value);
					fSelOffset = fOffset + value.length();
				}
				break;
			}
		}
		if (sb.length() == 0)
			return;
		try {
			document.replace(fOffset, fLen, sb.toString());
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
		}
		
		if (doInternalWork)
			modifyModel(document);
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
			
			if (fSchemaObject instanceof VSchemaObject) {
				switch (((VSchemaObject)fSchemaObject).getVType()) {
				case XMLContentAssistProcessor.F_AT_EP:
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
			
			if (pluginParent != null && schemaElement != null) {
				XMLInsertionComputer.computeInsertion(schemaElement, pluginParent);
				fProcessor.flushDocument();
				if (model instanceof AbstractEditingModel) {
					((AbstractEditingModel)model).adjustOffsets(document);
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
		if (fSchemaObject == null)
			return null;
		return fSchemaObject.getDescription();
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public String getDisplayString() {
		if (fSchemaObject instanceof VSchemaObject) {
			switch (((VSchemaObject)fSchemaObject).getVType()) {
			case XMLContentAssistProcessor.F_CL:
				return "... />"; //$NON-NLS-1$
			case XMLContentAssistProcessor.F_AT_EP:
			case XMLContentAssistProcessor.F_AT_VAL:
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
		if (fSchemaObject instanceof VSchemaObject)
			return fProcessor.getImage(((VSchemaObject)fSchemaObject).getVType());
		if (fSchemaObject instanceof ISchemaAttribute)
			return fProcessor.getImage(XMLContentAssistProcessor.F_AT);
		if (fSchemaObject instanceof ISchemaElement || fSchemaObject == null)
			return fProcessor.getImage(XMLContentAssistProcessor.F_EL);
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
