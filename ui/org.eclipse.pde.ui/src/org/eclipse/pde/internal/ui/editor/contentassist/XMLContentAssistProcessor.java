package org.eclipse.pde.internal.ui.editor.contentassist;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;
import org.eclipse.swt.graphics.Image;

public class XMLContentAssistProcessor extends TypePackageCompletionProcessor implements IContentAssistProcessor, ICompletionListener {

	// specific assist types
	protected static final int
		F_XX = -1, // infer by passing object
		F_EP = 0, // extension point
		F_EX = 1, // extension
		F_EL = 2, // element
		F_AT = 3, // attribute
		F_CL = 4, // close tag
		F_AT_VAL = 5, // attribute value
		F_AT_EP = 6, // extension attribute "point" value
		
		F_TOTAL_TYPES = 7;
	
	// proposal generation type
	private static final int 
		F_NO_ASSIST = 0,
		F_ADD_ATTRIB = 1,
		F_ADD_CHILD = 2,
		F_OPEN_TAG = 3;
	
	private static final ISchemaObject[] F_V_BOOLS = new ISchemaObject[] {
		new VSchemaObject("true", null, F_AT_VAL), //$NON-NLS-1$
		new VSchemaObject("false", null, F_AT_VAL) //$NON-NLS-1$
	};
	
	private static final String F_STR_EXT_PT = "extension-point"; //$NON-NLS-1$
	private static final String F_STR_EXT = "extension"; //$NON-NLS-1$
	
	protected static class VSchemaObject implements ISchemaObject {
		String vName, vDesc; int vType;
		public VSchemaObject(String name, String description, int type)
			{ vName = name; vDesc = description; vType = type; }
		public String getDescription() {return vDesc;}
		public String getName() {return vName;}
		public ISchemaObject getParent() {return null;}
		public ISchema getSchema() {return null;}
		public void setParent(ISchemaObject parent) {}
		public Object getAdapter(Class adapter) {return null;}
		public void write(String indent, PrintWriter writer) {}
		public int getVType() {return vType;}
	}
	
	private PDESourcePage fSourcePage;
	private final Image[] fImages = new Image[F_TOTAL_TYPES];
	// TODO add a listener to add/remove extension points as they are added/removed from working models
	private ISchemaObject[] fPoints; // available extension points
	private IDocumentRange fRange;
	private int fDocLen = -1;
	
	public XMLContentAssistProcessor(PDESourcePage sourcePage) {
		init();
		fSourcePage = sourcePage;
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IBaseModel model = getModel();
		IDocument doc = viewer.getDocument();
		int docLen = doc.getLength();
		if (docLen == fDocLen)
			return null; // left/right cursor has been pressed - cancel content assist
		
		fDocLen = docLen;
		if (model instanceof AbstractEditingModel
				&& fSourcePage.isDirty()
				&& ((AbstractEditingModel)model).isStale()
				&& fRange == null)
			((AbstractEditingModel)model).reconciled(doc);

		if (fRange == null) {
			assignRange(offset);
		} else {
			// TODO - we may be looking at the wrong fRange
			// when this happens --> reset it and reconcile
			// how can we tell if we are looking at the wrong one... ?
			boolean resetAndReconcile = false;
			if (!(fRange instanceof IDocumentAttribute))
				// too easy to reconcile.. this is temporary
				resetAndReconcile = true;
				
			if (resetAndReconcile) {
				fRange = null;
				if (model instanceof IReconcilingParticipant)
					((IReconcilingParticipant)model).reconciled(doc);
			}
		}
		
		if (fRange instanceof IDocumentAttribute) 
			return computeCompletionProposal((IDocumentAttribute)fRange, offset, doc);
		else if (fRange instanceof IDocumentNode)
			return computeCompletionProposal((IDocumentNode)fRange, offset, doc);
		else if (fRange instanceof IDocumentTextNode)
			return null;
		else if (model instanceof PluginModelBase)
			// broken model - infer from text content
			return computeBrokenModelProposal(((PluginModelBase)model).getLastErrorNode(), offset, doc);
		
		return null;
	}

	private void assignRange(int offset) {
		fRange = fSourcePage.getRangeElement(offset, true);
		if (fRange == null)
			return;
		// if we are rigth AT (cursor before) the range, we want to contribute
		// to its parent
		if (fRange instanceof IDocumentAttribute) {
			if (((IDocumentAttribute)fRange).getNameOffset() == offset)
				fRange = ((IDocumentAttribute)fRange).getEnclosingElement();
		} else if (fRange instanceof IDocumentNode) {
			if (((IDocumentNode)fRange).getOffset() == offset)
				fRange = ((IDocumentNode)fRange).getParentNode();
		} else if (fRange instanceof IDocumentTextNode) {
			if (((IDocumentTextNode)fRange).getOffset() == offset)
				fRange = ((IDocumentTextNode)fRange).getEnclosingElement();
		}
	}

	private ICompletionProposal[] computeCompletionProposal(IDocumentAttribute attr, int offset, IDocument doc) {
		if (offset < attr.getValueOffset())
			return null;
		int[] offests = new int[] {offset, offset, offset};
		String[] guess = guessContentRequest(offests, doc, false);
		if (guess == null)
			return null;
//		String element = guess[0];
//		String attribute = guess[1];
		String attrValue = guess[2];
		
		IPluginObject obj = XMLUtil.getTopLevelParent((IDocumentNode)attr);
		if (obj instanceof IPluginExtension) {
			if (attr.getAttributeName().equals(IPluginExtension.P_POINT) && 
					offset >= attr.getValueOffset())
				return computeAttributeProposal(attr, offset, attrValue, fPoints);
			ISchemaAttribute sAttr = XMLUtil.getSchemaAttribute(attr, ((IPluginExtension)obj).getPoint());
			if (sAttr == null)
				return null;
			
			if (sAttr.getKind() == IMetaAttribute.JAVA) {
				IResource resource = obj.getModel().getUnderlyingResource();
				if (resource == null)
					return null;
				// Revisit: NEW CODE HERE
				ArrayList list = new ArrayList();
				ICompletionProposal[] proposals = null;
				
				generateTypePackageProposals(attrValue, resource.getProject(), 
						list, offset - attrValue.length(), IJavaSearchConstants.CLASS_AND_INTERFACE);

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
				
			} else { // we have an IMetaAttribute.STRING kind
				if (sAttr.getType() == null)
					return null;
				ISchemaRestriction sRestr = (sAttr.getType()).getRestriction();
				ISchemaObject[] objs = null;
				if (sRestr == null) {
					ISchemaSimpleType type = sAttr.getType();
					if (type != null && type.getName().equals("boolean")) //$NON-NLS-1$
						objs = F_V_BOOLS;
				} else {
					Object[] restrictions = sRestr.getChildren();
					objs = new ISchemaObject[restrictions.length];
					for (int i = 0; i < restrictions.length; i++)
						if (restrictions[i] instanceof ISchemaObject)
							objs[i] = new VSchemaObject(((ISchemaObject)restrictions[i]).getName(), null, F_AT_VAL);
				}
				return computeAttributeProposal(attr, offset, attrValue, objs);
			}
		} else if (obj instanceof IPluginExtensionPoint) {
			if (attr.getAttributeValue().equals(IPluginExtensionPoint.P_SCHEMA)) {
				// provide proposals with all schama files in current plugin?
				
			}
		}
		return null;
	}
	
	private ICompletionProposal[] computeAttributeProposal(IDocumentAttribute attr, int offset, String currValue, ISchemaObject[] validValues) {
		if (validValues == null)
			return null;
		ArrayList list = new ArrayList();
		for (int i = 0; i < validValues.length; i++)
			addToList(list, currValue, validValues[i]);
		
		return convertListToProposal(list, (IDocumentRange)attr, offset);
	}

	private ICompletionProposal[] computeCompletionProposal(IDocumentNode node, int offset, IDocument doc) {
		int prop_type = determineAssistType(node, doc, offset);
		switch (prop_type) {
		case F_ADD_ATTRIB:
			return computeAddAttributeProposal(F_XX, node, offset, doc, null, node.getXMLTagName());
		case F_OPEN_TAG:
			return computeOpenTagProposal(node, offset, doc);
		case F_ADD_CHILD:
			return computeAddChildProposal(node, offset, doc, null);
		}
		return null;
	}
	
	private int determineAssistType(IDocumentNode node, IDocument doc, int offset) {
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
		return offset - 1 >= 0 && Character.isWhitespace(eleValue.charAt(offset - 1)) && 
					(Character.isWhitespace(c) || c == '/' || c == '>');
	}
	
	private ICompletionProposal[] computeAddChildProposal(IDocumentNode node, int offset, IDocument doc, String filter) {
		ArrayList propList = new ArrayList();
		if (node instanceof IPluginBase) {
			addToList(propList, filter, new VSchemaObject(F_STR_EXT, null, F_EX));
			addToList(propList, filter, new VSchemaObject(F_STR_EXT_PT, null, F_EP));
		} else if (node instanceof IPluginExtensionPoint) {
			return null;
		} else {
			IPluginObject obj = XMLUtil.getTopLevelParent(node);
			if (obj instanceof IPluginExtension) {
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
				if (sElem != null && sElem.getType() instanceof ISchemaComplexType) {
					IDocumentNode[] children = node.getChildNodes();
					ISchemaCompositor comp = ((ISchemaComplexType)sElem.getType()).getCompositor();
					if (comp == null)
						return null;
					ISchemaObject[] sChildren = comp.getChildren();
					
					// TODO need to check compositor max/min etc.
					ArrayList list = new ArrayList();
					for (int i = 0; i < sChildren.length && sChildren[i] instanceof ISchemaElement; i++) {
						int k; // if we break early we wont add
						for (k = 0; k < children.length; k++)
							if (children[k].getXMLTagName().equals(sChildren[i].getName()) &&
									((ISchemaElement)sChildren[i]).getMaxOccurs() == 1)
								break;
						if (k == children.length)
							list.add(sChildren[i]);
					}
					int length = list.size();
					for (int i = 0; i < length; i++)
						addToList(propList, filter, (ISchemaElement)list.get(i));
				}
			}
		}
		return convertListToProposal(propList, node, offset);
	}

	private ICompletionProposal[] computeOpenTagProposal(IDocumentNode node, int offset, IDocument doc) {
		IPluginObject obj = XMLUtil.getTopLevelParent(node);
		if (obj instanceof IPluginExtension) {
			ISchemaElement sElem = XMLUtil.getSchemaElement(node, ((IPluginExtension)obj).getPoint());
			if (sElem == null)
				return null;
			ISchemaCompositor comp = ((ISchemaComplexType)sElem.getType()).getCompositor();
			if (comp != null)
				return new ICompletionProposal[] { new XMLCompletionProposal(node, null, offset, this) };
		}
		return null;
	}

	private ICompletionProposal[] computeAddAttributeProposal(int type, IDocumentNode node, int offset, IDocument doc, String filter, String tag) {
		String nodeName = tag;
		if (nodeName == null && node != null)
			nodeName = node.getXMLTagName();
		if (type == F_EX || node instanceof IPluginExtension) {
			ISchemaElement sElem = XMLUtil.getSchemaElement(node, node != null ?
					((IPluginExtension)node).getPoint() : null);
			ISchemaObject[] sAttrs = sElem != null ?
					sElem.getAttributes() :
					new ISchemaObject[] {
						new VSchemaObject(IIdentifiable.P_ID, "The ID of this extension.", F_AT),
						new VSchemaObject(IPluginObject.P_NAME, "The name of this extension.", F_AT),
						new VSchemaObject(IPluginExtension.P_POINT, "The ID of the extension-point this extension will contribute to.", F_AT)
					};
			return computeAttributeProposals(sAttrs, node, offset, filter, nodeName);
		} else if (type == F_EP || node instanceof IPluginExtensionPoint) {
			ISchemaObject[] sAttrs = new ISchemaObject[] {
						new VSchemaObject(IIdentifiable.P_ID, "The ID of this extension-point.", F_AT),
						new VSchemaObject(IPluginObject.P_NAME, "The name of this extension-point.", F_AT),
						new VSchemaObject(IPluginExtensionPoint.P_SCHEMA, "The location of this extension-point's schema file.", F_AT)
					};
			return computeAttributeProposals(sAttrs, node, offset, filter, nodeName);
		} else {
			IPluginObject obj = XMLUtil.getTopLevelParent(node);
			if (obj instanceof IPluginExtension) {
				ISchemaElement sElem = XMLUtil.getSchemaElement(node, node != null ?
						((IPluginExtension)obj).getPoint() : null);
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
			if (name.startsWith(filter))
				list.add(object);
		}
	}
	
	private ICompletionProposal[] computeBrokenModelProposal(IDocumentNode parent, int offset, IDocument doc) {
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
			String point = ((IPluginExtension)obj).getPoint();
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
			if (element.equals(F_STR_EXT))
				return computeAddAttributeProposal(F_EX, null, atRepOffset, doc, attr, F_STR_EXT);
			if (element.equals(F_STR_EXT_PT))
				return computeAddAttributeProposal(F_EP, null, atRepOffset, doc, attr, F_STR_EXT_PT);
		}
		return null;
	}
	
	private ICompletionProposal[] computeBrokenModelAttributeContentProposal(IDocumentNode parent, int offset, String element, String attr, String filter) {
		// TODO use computeCompletionProposal(IDocumentAttribute attr, int offset) if possible
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
		} catch (BadLocationException e) {}
		if (node == null)
			return null;
		
		if (quoteCount % 2 == 0)
			attVal = null;
		else if (brokenModel)
			return null; // open quotes - don't provide assist
			
		return new String[] {node, attr, attVal};
	}
	
	protected IBaseModel getModel()
		{ return fSourcePage.getInputContext().getModel(); }
	protected ITextSelection getCurrentSelection() {
		ISelection sel = fSourcePage.getSelectionProvider().getSelection();
		if (sel instanceof ITextSelection)
			return (ITextSelection)sel;
		return null;
	}
	protected void flushDocument() {
		fSourcePage.getInputContext().flushEditorInput();
	}
	
	private ICompletionProposal[] computeAttributeProposals(ISchemaObject[] sAttrs, IDocumentNode node, int offset, String filter, String parentName) {
		if (sAttrs == null || sAttrs.length == 0)
			return null;
		IDocumentAttribute[] attrs = node != null ? node.getNodeAttributes() : new IDocumentAttribute[0];
		
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
			list.add(0, new VSchemaObject(parentName, null, F_CL));
		return convertListToProposal(list, node, offset);
	}

	private ICompletionProposal[] convertListToProposal(ArrayList list, IDocumentRange range, int offset) {
		ICompletionProposal[] proposals = new ICompletionProposal[list.size()];
		if (proposals.length == 0)
			return null;
		for (int i = 0; i < proposals.length; i++)
			proposals[i] = new XMLCompletionProposal(range, (ISchemaObject)list.get(i), offset, this);
		return proposals;
	}

	public void assistSessionEnded(ContentAssistEvent event) {
		fRange = null;
		fDocLen = -1;
	}

	public void assistSessionStarted(ContentAssistEvent event) {
	}

	public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
	}

	private void init() {
		if (fPoints == null) {
			ArrayList extPoints = new ArrayList();
			IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
			for (int i = 0; i < plugins.length; i++) {
				IPluginExtensionPoint[] points = plugins[i].getPluginBase().getExtensionPoints();
				for (int j = 0; j < points.length; j++)
					extPoints.add(points[j]);
			}
			
			fPoints = new ISchemaObject[extPoints.size()];
			for (int i = 0; i < fPoints.length; i++)
				fPoints[i] = new VSchemaObject(((IPluginExtensionPoint)extPoints.get(i)).getFullId(), null, F_AT_EP);
		}
	}
	
	public Image getImage(int type) {
		if (fImages[type] == null) {
			switch(type) {
			case F_EP:
			case F_AT_EP:
				return fImages[type] = PDEPluginImages.DESC_EXT_POINT_OBJ.createImage();
			case F_EX:
				return fImages[type] = PDEPluginImages.DESC_EXTENSION_OBJ.createImage();
			case F_EL:
			case F_CL:
				return fImages[type] = PDEPluginImages.DESC_XML_ELEMENT_OBJ.createImage();
			case F_AT:
			case F_AT_VAL:
				return fImages[type] = PDEPluginImages.DESC_ATT_URI_OBJ.createImage();
			}
		}
		return fImages[type];
	}
	
	public void disposeImages() {
		for (int i = 0; i < fImages.length; i++)
			if (fImages[i] != null && !fImages[i].isDisposed())
				fImages[i].dispose();
	}
	
}
