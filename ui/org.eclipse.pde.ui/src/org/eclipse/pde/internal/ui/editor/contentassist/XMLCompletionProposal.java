/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 227105
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.HashSet;
import java.util.Stack;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;
import org.eclipse.pde.internal.ui.util.TextUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

public class XMLCompletionProposal implements ICompletionProposal, ICompletionProposalExtension3, ICompletionProposalExtension4, ICompletionProposalExtension5 {

	private static final String F_DEF_ATTR_INDENT = "      "; //$NON-NLS-1$

	private ISchemaObject fSchemaObject;
	private IDocumentRange fRange;
	private int fOffset;
	private int fLen;
	private int fSelOffset;
	private int fSelLen;
	private XMLContentAssistProcessor fProcessor;
	private String fAddInfo;
	private IInformationControlCreator fCreator;

	private IPluginParent fPluginParent;
	private ISchemaElement fSchemaElement;

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
		if (fLen < 0) {
			// If the cursor is moved after the popup is opened sometimes fLen can be negative, see bug 266083
			fLen = 0;
		}
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
	 */
	private boolean applyVirtual(IDocument document, ITextSelection sel, String delim, StringBuffer documentInsertBuffer, boolean doInternalWork) {
		int type = ((VirtualSchemaObject) fSchemaObject).getVType();
		switch (type) {
			case XMLContentAssistProcessor.F_ATTRIBUTE :
				applyAttribute(documentInsertBuffer);
				break;
			case XMLContentAssistProcessor.F_CLOSE_TAG :
				fOffset = sel.getOffset();
				fLen = 0;
				documentInsertBuffer.append(" />"); //$NON-NLS-1$
				break;
			case XMLContentAssistProcessor.F_EXTENSION :
				applyExtension(document, delim, documentInsertBuffer);
				break;
			case XMLContentAssistProcessor.F_EXTENSION_POINT :
				applyExtensionPoint(documentInsertBuffer);
				break;
			case XMLContentAssistProcessor.F_EXTENSION_POINT_AND_VALUE :
				doInternalWork = true; // we will want to add required child nodes/attributes
				applyExtensionFullPoint(document, delim, documentInsertBuffer);
				break;
			case XMLContentAssistProcessor.F_EXTENSION_ATTRIBUTE_POINT_VALUE :
				doInternalWork = true; // we will want to add required child nodes/attributes
			case XMLContentAssistProcessor.F_ATTRIBUTE_VALUE :
			case XMLContentAssistProcessor.F_ATTRIBUTE_BOOLEAN_VALUE :
			case XMLContentAssistProcessor.F_ATTRIBUTE_ID_VALUE :
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
		if (fRange instanceof IDocumentAttributeNode) {
			fOffset = ((IDocumentAttributeNode) fRange).getValueOffset();
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
	 * @param documentInsertBuffer
	 */
	private void applyExtensionFullPoint(IDocument document, String delim, StringBuffer documentInsertBuffer) {

		String pointID = fSchemaObject.getName();
		String indent = getIndent(document, fOffset);
		// Add extension mark-up to the buffer right up until the point 
		// attribute value
		documentInsertBuffer.append('<');
		documentInsertBuffer.append("extension"); //$NON-NLS-1$
		documentInsertBuffer.append(delim);
		documentInsertBuffer.append(indent);
		documentInsertBuffer.append(F_DEF_ATTR_INDENT);
		documentInsertBuffer.append("point"); //$NON-NLS-1$
		documentInsertBuffer.append('=');
		documentInsertBuffer.append('"');
		// Calculate the offset for the start of the selection
		// We want to select the point attribute value in between the quotes
		// fOffset is the point where content assist was first invoked
		fSelOffset = fOffset + documentInsertBuffer.length();
		// Calculate the selection length
		fSelLen = pointID.length();
		// Add extension mark-up to the buffer including the point attribute
		// value and beyond
		documentInsertBuffer.append(pointID);
		documentInsertBuffer.append('"');
		documentInsertBuffer.append('>');
		documentInsertBuffer.append(delim);
		documentInsertBuffer.append(indent);
		documentInsertBuffer.append('<');
		documentInsertBuffer.append('/');
		documentInsertBuffer.append("extension"); //$NON-NLS-1$
		documentInsertBuffer.append('>');
	}

	/**
	 * @param document
	 * @param delim
	 * @param sb
	 */
	private void applyElement(String indent, String delim, StringBuffer documentInsertBuffer) {
		documentInsertBuffer.append('<');
		documentInsertBuffer.append(((ISchemaElement) fSchemaObject).getName());
		documentInsertBuffer.append('>');
		documentInsertBuffer.append(delim);
		documentInsertBuffer.append(indent);
		documentInsertBuffer.append('<');
		documentInsertBuffer.append('/');
		documentInsertBuffer.append(((ISchemaElement) fSchemaObject).getName());
		documentInsertBuffer.append('>');
	}

	/**
	 * @param sb
	 */
	private void applyAttribute(StringBuffer documentInsertBuffer) {
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
		String value = attName;
		if (fSchemaObject instanceof ISchemaAttribute) {
			value = XMLInsertionComputer.generateAttributeValue((ISchemaAttribute) fSchemaObject, fProcessor.getModel(), attName);
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
			((IReconcilingParticipant) model).reconciled(document);

		if (model instanceof IPluginModelBase) {
			IPluginBase base = ((IPluginModelBase) model).getPluginBase();

			fPluginParent = null;
			fSchemaElement = null;

			if (fSchemaObject instanceof VirtualSchemaObject) {
				switch (((VirtualSchemaObject) fSchemaObject).getVType()) {
					case XMLContentAssistProcessor.F_EXTENSION_ATTRIBUTE_POINT_VALUE :
						if (!(fRange instanceof IDocumentAttributeNode))
							break;
						int offset = ((IDocumentAttributeNode) fRange).getEnclosingElement().getOffset();
						IPluginExtension[] extensions = base.getExtensions();
						for (int i = 0; i < extensions.length; i++) {
							if (((IDocumentElementNode) extensions[i]).getOffset() == offset) {
								if (extensions[i].getChildCount() != 0)
									break; // don't modify existing extensions
								fPluginParent = extensions[i];
								fSchemaElement = XMLUtil.getSchemaElement((IDocumentElementNode) extensions[i], extensions[i].getPoint());
								break;
							}
						}
						break;
					case XMLContentAssistProcessor.F_EXTENSION_POINT_AND_VALUE :
						findExtensionVirtualPointValue(base);
						break;
				}
			} else if (fRange instanceof IDocumentElementNode && base instanceof IDocumentElementNode) {
				Stack s = new Stack();
				IDocumentElementNode node = (IDocumentElementNode) fRange;
				IDocumentElementNode newSearch = (IDocumentElementNode) base;
				// traverse up old model, pushing all nodes onto the stack along the way
				while (node != null && !(node instanceof IPluginBase)) {
					s.push(node);
					node = node.getParentNode();
				}

				// traverse down new model to find new node, using stack as a guideline
				while (!s.isEmpty()) {
					node = (IDocumentElementNode) s.pop();
					int nodeIndex = 0;
					while ((node = node.getPreviousSibling()) != null)
						nodeIndex += 1;
					newSearch = newSearch.getChildAt(nodeIndex);
				}
				if (newSearch != null) {
					IDocumentElementNode[] children = newSearch.getChildNodes();
					for (int i = 0; i < children.length; i++) {
						if (children[i].getOffset() == fOffset && children[i] instanceof IPluginElement) {
							fPluginParent = (IPluginElement) children[i];
							fSchemaElement = (ISchemaElement) fSchemaObject;
							break;
						}
					}
				}
			}

			if (fPluginParent != null && fSchemaElement != null) {
				XMLInsertionComputer.computeInsertion(fSchemaElement, fPluginParent);
				fProcessor.flushDocument();
				if (model instanceof AbstractEditingModel) {
					try {
						((AbstractEditingModel) model).adjustOffsets(document);
					} catch (CoreException e) {
					}
					setSelectionOffsets(document, fSchemaElement, fPluginParent);
				}
			}
		}
	}

	/**
	 * Assumption: Model already reconciled by caller
	 * @param base
	 */
	private void findExtensionVirtualPointValue(IPluginBase base) {

		IDocumentRange range = null;
		PDESourcePage page = fProcessor.getSourcePage();
		// Ensure page is defined
		if (page == null) {
			return;
		}
		// When we inserted the extension element and extension point attribute
		// name and value, we selected the point value
		// Find the corresponding range in order to add child elements to
		// the proper extension.
		range = page.getRangeElement(fOffset, true);
		// Ensure the range is an attribute
		if ((range == null) || (range instanceof IDocumentElementNode) == false) {
			return;
		}
		// Get the offset of the extension element
		int targetOffset = ((IDocumentElementNode) range).getOffset();
		// Search this plug-ins extensions for the proper one
		IPluginExtension[] extensions = base.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			// Get the offset of the current extension
			int extensionOffset = ((IDocumentElementNode) extensions[i]).getOffset();
			// If the offsets match we foudn the extension element
			// Note: The extension element should have no children
			if ((extensionOffset == targetOffset) && (extensions[i].getChildCount() == 0)) {
				fPluginParent = extensions[i];
				// Get the corresponding schema element
				fSchemaElement = XMLUtil.getSchemaElement((IDocumentElementNode) extensions[i], extensions[i].getPoint());
				break;
			}
		}
	}

	private void setSelectionOffsets(IDocument document, ISchemaElement schemaElement, IPluginParent pluginParent) {
		if (pluginParent instanceof IPluginExtension) {
			String point = ((IPluginExtension) pluginParent).getPoint();
			IPluginObject[] children = ((IPluginExtension) pluginParent).getChildren();
			if (children != null && children.length > 0 && children[0] instanceof IPluginParent) {
				pluginParent = (IPluginParent) children[0];
				schemaElement = XMLUtil.getSchemaElement((IDocumentElementNode) pluginParent, point);
			}
		}

		if (pluginParent instanceof IPluginElement) {
			int offset = ((IDocumentElementNode) pluginParent).getOffset();
			int len = ((IDocumentElementNode) pluginParent).getLength();
			String value = null;
			try {
				value = document.get(offset, len);
			} catch (BadLocationException e) {
			}
			if (((IPluginElement) pluginParent).getAttributeCount() > 0) {
				// Select value of first required attribute
				IPluginAttribute att = ((IPluginElement) pluginParent).getAttributes()[0];
				if (att instanceof PluginAttribute) {
					fSelOffset = ((PluginAttribute) att).getValueOffset();
					fSelLen = ((PluginAttribute) att).getValueLength();
				}
			} else if (XMLInsertionComputer.hasOptionalChildren(schemaElement, false, new HashSet()) && value != null) {
				int ind = value.indexOf('>');
				if (ind > 0) {
					fSelOffset = offset + ind + 1;
					fSelLen = 0;
				}
			} else if (XMLInsertionComputer.hasOptionalAttributes(schemaElement) && value != null) {
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
		if (fAddInfo == null) {
			if (fSchemaObject == null)
				return null;
			StringBuffer sb = new StringBuffer();
			HTMLPrinter.insertPageProlog(sb, 0, TextUtil.getJavaDocStyleSheerURL());
			String desc = null;
			if (fSchemaObject == null)
				desc = PDEUIMessages.BaseWizardSelectionPage_noDesc;
			else {
				desc = fSchemaObject.getDescription();
				if (desc == null || desc.trim().length() == 0)
					desc = PDEUIMessages.BaseWizardSelectionPage_noDesc;
			}
			sb.append(desc);
			HTMLPrinter.addPageEpilog(sb);
			fAddInfo = sb.toString();
		}
		return fAddInfo;
	}

	public IContextInformation getContextInformation() {
		return null;
	}

	public String getDisplayString() {
		if (fSchemaObject instanceof VirtualSchemaObject) {
			switch (((VirtualSchemaObject) fSchemaObject).getVType()) {
				case XMLContentAssistProcessor.F_CLOSE_TAG :
					return "... />"; //$NON-NLS-1$
				case XMLContentAssistProcessor.F_EXTENSION_POINT_AND_VALUE :
				case XMLContentAssistProcessor.F_EXTENSION_ATTRIBUTE_POINT_VALUE :
				case XMLContentAssistProcessor.F_ATTRIBUTE_VALUE :
					return fSchemaObject.getName();
			}
		}
		if (fSchemaObject instanceof ISchemaAttribute)
			return fSchemaObject.getName();
		if (fSchemaObject != null)
			return fSchemaObject.getName();
		if (fRange instanceof IDocumentElementNode)
			return "...> </" + ((IDocumentElementNode) fRange).getXMLTagName() + ">"; //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	public Image getImage() {
		if (fSchemaObject instanceof VirtualSchemaObject)
			return fProcessor.getImage(((VirtualSchemaObject) fSchemaObject).getVType());
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

	public IInformationControlCreator getInformationControlCreator() {
		if (fCreator == null) {
			fCreator = new AbstractReusableInformationControlCreator() {
				public IInformationControl doCreateInformationControl(Shell parent) {
					if (BrowserInformationControl.isAvailable(parent))
						return new BrowserInformationControl(parent, JFaceResources.DIALOG_FONT, false);
					return new DefaultInformationControl(parent, false);
				}
			};
		}
		return fCreator;
	}

	public int getPrefixCompletionStart(IDocument document, int completionOffset) {
		return 0;
	}

	public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return true;
	}

}
