package org.eclipse.pde.internal.builders;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;


public abstract class PluginBaseErrorReporter extends ExtensionsErrorReporter {

	public PluginBaseErrorReporter(IFile file) {
		super(file);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.ExtensionsErrorReporter#validateContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void validateContent(IProgressMonitor monitor) {
		Element element = getDocumentRoot();
		String elementName = element.getNodeName();
		if (!getRootElementName().equals(elementName)) {
			reportIllegalElement(element, CompilerFlags.ERROR);
		} else {
			validateTopLevelAttributes(element);
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (monitor.isCanceled())
					break;
				Element child = (Element)children.item(i);
				String name = child.getNodeName();
				if (name.equals("extension")) {
					validateExtension(child);
				} else if (name.equals("extension-point")) {
					validateExtensionPoint(child);
				} else if (name.equals("runtime")){
					validateRuntime(child);
				} else if (name.equals("requires")) {
					validateRequires(child);
				} else {
					int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
					if (severity != CompilerFlags.IGNORE)
						reportIllegalElement(element, severity);
				}
			}
		}
	}
	
	protected void validateTopLevelAttributes(Element element) {
		assertAttributeDefined(element, "id", CompilerFlags.ERROR);
		if (assertAttributeDefined(element, "version", CompilerFlags.ERROR)) {
			validateVersionAttribute(element, element.getAttributeNode("version"));
		}
		if (assertAttributeDefined(element, "name", CompilerFlags.ERROR)) {
			validateTranslatableString(element, element.getAttributeNode("name"));
		}
		Attr attr = element.getAttributeNode("provider-name");
		if (attr != null)
			validateTranslatableString(element, attr);	
	}
	
	protected void validateVersionAttribute(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE);
		if (severity != CompilerFlags.IGNORE) {
			IStatus status = PluginVersionIdentifier.validateVersion(attr.getValue());
			if (status.getSeverity() != IStatus.OK)
				report(status.getMessage(), getLine(element, attr.getName()), CompilerFlags.ERROR);
		}
	}
	
	protected void validateMatch(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_ILLEGAL_ATT_VALUE);
		if (severity != CompilerFlags.IGNORE) {
			String value = attr.getValue();
			if (!"perfect".equals(value) && !"equivalent".equals(value)
				&& !"greaterOrEqual".equals(value) && !"compatible".equals(value))
				reportIllegalAttributeValue(element, attr, severity);
		}
	}
	
	protected abstract String getRootElementName();

	protected void validateRequires(Element element) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element)children.item(i);
			if (child.getNodeName().equals("import")) {
				validateImport(child);
			} else if (severity != CompilerFlags.IGNORE) {
					reportIllegalElement(child, severity);			
			}
		}
	}
	
	protected void validateImport(Element element) {
		if (assertAttributeDefined(element, "plugin", CompilerFlags.ERROR)) {
			validatePluginID(element, element.getAttributeNode("plugin"));
		}
		Attr attr = element.getAttributeNode("version");
		if (attr != null)
			validateVersionAttribute(element, attr);
		
		attr = element.getAttributeNode("match");
		if (attr != null)
			validateMatch(element, attr);
		
		attr = element.getAttributeNode("export");
		if (attr != null)
			validateBoolean(element, attr);
		
		attr = element.getAttributeNode("optional");
		if (attr != null)
			validateBoolean(element, attr);
	}
	
	protected void validateRuntime(Element element) {
		int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element)children.item(i);
			if (child.getNodeName().equals("library")) {
				validateLibrary(child);
			} else if (severity != CompilerFlags.IGNORE) {
				reportIllegalElement(child, severity);			
			}
		}
		
	}
	
	protected void validateLibrary(Element element) {
		assertAttributeDefined(element, "name", CompilerFlags.ERROR);
		
		int severity = CompilerFlags.getFlag(CompilerFlags.P_UNKNOWN_ELEMENT);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element)children.item(i);
			if (child.getNodeName().equals("export")) {
				assertAttributeDefined(child, "name", CompilerFlags.ERROR);
			} else if (severity != CompilerFlags.IGNORE) {
				reportIllegalElement(child, severity);			
			} 
		}
	}
	
	protected void validatePluginID(Element element, Attr attr) {
		int severity = CompilerFlags
				.getFlag(CompilerFlags.P_UNRESOLVED_IMPORTS);
		if (severity != CompilerFlags.IGNORE) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(attr.getValue());
			if (model == null || !model.isEnabled()) {
				report(PDE.getFormattedMessage("Builders.Manifest.dependency", attr.getValue()), 
						getLine(element, attr.getName()),
						severity);
			}
		}
	}

}
