package org.eclipse.pde.internal.builders;

import java.net.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;


public class ManifestErrorReporter extends XMLErrorReporter {

	/**
	 * @param file
	 */
	public ManifestErrorReporter(IFile file) {
		super(file);
	}
	
	protected void reportIllegalElement(Element element, int severity) {
		Node parent = element.getParentNode();
		if (parent == null || parent instanceof org.w3c.dom.Document) {
			report(PDE.getResourceString("Builders.Manifest.illegalRoot"), getLine(element), severity); //$NON-NLS-1$
		} else {
			report(PDE.getFormattedMessage(
					"Builders.Manifest.child", new String[] { //$NON-NLS-1$
					element.getNodeName(), parent.getNodeName() }),
					getLine(element), severity);
		}
	}
	
	protected void reportMissingRequiredAttribute(Element element, String attName, int severity) {
		String message = PDE
				.getFormattedMessage(
						"Builders.Manifest.missingRequired", new String[] { attName, element.getNodeName() }); //$NON-NLS-1$			
		report(message, getLine(element), severity);
	}

	protected boolean assertAttributeDefined(Element element, String attrName, int severity) {
		Attr attr = element.getAttributeNode(attrName);
		if (attr == null) {
			reportMissingRequiredAttribute(element, attrName, severity);
			return false;
		}
		return true;
	}

	protected void reportUnknownAttribute(Element element, String attName, int severity) {
		String message = PDE.getFormattedMessage("Builders.Manifest.attribute", //$NON-NLS-1$
				attName);
		report(message, getLine(element, attName), severity);
	}
	
	protected void reportIllegalAttributeValue(Element element, Attr attr) {
		String message = PDE.getFormattedMessage("Builders.Manifest.att-value", //$NON-NLS-1$
				new String[] { attr.getValue(), attr.getName() });
		report(message, getLine(element, attr.getName()), CompilerFlags.ERROR);
	}
	
	protected void validateVersionAttribute(Element element, Attr attr) {
		IStatus status = PluginVersionIdentifier.validateVersion(attr.getValue());
		if (status.getSeverity() != IStatus.OK)
			report(status.getMessage(), getLine(element, attr.getName()), CompilerFlags.ERROR);
	}
	
	protected void validateMatch(Element element, Attr attr) {
		String value = attr.getValue();
		if (!"perfect".equals(value) && !"equivalent".equals(value) //$NON-NLS-1$ //$NON-NLS-2$
			&& !"greaterOrEqual".equals(value) && !"compatible".equals(value)) //$NON-NLS-1$ //$NON-NLS-2$
			reportIllegalAttributeValue(element, attr);
	}

	protected void validateElementWithContent(Element element, boolean hasContent, Set allowedAttributes) {
		NodeList children = element.getChildNodes();
		boolean textFound = false;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) {
				textFound = ((Text)child).getNodeValue().trim().length() > 0;
			} else if (child instanceof Element) {
				reportIllegalElement((Element)child, CompilerFlags.ERROR);
			}
		}
		if (!textFound)
			reportMissingElementContent(element);
	}

	private void reportMissingElementContent(Element element) {
		report(PDE.getFormattedMessage("Builders.Feature.empty", element //$NON-NLS-1$
				.getNodeName()), getLine(element), CompilerFlags.ERROR);
	}
	
	protected void reportExtraneousElements(NodeList elements, int maximum) {
		if (elements.getLength() > maximum) {
			for (int i = maximum; i < elements.getLength(); i++) {
				Element element = (Element) elements.item(i);
				report(PDE.getFormattedMessage("Builders.Feature.multiplicity", //$NON-NLS-1$
						element.getNodeName()), getLine(element),
						CompilerFlags.ERROR);
			}
		}
	}

	protected void validateURL(Element element, String attName) {
		String value = element.getAttribute(attName);
		try {
			if (!value.startsWith("http:") && !value.startsWith("file:")) //$NON-NLS-1$ //$NON-NLS-2$
				value = "file:" + value; //$NON-NLS-1$
			new URL(value);
		} catch (MalformedURLException e) {
			report(PDE.getFormattedMessage("Builders.Feature.badURL", attName), getLine(element, attName), CompilerFlags.ERROR); //$NON-NLS-1$
		}
	}
	
	protected void validatePluginID(Element element, Attr attr) {
		int severity = CompilerFlags
				.getFlag(project, CompilerFlags.P_UNRESOLVED_IMPORTS);
		if ("true".equals(element.getAttribute("optional")) && severity == CompilerFlags.ERROR)  //$NON-NLS-1$ //$NON-NLS-2$
			severity = CompilerFlags.WARNING;
		if (severity != CompilerFlags.IGNORE) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(attr.getValue());
			if (model == null || !model.isEnabled()) {
				report(PDE.getFormattedMessage("Builders.Manifest.dependency", attr.getValue()),  //$NON-NLS-1$
						getLine(element, attr.getName()),
						severity);
			}
		}
	}

	protected void validateBoolean(Element element, Attr attr) {
		String value = attr.getValue();
		if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) //$NON-NLS-1$ //$NON-NLS-2$
			reportIllegalAttributeValue(element, attr);
	}	

}
