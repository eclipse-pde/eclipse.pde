package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.HashSet;
import java.util.Stack;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.contentassist.XMLContentAssistProcessor.VSchemaObject;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class XMLCompletionProposal implements ICompletionProposal {
	
	private static final String F_DEF_ATTR_INDENT = "      "; //$NON-NLS-1$
	
	private ISchemaObject fSchemaObject;
	private IDocumentRange fRange;
	private int fOffset, fLen, fSelOffset = -1, fSelLen;
	private XMLContentAssistProcessor fProcessor;
	
	public XMLCompletionProposal(IDocumentRange node, ISchemaObject object, int offset, XMLContentAssistProcessor processor) {
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
			String value = generateAttributeValue((ISchemaAttribute)fSchemaObject, fProcessor.getModel());
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
				computeInsertion(schemaElement, pluginParent);
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
			} else if (hasOptionalChildren(schemaElement, false, new HashSet()) && value != null) {
				// position caret for element insertion
				int ind = value.indexOf('>');
				if (ind > 0) {
					fSelOffset = offset + ind + 1;
					fSelLen = 0;
				}
			} else if (hasOptionalAttributes(schemaElement) && value != null) {
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
	
	private boolean hasOptionalAttributes(ISchemaElement ele) {
		ISchemaAttribute[] attrs = ele.getAttributes();
		for (int i = 0; i < attrs.length; i++)
			if (attrs[i].getUse() == ISchemaAttribute.OPTIONAL || 
					attrs[i].getUse() == ISchemaAttribute.DEFAULT)
				return true;
		return false;
	}
	
	private boolean hasOptionalChildren(ISchemaObject obj, boolean onChild, HashSet set) {
		if (obj == null || set.contains(obj))
			return false;
		set.add(obj);
		if (obj instanceof ISchemaElement) {
			if (onChild 
					&& ((ISchemaElement)obj).getMinOccurs() == 0
					&& ((ISchemaElement)obj).getMaxOccurs() > 0)
				return true;
			ISchemaType type = ((ISchemaElement) obj).getType();
			if (type instanceof ISchemaComplexType)
				return hasOptionalChildren(((ISchemaComplexType)type).getCompositor(), true, set);
		} else if (obj instanceof ISchemaCompositor) {
			ISchemaObject[] children = ((ISchemaCompositor)obj).getChildren();
			if (children != null)
				for (int i = 0; i < children.length; i++)
					if (hasOptionalChildren(children[i], true, set))
						return true;
		}
		return false;
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
		// TODO how / where does this show up?
		// does something need to be enabled?
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
	
	/**
	 * @param sElement
	 * @param pElement
	 */
	protected void computeInsertion(ISchemaElement sElement,
			IPluginParent pElement) {
		HashSet visited = new HashSet();
		visited.add(sElement.getName());
		computeInsertionType(sElement, pElement, visited);
	}
	
	/**
	 * @param sElement
	 * @param pElement
	 * @param visited
	 */
	protected void computeInsertionType(ISchemaElement sElement,
			IPluginParent pElement, HashSet visited) {
		
		if ((sElement == null) ||
				(pElement == null)) {
			// If there is no corresponding schema information or plug-in 
			// model, then there is nothing to augment
			return;
		} else if (sElement.getType() instanceof ISchemaSimpleType) {
			// For simple types, insert a comment informing the user to
			// add element content text
			try {
				if (pElement instanceof IPluginElement)
					((IPluginElement)pElement).setText(NLS.bind(
							PDEUIMessages.XMLCompletionProposal_InfoElement,
							pElement.getName()));
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			return;
		} else if (sElement.getType() instanceof ISchemaComplexType) {
			// Note:  Mixed content types do not affect auto-generation
			// Note:  Deprecated elements do not affect auto-generation
			// Insert element attributes
			computeInsertionAllAttributes(pElement, sElement);
			// Get this element's compositor
			ISchemaCompositor compositor = ((ISchemaComplexType)sElement.getType()).getCompositor();
			// Process the compositor
			computeInsertionSequence(compositor, pElement, visited);
		} else {
			// Unknown element type
			return;
		}
	}

	/**
	 * @param pElement
	 * @param visited
	 * @param schemaObject
	 */
	protected void computeInsertionObject(IPluginParent pElement, HashSet visited, ISchemaObject schemaObject) {
		if (schemaObject instanceof ISchemaElement) {
			ISchemaElement schemaElement = (ISchemaElement) schemaObject;
			computeInsertionElement(pElement, visited, schemaElement);
		} else if (schemaObject instanceof ISchemaCompositor) {
			ISchemaCompositor sCompositor = (ISchemaCompositor) schemaObject;
			computeInsertionSequence(sCompositor, pElement, visited);
		} else {
			// Unknown schema object
		}
	}

	/**
	 * @param pElement
	 * @param compositor
	 */
	protected boolean computeInsertionCompositor(IPluginParent pElement, ISchemaCompositor compositor) {
		if (compositor == null) {
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			// Too presumption to choose for the user
			// Avoid processing and generate a comment to inform the user that
			// they need to update this element accordingly
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.ALL) {
			// Not supported by PDE - should never get here
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.GROUP) {
			// Not supported by PDE - should never get here
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.SEQUENCE) {
			return true;
		} else {
			// Unknown compositor
			return false;
		}
	}

	/**
	 * @param pElement
	 * @param visited
	 * @param schemaElement
	 */
	protected void computeInsertionElement(IPluginParent pElement, HashSet visited, ISchemaElement schemaElement) {
		try {
			for (int j = 0; j < schemaElement.getMinOccurs(); j++) {
				// Update Model
				IPluginElement childElement = null;
				childElement = 
					pElement.getModel().getFactory().createElement(pElement);
				childElement.setName(schemaElement.getName());
				pElement.add(childElement);
				// Track visited
				HashSet newSet = (HashSet) visited.clone();
				if (newSet.add(schemaElement.getName())) {
					computeInsertionType(schemaElement, childElement, newSet);
				} else {
					childElement.setText(PDEUIMessages.XMLCompletionProposal_ErrorCycle);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	/**
	 * @param pElement
	 * @param type
	 * @param attributes
	 */
	protected void computeInsertionAllAttributes(IPluginParent pElement, ISchemaElement sElement) {
		// Has to be a complex type if there are attributes
		ISchemaComplexType type = (ISchemaComplexType)sElement.getType();
		// Get the underlying project
		IResource resource = pElement.getModel().getUnderlyingResource();
		IProject project = null;
		if (resource != null)
			project = resource.getProject();
		// Get all the attributes
		ISchemaAttribute[] attributes = type.getAttributes();
		// Generate a unique number for IDs
		int counter = XMLUtil.getCounterValue(sElement);
		// Process all attributes
		for (int i = 0; i < type.getAttributeCount(); i++) {
			ISchemaAttribute attribute = attributes[i];
			// Note:  If an attribute is deprecated, it does not affect
			// auto-generation.
			try {
				if (attribute.getUse() == ISchemaAttribute.REQUIRED || 
						attribute.getUse() == ISchemaAttribute.DEFAULT) {
					String value = generateAttributeValue(project, counter, attribute);
					// Update Model
					setAttribute(pElement, attribute.getName(), value);
				}
				// Ignore optional attributes
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	/**
	 * @param project
	 * @param counter
	 * @param attribute
	 * @return
	 */
	protected String generateAttributeValue(IProject project, int counter, ISchemaAttribute attribute) {
		String value = ""; //$NON-NLS-1$
		ISchemaRestriction restriction = 
			attribute.getType().getRestriction();

		if (attribute.getKind() == IMetaAttribute.JAVA &&
			project != null) {
			value = XMLUtil.createDefaultClassName(project,
					attribute, counter);
		} else if ((attribute.getUse() == ISchemaAttribute.DEFAULT) &&
					(attribute.getValue() != null)) {
			value = attribute.getValue().toString();
		} else if (restriction != null) {
			// Check for enumeration restrictions, if there is one, 
			// just pick the first enumerated value
			value = restriction.getChildren()[0].toString();
		} else if (project != null) {
			// Cases:
			// IMetaAttribute.STRING
			// IMetaAttribute.RESOURCE
			value = XMLUtil.createDefaultName(project,
					attribute, counter);
		}
		return value;
	}
	
	protected String generateAttributeValue(ISchemaAttribute attribute, IBaseModel baseModel) {
		if (baseModel instanceof IModel) {
			IResource resource = ((IModel)baseModel).getUnderlyingResource();
			if (resource != null) {
				int counter = 1;
				if (attribute.getParent() instanceof ISchemaElement) {
					ISchemaElement sElement = (ISchemaElement)attribute.getParent();
					// Generate a unique number for IDs
					counter = XMLUtil.getCounterValue(sElement);
				}					
				return generateAttributeValue(resource.getProject(), counter, attribute);
			}
		}
		return " "; //$NON-NLS-1$
	}
	
	/**
	 * @param compositor
	 * @param pElement
	 * @param visited
	 */
	protected void computeInsertionSequence(ISchemaCompositor compositor,
			IPluginParent pElement, HashSet visited) {
		if (compositor == null)
			return;
		// Process the compositor the minimum number of times
		for (int k = 0; k < compositor.getMinOccurs(); k++) {
			// Only continue processing if the compositor is a sequence
			if (computeInsertionCompositor(pElement, compositor) == false)
				continue;
			// We have a sequence
			ISchemaObject[] schemaObject = compositor.getChildren();
			// Process the compositors children
			for (int i = 0; i < compositor.getChildCount(); i++) {
				computeInsertionObject(pElement, visited, schemaObject[i]);
			}
		}		
	}
	
	private void setAttribute(IPluginParent parent, String attName, String attValue) throws CoreException {
		if (parent instanceof IPluginElement) {
			((IPluginElement)parent).setAttribute(attName, attValue);
		} else if (parent instanceof IPluginExtension) {
			IPluginExtension pe = (IPluginExtension)parent;
			if (attName.equals(IIdentifiable.P_ID)) {
				String currValue = pe.getId();
				if (currValue == null || currValue.length() == 0)
					pe.setId(attValue);
			} else if (attName.equals(IPluginObject.P_NAME)) {
				String currValue = pe.getName();
				if (currValue == null || currValue.length() == 0)
					pe.setName(attName);
			} else if (attName.equals(IPluginExtension.P_POINT)) {
				String currValue = pe.getPoint();
				if (currValue == null || currValue.length() == 0)
					pe.setPoint(attValue);
			}
		}
	}
	
}
