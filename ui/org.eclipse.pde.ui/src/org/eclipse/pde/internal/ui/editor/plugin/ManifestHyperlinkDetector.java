package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.JavaHyperlink;

public class ManifestHyperlinkDetector implements IHyperlinkDetector {

	private PDESourcePage fSourcePage;
	
	/**
	 * @param editor the editor in which to detect the hyperlink
	 */
	public ManifestHyperlinkDetector(PDESourcePage page) {
		fSourcePage = page;
	}
	
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || canShowMultipleHyperlinks)
			return null;

		IDocumentRange element = fSourcePage.getRangeElement(region.getOffset());
		if (element == null)
			return null;
		
		if (!(element instanceof IDocumentAttribute))
			return null;
		
		// only highlight if we are hovering inside of the attribute value
		IDocumentAttribute attr = (IDocumentAttribute)element;
		if (attr.getValueOffset() > region.getOffset() ||
				attr.getValueOffset() + attr.getValueLength() - 1 < region.getOffset())
			return null;
		
		// consult schema to make sure attribute is of kind IMetaAttribute.JAVA
		IDocumentNode node = attr.getEnclosingElement();
		while (node != null && !(node instanceof IPluginExtension))
			node = node.getParentNode();
		
		if (node == null)
			return null;
		
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(((IPluginExtension)node).getPoint());
		if (schema == null)
			return null;
		
		ISchemaElement sElement = schema.findElement(attr.getEnclosingElement().getXMLTagName());
		if (sElement == null)
			return null;
		
		ISchemaAttribute sAttr = sElement.getAttribute(attr.getAttributeName());
		if (sAttr == null)
			return null;
		
		if (((IPluginAttribute)attr).getValue().length() == 0)
			return null;
		
		if (sAttr.getKind() == IMetaAttribute.JAVA) {
			return new IHyperlink[] { new JavaHyperlink(
					new Region(attr.getValueOffset(), attr.getValueLength()),
					((IPluginExtension)node).getModel().getUnderlyingResource().getProject(),
					((IPluginAttribute)attr).getValue())};
		} else if (sAttr.getKind() == IMetaAttribute.RESOURCE) {
			// TODO select OR open resource in package explorer
		}
		
		return null;
	}

}
