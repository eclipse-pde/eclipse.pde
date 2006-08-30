package org.eclipse.pde.internal.ui.editor.text;

import java.net.URL;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaAnnotationHandler;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.util.SchemaUtil;
import org.eclipse.pde.internal.core.util.XMLComponentRegistry;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;

public class PluginXMLTextHover extends PDETextHover {
	
	private PDESourcePage fSourcePage;
	
	public PluginXMLTextHover(PDESourcePage sourcePage) {
		fSourcePage = sourcePage;
	}

	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		int offset = hoverRegion.getOffset();
		IDocumentRange range = fSourcePage.getRangeElement(offset, true);
		if (!(range instanceof IPluginObject))
			return null;
		
		ISchema schema = getSchema((IPluginObject)range);
		if (schema == null)
			return null;
		
		ISchemaObject sObj = getSchemaObject(schema, (IPluginObject)range);
		if (range instanceof IPluginAttribute && sObj instanceof ISchemaElement) {
			IDocumentAttribute da = (IDocumentAttribute)range;
			if (da.getNameOffset() <= offset && 
					offset <= da.getNameOffset() + da.getNameLength())
				// inside name
				return getAttributeText((IPluginAttribute)range, (ISchemaElement)sObj);
			// inside value
			return getAttributeValueText((IPluginAttribute)range, (ISchemaElement)sObj);
		} else if (range instanceof IPluginElement) {
			IDocumentNode dn = (IDocumentNode)range;
			int dnOff = dn.getOffset();
			int dnLen = dn.getLength();
			String dnName = dn.getXMLTagName();
			if (dnOff + 1 <= offset && offset <= dnOff + dnName.length())
				// inside opening tag
				return getElementText((ISchemaElement)sObj);
			try {
				String nt = textViewer.getDocument().get(dnOff, dnLen);
				if (nt.endsWith("</" + dnName + ">")) { //$NON-NLS-1$ //$NON-NLS-2$
					offset = offset - dnOff;
					if (nt.length() - dnName.length() - 1 <= offset &&
							offset <= nt.length() - 2)
						// inside closing tag
						return getElementText((ISchemaElement)sObj);
				}
			} catch (BadLocationException e) {
			}
		}
		return null;
	}

	private ISchema getSchema(IPluginObject object) {
		IPluginObject extension = object;
		if (object instanceof IDocumentAttribute)
			extension = (IPluginObject)((IDocumentAttribute)object).getEnclosingElement();
		while (extension != null && !(extension instanceof IPluginExtension))
			extension = extension.getParent();
		
		if (extension == null)
			// started off outside of an extension element
			return null;
		
		String point = ((IPluginExtension)extension).getPoint();
		return PDECore.getDefault().getSchemaRegistry().getSchema(point);
	}
	
	private ISchemaObject getSchemaObject(ISchema schema, IPluginObject object) {
		if (object instanceof IPluginElement)
			return schema.findElement(((IPluginElement)object).getName());
		if (object instanceof IPluginExtension)
			return schema.findElement("extension"); //$NON-NLS-1$
		if (object instanceof IDocumentAttribute)
			return schema.findElement(((IDocumentAttribute)object).getEnclosingElement().getXMLTagName());
		return null;
	}
	
	private String getAttributeText(IPluginAttribute attrib, ISchemaElement sEle) {
		ISchemaAttribute sAtt = sEle.getAttribute(attrib.getName());
		if (sAtt == null)
			return null;
		return sAtt.getDescription();
	}
	
	private String getAttributeValueText(IPluginAttribute attrib, ISchemaElement sEle) {
		if (sEle.getName().equals("extension")) //$NON-NLS-1$
			return getSchemaDescription(attrib, sEle);
		ISchemaAttribute sAtt = sEle.getAttribute(attrib.getName());
		if (sAtt == null)
			return null;
		
		String value = attrib.getValue();
		if (sAtt.isTranslatable() && value.startsWith("%")) //$NON-NLS-1$
			return attrib.getModel().getResourceString(value);
		return null;
	}
	
	private String getSchemaDescription(IPluginAttribute attr, ISchemaElement sEle) {
		String description = XMLComponentRegistry.Instance().getDescription(
				attr.getValue(), XMLComponentRegistry.F_SCHEMA_COMPONENT);
		
		if (description == null) {
			URL url = sEle.getSchema().getURL();
			SchemaAnnotationHandler handler = new SchemaAnnotationHandler();
			SchemaUtil.parseURL(url, handler);
			description = handler.getDescription();
			XMLComponentRegistry.Instance().putDescription(
					attr.getValue(), description, XMLComponentRegistry.F_SCHEMA_COMPONENT);
		}
		return description;
	}
	
	private String getElementText(ISchemaElement sEle) {
		return sEle.getDescription();
	}

}
