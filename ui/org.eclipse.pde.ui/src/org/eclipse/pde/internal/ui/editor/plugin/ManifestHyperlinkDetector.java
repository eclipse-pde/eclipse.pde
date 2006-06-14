package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.schema.SchemaRootElement;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.JavaHyperlink;
import org.eclipse.pde.internal.ui.editor.text.ResourceHyperlink;
import org.eclipse.pde.internal.ui.editor.text.SchemaHyperlink;
import org.eclipse.pde.internal.ui.editor.text.TranslationHyperlink;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;

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
		if (!(element instanceof IDocumentAttribute))
			return null;
		
		IDocumentAttribute attr = (IDocumentAttribute)element;
		String attrValue = attr.getAttributeValue();
		if (attrValue.length() == 0)
			return null;
		
		// only highlight if we are hovering inside of the attribute value
		if (!XMLUtil.withinAttributeValue(attr, region.getOffset()))
			return null;
		
		IPluginObject node = XMLUtil.getTopLevelParent((IDocumentNode)attr);
		if (node == null || !node.getModel().isEditable())
			return null;
		
		IPluginModelBase base = node.getPluginModel();
		IResource res = base.getUnderlyingResource();
		IRegion linkRegion = new Region(attr.getValueOffset(), attr.getValueLength());

		IHyperlink[] link = new IHyperlink[1];
		if (node instanceof IPluginExtensionPoint) {
			if (attr.getAttributeName().equals(IPluginExtensionPoint.P_SCHEMA))
				link[0] = new SchemaHyperlink(linkRegion, attrValue, res);
			else if (attr.getAttributeName().equals(IPluginObject.P_NAME))
				if (attrValue.charAt(0) == '%')
					link[0] = new TranslationHyperlink(linkRegion, attrValue, base);
			
		} else if (node instanceof IPluginExtension) {
			ISchemaAttribute sAttr = XMLUtil.getSchemaAttribute(attr, ((IPluginExtension)node).getPoint());
			if (sAttr == null)
				return null;
			
			if (sAttr.getKind() == IMetaAttribute.JAVA) {
				link[0] = new JavaHyperlink(linkRegion, attrValue, res);
			} else if (sAttr.getKind() == IMetaAttribute.RESOURCE) {
				link[0] = new ResourceHyperlink(linkRegion, attrValue, res);
			} else if (sAttr.getParent() instanceof SchemaRootElement) {
				link[0] = new ExtensionHyperLink(linkRegion, attrValue);
			} else if (sAttr.isTranslatable()) {
				if (attrValue.charAt(0) == '%')
					link[0] = new TranslationHyperlink(linkRegion, attrValue, base);
			}
		}
		
		if (link[0] != null)
			return link;
		return null;
	}

}
