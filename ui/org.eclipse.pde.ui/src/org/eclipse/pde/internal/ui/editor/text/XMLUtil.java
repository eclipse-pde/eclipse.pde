package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;

public abstract class XMLUtil {

	/**
	 * Scans up the node's parents till it reaches
	 * a IPluginExtension or IPluginExtensionPoint (or null)
	 * and returns the result.
	 * 
	 * @param node
	 * @return the IPluginExtension or IPluginExtensionPoint that contains <code>node</code>
	 */
	public static IPluginObject getTopLevelParent(IDocumentNode node) {
		if (node instanceof IDocumentAttribute)
			node = ((IDocumentAttribute)node).getEnclosingElement();
		
		while (node != null && 
				!(node instanceof IPluginExtension) && 
				!(node instanceof IPluginExtensionPoint))
			node = node.getParentNode();
		
		return node != null ? (IPluginObject)node : null;
	}
	
	public static boolean withinAttributeValue(IDocumentAttribute attr, int offset) {
		return attr.getValueOffset() <= offset && offset <= attr.getValueOffset() + attr.getValueLength();
	}
	
	/**
	 * Get the ISchemaElement corresponding to this IDocumentNode
	 * @param node
	 * @param extensionPoint the extension point of the schema, if <code>null</code> it will be deduced
	 * @return the ISchemaElement for <code>node</code>
	 */
	public static ISchemaElement getSchemaElement(IDocumentNode node, String extensionPoint) {
		if (extensionPoint == null) {
			IPluginObject obj = getTopLevelParent(node);
			if (!(obj instanceof IPluginExtension))
				return null;
			extensionPoint = ((IPluginExtension)obj).getPoint();
		}
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(extensionPoint);
		if (schema == null)
			return null;
		
		ISchemaElement sElement = schema.findElement(node.getXMLTagName());
		return sElement;
	}
	
	/**
	 * Get the ISchemaAttribute corresponding to this IDocumentAttribute
	 * @param attr
	 * @param extensionPoint the extension point of the schema, if <code>null</code> it will be deduced
	 * @return the ISchemaAttribute for <code>attr</code>
	 */
	public static ISchemaAttribute getSchemaAttribute(IDocumentAttribute attr, String extensionPoint) {
		ISchemaElement ele = getSchemaElement(attr.getEnclosingElement(), extensionPoint);
		if (ele == null)
			return null;
		
		return ele.getAttribute(attr.getAttributeName());
	}
	
}
