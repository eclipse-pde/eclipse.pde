/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Remy Chi Jian Suen <remy.suen@gmail.com> - bug 201566
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 227105
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.*;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.contentassist.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.text.*;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.core.util.PDESchemaHelper;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.FormEditor;

public class XMLContentAssistProcessor extends TypePackageCompletionProcessor implements IContentAssistProcessor, ICompletionListener {

	protected boolean fAssistSessionStarted;

	// Specific assist types

	protected static final int F_INFER_BY_OBJECT = -1;

	protected static final int F_EXTENSION_POINT = 0;

	protected static final int F_EXTENSION = 1;

	protected static final int F_ELEMENT = 2;

	protected static final int F_ATTRIBUTE = 3;

	protected static final int F_CLOSE_TAG = 4;

	protected static final int F_ATTRIBUTE_VALUE = 5;

	protected static final int F_EXTENSION_ATTRIBUTE_POINT_VALUE = 6;

	protected static final int F_EXTENSION_POINT_AND_VALUE = 7;

	protected static final int F_ATTRIBUTE_ID_VALUE = 8;

	protected static final int F_ATTRIBUTE_BOOLEAN_VALUE = 9;

	protected static final int F_TOTAL_TYPES = 10;

	// proposal generation type
	private static final int F_NO_ASSIST = 0, F_ADD_ATTRIB = 1, F_ADD_CHILD = 2, F_OPEN_TAG = 3;

	private static final ArrayList F_V_BOOLS = new ArrayList();
	static {
		F_V_BOOLS.add(new VirtualSchemaObject("true", null, F_ATTRIBUTE_BOOLEAN_VALUE)); //$NON-NLS-1$
		F_V_BOOLS.add(new VirtualSchemaObject("false", null, F_ATTRIBUTE_BOOLEAN_VALUE)); //$NON-NLS-1$
	}

	private static final String F_STR_EXT_PT = "extension-point"; //$NON-NLS-1$
	private static final String F_STR_EXT = "extension"; //$NON-NLS-1$

	private PDESourcePage fSourcePage;
	private final Image[] fImages = new Image[F_TOTAL_TYPES];
	// TODO add a listener to add/remove extension points as they are added/removed from working models
	private IDocumentRange fRange;
	private int fDocLen = -1;

	/** All external plug-in extension points */
	private ArrayList fExternalExtPoints;

	/** All internal plug-in extension points */
	private ArrayList fInternalExtPoints;

	/** All external and internal plug-in extension points */
	private ArrayList fAllExtPoints;

	public XMLContentAssistProcessor(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument doc = viewer.getDocument();
		int docLen = doc.getLength();
		if (docLen == fDocLen)
			return null; // left/right cursor has been pressed - cancel content assist

		fDocLen = docLen;
		IBaseModel model = getModel();
		if (model instanceof AbstractEditingModel && fSourcePage.isDirty() && ((AbstractEditingModel) model).isStale() && fRange == null) {
			((AbstractEditingModel) model).reconciled(doc);
		} else if (fAssistSessionStarted) {
			// Always reconcile when content assist is first invoked
			// Fix Bug # 149478
			((AbstractEditingModel) model).reconciled(doc);
			fAssistSessionStarted = false;
		}

		if (fRange == null) {
			assignRange(offset);
		} else {
			// TODO - we may be looking at the wrong fRange
			// when this happens --> reset it and reconcile
			// how can we tell if we are looking at the wrong one... ?
			boolean resetAndReconcile = false;
			if (!(fRange instanceof IDocumentAttributeNode))
				// too easy to reconcile.. this is temporary
				resetAndReconcile = true;

			if (resetAndReconcile) {
				fRange = null;
				if (model instanceof IReconcilingParticipant)
					((IReconcilingParticipant) model).reconciled(doc);
			}
		}
		// Get content assist text if any
		XMLContentAssistText caText = XMLContentAssistText.parse(offset, doc);

		if (caText != null) {
			return computeCATextProposal(doc, offset, caText);
		} else if (fRange instanceof IDocumentAttributeNode) {
			return computeCompletionProposal((IDocumentAttributeNode) fRange, offset, doc);
		} else if (fRange instanceof IDocumentElementNode) {
			return computeCompletionProposal((IDocumentElementNode) fRange, offset, doc);
		} else if (fRange instanceof IDocumentTextNode) {
			return null;
		} else if (model instanceof PluginModelBase) {
			// broken model - infer from text content
			return computeBrokenModelProposal(((PluginModelBase) model).getLastErrorNode(), offset, doc);
		}
		return null;
	}

	protected ICompletionProposal[] debugPrintProposals(ICompletionProposal[] proposals, String id, boolean print) {
		if (proposals == null) {
			System.out.println("[0] " + id); //$NON-NLS-1$
			return proposals;
		}
		System.out.println("[" + proposals.length + "] " + id); //$NON-NLS-1$ //$NON-NLS-2$
		if (print == false) {
			return proposals;
		}
		for (int i = 0; i < proposals.length; i++) {
			System.out.println(proposals[i].getDisplayString());
		}
		return proposals;
	}

	private void assignRange(int offset) {
		fRange = fSourcePage.getRangeElement(offset, true);
		if (fRange == null)
			return;
		// if we are rigth AT (cursor before) the range, we want to contribute
		// to its parent
		if (fRange instanceof IDocumentAttributeNode) {
			if (((IDocumentAttributeNode) fRange).getNameOffset() == offset)
				fRange = ((IDocumentAttributeNode) fRange).getEnclosingElement();
		} else if (fRange instanceof IDocumentElementNode) {
			if (((IDocumentElementNode) fRange).getOffset() == offset)
				fRange = ((IDocumentElementNode) fRange).getParentNode();
		} else if (fRange instanceof IDocumentTextNode) {
			if (((IDocumentTextNode) fRange).getOffset() == offset)
				fRange = ((IDocumentTextNode) fRange).getEnclosingElement();
		}
	}

	private ICompletionProposal[] computeCATextProposal(IDocument doc, int offset, XMLContentAssistText caText) {
		fRange = fSourcePage.getRangeElement(offset, true);
		if ((fRange != null) && (fRange instanceof IDocumentTextNode)) {
			// We have a text node.
			// Get its parent element
			fRange = ((IDocumentTextNode) fRange).getEnclosingElement();
		}
		if ((fRange != null) && (fRange instanceof IDocumentElementNode)) {
			return computeAddChildProposal((IDocumentElementNode) fRange, caText.getStartOffset(), doc, caText.getText());
		}
		return null;
	}

	private ICompletionProposal[] computeCompletionProposal(IDocumentAttributeNode attr, int offset, IDocument doc) {
		if (offset < attr.getValueOffset())
			return null;
		int[] offests = new int[] {offset, offset, offset};
		String[] guess = guessContentRequest(offests, doc, false);
		if (guess == null)
			return null;
//		String element = guess[0];
//		String attribute = guess[1];
		String attrValue = guess[2];

		IPluginObject obj = XMLUtil.getTopLevelParent(attr);
		if (obj instanceof IPluginExtension) {
			if (attr.getAttributeName().equals(IPluginExtension.P_POINT) && offset >= attr.getValueOffset()) {
				return computeExtPointAttrProposals(attr, offset, attrValue);
			}
			ISchemaAttribute sAttr = XMLUtil.getSchemaAttribute(attr, ((IPluginExtension) obj).getPoint());
			if (sAttr == null)
				return null;

			if (sAttr.getKind() == IMetaAttribute.JAVA) {
				IResource resource = obj.getModel().getUnderlyingResource();
				if (resource == null)
					return null;
				// Revisit: NEW CODE HERE
				ArrayList list = new ArrayList();
				ICompletionProposal[] proposals = null;

				generateTypePackageProposals(attrValue, resource.getProject(), list, offset - attrValue.length(), IJavaSearchConstants.CLASS_AND_INTERFACE);

				if ((list != null) && (list.size() != 0)) {
					// Convert the results array list into an array of completion
					// proposals
					proposals = (ICompletionProposal[]) list.toArray(new ICompletionProposal[list.size()]);
					sortCompletions(proposals);
					return proposals;
				}
				return null;
			} else if (sAttr.getKind() == IMetaAttribute.RESOURCE) {
				// provide proposals with all resources in current plugin?

			} else if (sAttr.getKind() == IMetaAttribute.IDENTIFIER) {
				String[] validAttributes = (String[]) PDESchemaHelper.getValidAttributes(sAttr).keySet().toArray(new String[0]);
				Arrays.sort(validAttributes);
				ArrayList objs = new ArrayList(validAttributes.length);
				for (int i = 0; i < validAttributes.length; i++)
					objs.add(new VirtualSchemaObject(validAttributes[i], null, F_ATTRIBUTE_ID_VALUE));
				return computeAttributeProposal(attr, offset, attrValue, objs);
			} else { // we have an IMetaAttribute.STRING kind
				if (sAttr.getType() == null)
					return null;
				ISchemaRestriction sRestr = (sAttr.getType()).getRestriction();
				ArrayList objs = new ArrayList();
				if (sRestr == null) {
					ISchemaSimpleType type = sAttr.getType();
					if (type != null && type.getName().equals("boolean")) //$NON-NLS-1$
						objs = F_V_BOOLS;
				} else {
					Object[] restrictions = sRestr.getChildren();
					for (int i = 0; i < restrictions.length; i++)
						if (restrictions[i] instanceof ISchemaObject)
							objs.add(new VirtualSchemaObject(((ISchemaObject) restrictions[i]).getName(), null, F_ATTRIBUTE_VALUE));
				}
				return computeAttributeProposal(attr, offset, attrValue, objs);
			}
		} else if (obj instanceof IPluginExtensionPoint) {
			if (attr.getAttributeValue().equals(IPluginExtensionPoint.P_SCHEMA)) {
				// provide proposals with all schema files in current plugin?

			}
		}
		return null;
	}

	private ICompletionProposal[] computeExtPointAttrProposals(IDocumentAttributeNode attribute, int offset, String currentAttributeValue) {
		// Get all the applicable extension points
		ArrayList allExtensionPoints = getAllExtensionPoints(F_EXTENSION_ATTRIBUTE_POINT_VALUE);
		// Ensure we found extension points
		if ((allExtensionPoints == null) || (allExtensionPoints.size() == 0)) {
			return null;
		}
		// If there is no current attribute value, then the applicable extension
		// points do not need to be filtered.  Return the list as is
		if ((currentAttributeValue == null) || (currentAttributeValue.length() == 0)) {
			return convertListToProposal(allExtensionPoints, attribute, offset);
		}
		// Create the filtered proposal list
		ArrayList filteredProposalList = new ArrayList();
		// Filter the applicable extension points by the current attribute
		// value
		filterExtPointAttrProposals(filteredProposalList, allExtensionPoints, currentAttributeValue);
		// Convert into a digestable list of proposals
		return convertListToProposal(filteredProposalList, attribute, offset);
	}

	private ICompletionProposal[] computeRootNodeProposals(IDocumentElementNode node, int offset, String filter) {
		// Create the filtered proposal list
		ArrayList filteredProposalList = new ArrayList();
		// Add extension to the list
		addToList(filteredProposalList, filter, new VirtualSchemaObject(F_STR_EXT, PDEUIMessages.XMLContentAssistProcessor_extensions, F_EXTENSION));
		// Add extension point to the list
		addToList(filteredProposalList, filter, new VirtualSchemaObject(F_STR_EXT_PT, PDEUIMessages.XMLContentAssistProcessor_extensionPoints, F_EXTENSION_POINT));
		// Get all avaliable extensions with point attribute values
		ArrayList allExtensionPoints = getAllExtensionPoints(F_EXTENSION_POINT_AND_VALUE);
		// Ensure we found extension points
		if ((allExtensionPoints == null) || (allExtensionPoints.size() == 0)) {
			// Return the current proposal list without extension points
			return convertListToProposal(filteredProposalList, node, offset);
		}
		// If there is no current value, then the applicable extension
		// points do not need to be filtered.  Merge the list as is with the
		// existing proposals
		if ((filter == null) || (filter.length() == 0)) {
			filteredProposalList.addAll(allExtensionPoints);
			return convertListToProposal(filteredProposalList, node, offset);
		}
		// Filter the applicable extension points by the current value
		filterExtPointAttrProposals(filteredProposalList, allExtensionPoints, filter);
		// Convert into a digestable list of proposals
		return convertListToProposal(filteredProposalList, node, offset);
	}

	/**
	 * @param filteredProposalList - Must not be NULL
	 * @param allExtensionPoints - Must not be NULL
	 * @param filter - Must not be NULL
	 */
	private void filterExtPointAttrProposals(ArrayList filteredProposalList, ArrayList allExtensionPoints, String filter) {
		// Create a regex pattern out of the current attribute value
		// Ignore case
		String patternString = "(?i)" + filter; //$NON-NLS-1$
		// Compile the pattern
		Pattern pattern = Pattern.compile(patternString);
		// Iterate over the applicable extension points and add extension points
		// matching the pattern to the filtered proposal list
		Iterator iterator = allExtensionPoints.iterator();
		while (iterator.hasNext()) {
			// Get the schema object
			ISchemaObject schemaObject = (ISchemaObject) iterator.next();
			// Ensure the schema object is defined
			if (schemaObject == null) {
				continue;
			}
			// Get the name of the schema object
			String name = schemaObject.getName();
			// If the current attribute value matches some part of the name
			// add it to the list
			if (pattern.matcher(name).find()) {
				filteredProposalList.add(schemaObject);
			}
		}
	}

	private ICompletionProposal[] computeAttributeProposal(IDocumentAttributeNode attr, int offset, String currValue, List validValues) {
		if (validValues == null)
			return null;
		ArrayList list = new ArrayList();
		for (int i = 0; i < validValues.size(); i++)
			addToList(list, currValue, (ISchemaObject) validValues.get(i));

		return convertListToProposal(list, attr, offset);
	}

	private ICompletionProposal[] computeCompletionProposal(IDocumentElementNode node, int offset, IDocument doc) {
		int prop_type = determineAssistType(node, doc, offset);
		switch (prop_type) {
			case F_ADD_ATTRIB :
				return computeAddAttributeProposal(F_INFER_BY_OBJECT, node, offset, doc, null, node.getXMLTagName());
			case F_OPEN_TAG :
				return computeOpenTagProposal(node, offset, doc);
			case F_ADD_CHILD :
				return computeAddChildProposal(node, offset, doc, null);
		}
		return null;
	}

	private int determineAssistType(IDocumentElementNode node, IDocument doc, int offset) {
		int len = node.getLength();
		int off = node.getOffset();
		if (len == -1 || off == -1)
			return F_NO_ASSIST;

		offset = offset - off; // look locally
		if (offset > node.getXMLTagName().length() + 1) {
			try {
				String eleValue = doc.get(off, len);
				int ind = eleValue.indexOf('>');
				if (ind > 0 && eleValue.charAt(ind - 1) == '/')
					ind -= 1;
				if (offset <= ind) {
					if (canInsertAttrib(eleValue, offset))
						return F_ADD_ATTRIB;
					return F_NO_ASSIST;
				}
				ind = eleValue.lastIndexOf('<');
				if (ind == 0 && offset == len - 1)
					return F_OPEN_TAG; // childless node - check if it can be cracked open
				if (ind + 1 < len && eleValue.charAt(ind + 1) == '/' && offset <= ind)
					return F_ADD_CHILD;
			} catch (BadLocationException e) {
			}
		}
		return F_NO_ASSIST;
	}

	private boolean canInsertAttrib(String eleValue, int offset) {
		// character before offset must be whitespace
		// character on offset must be whitespace, '/' or '>'
		char c = eleValue.charAt(offset);
		return offset - 1 >= 0 && Character.isWhitespace(eleValue.charAt(offset - 1)) && (Character.isWhitespace(c) || c == '/' || c == '>');
	}

	private ICompletionProposal[] computeAddChildProposal(IDocumentElementNode node, int offset, IDocument doc, String filter) {
		ArrayList propList = new ArrayList();
		if (node instanceof IPluginBase) {
			return computeRootNodeProposals(node, offset, filter);
		} else if (node instanceof IPluginExtensionPoint) {
			return null;
		} else {
			IPluginObject obj = XMLUtil.getTopLevelParent(node);
			if (obj instanceof IPluginExtension) {
				ISchemaElement sElement = XMLUtil.getSchemaElement(node, ((IPluginExtension) obj).getPoint());
				if ((sElement != null) && (sElement.getType() instanceof ISchemaComplexType)) {
					// We have a schema complex type.  Either the element has attributes
					// or the element has children.					
					// Generate the list of element proposals
					TreeSet elementSet = XMLElementProposalComputer.computeElementProposal(sElement, node);
					// Filter the list of element proposals
					Iterator iterator = elementSet.iterator();
					while (iterator.hasNext()) {
						addToList(propList, filter, (ISchemaObject) iterator.next());
					}
				} else {
					return null;
				}
			}
		}
		return convertListToProposal(propList, node, offset);
	}

	private ICompletionProposal[] computeOpenTagProposal(IDocumentElementNode node, int offset, IDocument doc) {
		IPluginObject obj = XMLUtil.getTopLevelParent(node);
		if (obj instanceof IPluginExtension) {
			ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension) obj).getPoint());
			if (sElem == null)
				return null;
			ISchemaCompositor comp = ((ISchemaComplexType) sElem.getType()).getCompositor();
			if (comp != null)
				return new ICompletionProposal[] {new XMLCompletionProposal(node, null, offset, this)};
		}
		return null;
	}

	private ICompletionProposal[] computeAddAttributeProposal(int type, IDocumentElementNode node, int offset, IDocument doc, String filter, String tag) {
		String nodeName = tag;
		if (nodeName == null && node != null)
			nodeName = node.getXMLTagName();
		if (type == F_EXTENSION || node instanceof IPluginExtension) {
			ISchemaElement sElem = XMLUtil.getSchemaElement(node, node != null ? ((IPluginExtension) node).getPoint() : null);
			ISchemaObject[] sAttrs = sElem != null ? sElem.getAttributes() : new ISchemaObject[] {new VirtualSchemaObject(IIdentifiable.P_ID, PDEUIMessages.XMLContentAssistProcessor_extId, F_ATTRIBUTE), new VirtualSchemaObject(IPluginObject.P_NAME, PDEUIMessages.XMLContentAssistProcessor_extName, F_ATTRIBUTE), new VirtualSchemaObject(IPluginExtension.P_POINT, PDEUIMessages.XMLContentAssistProcessor_extPoint, F_ATTRIBUTE)};
			return computeAttributeProposals(sAttrs, node, offset, filter, nodeName);
		} else if (type == F_EXTENSION_POINT || node instanceof IPluginExtensionPoint) {
			ISchemaObject[] sAttrs = new ISchemaObject[] {new VirtualSchemaObject(IIdentifiable.P_ID, PDEUIMessages.XMLContentAssistProcessor_extPointId, F_ATTRIBUTE), new VirtualSchemaObject(IPluginObject.P_NAME, PDEUIMessages.XMLContentAssistProcessor_extPointName, F_ATTRIBUTE), new VirtualSchemaObject(IPluginExtensionPoint.P_SCHEMA, PDEUIMessages.XMLContentAssistProcessor_schemaLocation, F_ATTRIBUTE)};
			return computeAttributeProposals(sAttrs, node, offset, filter, nodeName);
		} else {
			IPluginObject obj = XMLUtil.getTopLevelParent(node);
			if (obj instanceof IPluginExtension) {
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, node != null ? ((IPluginExtension) obj).getPoint() : null);
				ISchemaObject[] sAttrs = sElem != null ? sElem.getAttributes() : null;
				return computeAttributeProposals(sAttrs, node, offset, filter, nodeName);
			}
		}
		return null;
	}

	private void addToList(ArrayList list, String filter, ISchemaObject object) {
		if (object == null)
			return;
		if (filter == null || filter.length() == 0)
			list.add(object);
		else {
			String name = object.getName();
			if (filter.regionMatches(true, 0, name, 0, filter.length()))
				list.add(object);
		}
	}

	private ICompletionProposal[] computeBrokenModelProposal(IDocumentElementNode parent, int offset, IDocument doc) {
		if (parent == null)
			return null;

		int[] offArr = new int[] {offset, offset, offset};
		String[] guess = guessContentRequest(offArr, doc, true);
		if (guess == null)
			return null;

		int elRepOffset = offArr[0];
		int atRepOffset = offArr[1];
		int atValRepOffest = offArr[2];
		String element = guess[0];
		String attr = guess[1];
		String attVal = guess[2];

		IPluginObject obj = XMLUtil.getTopLevelParent(parent);
		if (obj instanceof IPluginExtension) {
			String point = ((IPluginExtension) obj).getPoint();
			if (attr == null)
				// search for element proposals
				return computeAddChildProposal(parent, elRepOffset, doc, element);

			ISchemaElement sEle = XMLUtil.getSchemaElement(parent, point);
			if (sEle == null)
				return null;
			sEle = sEle.getSchema().findElement(element);
			if (sEle == null)
				return null;

			if (attr.indexOf('=') != -1)
				// search for attribute content proposals
				return computeBrokenModelAttributeContentProposal(parent, atValRepOffest, element, attr, attVal);

			// search for attribute proposals
			return computeAttributeProposals(sEle.getAttributes(), null, atRepOffset, attr, element);
		} else if (parent instanceof IPluginBase) {
			if (attr == null)
				return computeAddChildProposal(parent, elRepOffset, doc, element);
			if (element.equalsIgnoreCase(F_STR_EXT))
				return computeAddAttributeProposal(F_EXTENSION, null, atRepOffset, doc, attr, F_STR_EXT);
			if (element.equalsIgnoreCase(F_STR_EXT_PT))
				return computeAddAttributeProposal(F_EXTENSION_POINT, null, atRepOffset, doc, attr, F_STR_EXT_PT);
		}
		return null;
	}

	private ICompletionProposal[] computeBrokenModelAttributeContentProposal(IDocumentElementNode parent, int offset, String element, String attr, String filter) {
		// TODO use computeCompletionProposal(IDocumentAttributeNode attr, int offset) if possible
		// or refactor above to be used here
		// CURRENTLY: attribute completion only works in non-broken models
		return null;
	}

	private String[] guessContentRequest(int[] offset, IDocument doc, boolean brokenModel) {
		StringBuffer nodeBuffer = new StringBuffer();
		StringBuffer attrBuffer = new StringBuffer();
		StringBuffer attrValBuffer = new StringBuffer();
		String node = null;
		String attr = null;
		String attVal = null;
		int quoteCount = 0;
		try {
			while (--offset[0] >= 0) {
				char c = doc.getChar(offset[0]);
				if (c == '"') {
					quoteCount += 1;
					nodeBuffer.setLength(0);
					attrBuffer.setLength(0);
					if (attVal != null) // ran into 2nd quotation mark, we are out of range
						continue;
					offset[2] = offset[0];
					attVal = attrValBuffer.toString();
				} else if (Character.isWhitespace(c)) {
					nodeBuffer.setLength(0);
					if (attr == null) {
						offset[1] = offset[0];
						int attBuffLen = attrBuffer.length();
						if (attBuffLen > 0 && attrBuffer.charAt(attBuffLen - 1) == '=')
							attrBuffer.setLength(attBuffLen - 1);
						attr = attrBuffer.toString();
					}
				} else if (c == '<') {
					node = nodeBuffer.toString();
					break;
				} else if (c == '>') {
					// only enable content assist if user is inside an open tag
					return null;
				} else {
					attrValBuffer.insert(0, c);
					attrBuffer.insert(0, c);
					nodeBuffer.insert(0, c);
				}
			}
		} catch (BadLocationException e) {
		}
		if (node == null)
			return null;

		if (quoteCount % 2 == 0)
			attVal = null;
		else if (brokenModel)
			return null; // open quotes - don't provide assist

		return new String[] {node, attr, attVal};
	}

	protected IBaseModel getModel() {
		return fSourcePage.getInputContext().getModel();
	}

	protected ITextSelection getCurrentSelection() {
		ISelection sel = fSourcePage.getSelectionProvider().getSelection();
		if (sel instanceof ITextSelection)
			return (ITextSelection) sel;
		return null;
	}

	protected void flushDocument() {
		fSourcePage.getInputContext().flushEditorInput();
	}

	private ICompletionProposal[] computeAttributeProposals(ISchemaObject[] sAttrs, IDocumentElementNode node, int offset, String filter, String parentName) {
		if (sAttrs == null || sAttrs.length == 0)
			return null;
		IDocumentAttributeNode[] attrs = node != null ? node.getNodeAttributes() : new IDocumentAttributeNode[0];

		ArrayList list = new ArrayList();
		for (int i = 0; i < sAttrs.length; i++) {
			int k; // if we break early we wont add
			for (k = 0; k < attrs.length; k++)
				if (attrs[k].getAttributeName().equals(sAttrs[i].getName()))
					break;
			if (k == attrs.length)
				addToList(list, filter, sAttrs[i]);
		}
		if (filter != null && filter.length() == 0)
			list.add(0, new VirtualSchemaObject(parentName, null, F_CLOSE_TAG));
		return convertListToProposal(list, node, offset);
	}

	private ICompletionProposal[] convertListToProposal(ArrayList list, IDocumentRange range, int offset) {
		ICompletionProposal[] proposals = new ICompletionProposal[list.size()];
		if (proposals.length == 0)
			return null;
		for (int i = 0; i < proposals.length; i++)
			proposals[i] = new XMLCompletionProposal(range, (ISchemaObject) list.get(i), offset, this);
		return proposals;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
		fRange = null;

		// Reset cached internal and external extension point proposals
		fAllExtPoints = null;
		// Reset cached internal point proposals
		// Note: Not resetting cached external point proposals
		// Assumption is that the users workspace can change; but, not the
		// platform including all the external plugins
		fInternalExtPoints = null;

		fDocLen = -1;
	}

	public void assistSessionStarted(ContentAssistEvent event) {
		fAssistSessionStarted = true;
	}

	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
	}

	private ArrayList getAllExtensionPoints(int vSchemaType) {
		// Return the previous extension points if defined
		if (fAllExtPoints != null) {
			return fAllExtPoints;
		}
		// Get the plugin model base 
		IPluginModelBase model = getPluginModelBase();
		// Note: All plug-in extension points are cached except the 
		// extension points defined by the plugin.xml we are currently 
		// editing. This means if a plug-in in the workspace defines a new
		// extension point and the plugin.xml editor is still open, the 
		// new extension point will not show up as a proposal because it is
		// using a cached list of extension points.
		// External extensions points are all extension points not defined by
		// the plugin currently being edited - opposed to non-workpspace
		// plugins.  Internal extension points are the extension points defined
		// by the plugin currently being edited.  May want to modify this
		// behaviour in the future.
		// Add all external extension point proposals to the list
		fAllExtPoints = (ArrayList) getExternalExtensionPoints(model, vSchemaType).clone();
		// Add all internal extension point proposals to the list
		fAllExtPoints.addAll(getInternalExtensionPoints(model, vSchemaType));

		return fAllExtPoints;
	}

	private ArrayList getExternalExtensionPoints(IPluginModelBase model, int vSchemaType) {
		// Return the previous external extension points if defined
		if (fExternalExtPoints != null) {
			updateExternalExtPointTypes(vSchemaType);
			return fExternalExtPoints;
		}
		// Query for all external extension points
		fExternalExtPoints = new ArrayList();
		// Get all plug-ins in the workspace
		IPluginModelBase[] plugins = PluginRegistry.getActiveModels();
		// Process each plugin
		for (int i = 0; i < plugins.length; i++) {
			// Make sure this plugin is not the one we are currently 
			// editing which defines internal extension points.
			// We don't want to cache internal extension points because the
			// workspace can change.
			if (plugins[i].getPluginBase().getId().equals(model.getPluginBase().getId())) {
				// Skip this plugin
				continue;
			}
			// Get all extension points defined by this plugin
			IPluginExtensionPoint[] points = plugins[i].getPluginBase().getExtensionPoints();
			// Process each extension point
			for (int j = 0; j < points.length; j++) {
				VirtualSchemaObject vObject = new VirtualSchemaObject(IdUtil.getFullId(points[j], model), points[j], vSchemaType);
				// Add the proposal to the list
				fExternalExtPoints.add(vObject);
			}
		}
		return fExternalExtPoints;
	}

	/**
	 * Handles edge case.
	 * External extension points are cached.
	 * As a result, these types persist over each other depending on what 
	 * context content assist is invoked first:
	 * F_EXTENSION_ATTRIBUTE_POINT_VALUE
	 * F_EXTENSION_POINT_AND_VALUE
	 * @param newVType
	 */
	private void updateExternalExtPointTypes(int newVType) {
		// Ensure we have proposals
		if (fExternalExtPoints.size() == 0) {
			return;
		}
		// Get the first proposal
		VirtualSchemaObject vObject = (VirtualSchemaObject) fExternalExtPoints.get(0);
		// If the first proposals type is the same as the new type, then we
		// do not have to do the update.  That is because all the proposals
		// have the same type. 
		if (vObject.getVType() == newVType) {
			return;
		}
		// Update all the proposals with the new type
		Iterator iterator = fExternalExtPoints.iterator();
		while (iterator.hasNext()) {
			((VirtualSchemaObject) iterator.next()).setVType(newVType);
		}
	}

	private ArrayList getInternalExtensionPoints(IPluginModelBase model, int vSchemaType) {
		// Return the previous internal extension points if defined
		if (fInternalExtPoints != null) {
			// Realistically, this line should never be hit
			return fInternalExtPoints;
		}
		fInternalExtPoints = new ArrayList();
		// Get all extension points defined by this plugin
		IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
		// Process each extension point
		for (int j = 0; j < points.length; j++) {
			VirtualSchemaObject vObject = new VirtualSchemaObject(IdUtil.getFullId(points[j], model), points[j], vSchemaType);
			// Add the proposal to the list
			fInternalExtPoints.add(vObject);
		}
		return fInternalExtPoints;
	}

	/**
	 * Returns a BundlePluginModel which has a getId() method that works.
	 * getModel() method returns a PluginModel whose getId() method does not
	 * work.
	 */
	private IPluginModelBase getPluginModelBase() {
		FormEditor formEditor = fSourcePage.getEditor();
		if ((formEditor instanceof PDEFormEditor) == false) {
			return null;
		}
		IBaseModel bModel = ((PDEFormEditor) formEditor).getAggregateModel();
		if ((bModel instanceof IPluginModelBase) == false) {
			return null;
		}
		return (IPluginModelBase) bModel;
	}

	public Image getImage(int type) {
		if (fImages[type] == null) {
			switch (type) {
				case F_EXTENSION_POINT :
				case F_EXTENSION_ATTRIBUTE_POINT_VALUE :
					return fImages[type] = PDEPluginImages.DESC_EXT_POINT_OBJ.createImage();
				case F_EXTENSION_POINT_AND_VALUE :
				case F_EXTENSION :
					return fImages[type] = PDEPluginImages.DESC_EXTENSION_OBJ.createImage();
				case F_ELEMENT :
				case F_CLOSE_TAG :
					return fImages[type] = PDEPluginImages.DESC_XML_ELEMENT_OBJ.createImage();
				case F_ATTRIBUTE :
				case F_ATTRIBUTE_VALUE :
					return fImages[type] = PDEPluginImages.DESC_ATT_URI_OBJ.createImage();
				case F_ATTRIBUTE_ID_VALUE :
					return fImages[type] = PDEPluginImages.DESC_ATT_ID_OBJ.createImage();
				case F_ATTRIBUTE_BOOLEAN_VALUE :
					return fImages[type] = PDEPluginImages.DESC_ATT_BOOLEAN_OBJ.createImage();
			}
		}
		return fImages[type];
	}

	public void dispose() {
		for (int i = 0; i < fImages.length; i++)
			if (fImages[i] != null && !fImages[i].isDisposed())
				fImages[i].dispose();
	}

	public PDESourcePage getSourcePage() {
		return fSourcePage;
	}

}
