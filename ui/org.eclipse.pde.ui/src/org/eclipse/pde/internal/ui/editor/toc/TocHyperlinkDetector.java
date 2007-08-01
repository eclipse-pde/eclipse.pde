package org.eclipse.pde.internal.ui.editor.toc;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.core.text.toc.TocObject;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;

public class TocHyperlinkDetector implements IHyperlinkDetector {

	private PDESourcePage fSourcePage;
	
	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public TocHyperlinkDetector(PDESourcePage page) {
		fSourcePage = page;
	}
	
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || canShowMultipleHyperlinks)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset(), true);
		if (!XMLUtil.withinRange(element, region.getOffset()))
			return null;
		
		if (element instanceof IDocumentAttribute)
			return detectAttributeHyperlink((IDocumentAttribute)element);
		return null;
	}

	private IHyperlink[] detectAttributeHyperlink(IDocumentAttribute attr) {
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;
		
		IDocumentNode node = attr.getEnclosingElement();
		if (node == null 
				|| !(node instanceof TocObject) 
				|| !((TocObject)node).getModel().isEditable())
		{	return null;
		}

		TocObject tocObject = (TocObject)node;
		TocModel model = tocObject.getModel();
		IResource res = model.getUnderlyingResource();
		IRegion linkRegion = new Region(attr.getValueOffset(), attr.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		if (tocObject.getType() == ITocConstants.TYPE_TOC) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_TOPIC))
			{	link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		} else if (tocObject.getType() == ITocConstants.TYPE_TOPIC) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_HREF))
			{	link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		} else if (tocObject.getType() == ITocConstants.TYPE_LINK) {
			if (attr.getAttributeName().equals(ITocConstants.ATTRIBUTE_TOC))
			{	link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			}
		}

		if (link[0] != null)
		{	return link;
		}

		return null;
	}

}
