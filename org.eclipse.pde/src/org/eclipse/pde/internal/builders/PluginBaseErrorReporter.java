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
		if (element == null)
			return;
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
				if (name.equals("extension")) { //$NON-NLS-1$
					validateExtension(child);
				} else if (name.equals("extension-point")) { //$NON-NLS-1$
					validateExtensionPoint(child);
				} else if (name.equals("runtime")){ //$NON-NLS-1$
					validateRuntime(child);
				} else if (name.equals("requires")) { //$NON-NLS-1$
					validateRequires(child);
				} else {
					int severity = CompilerFlags.getFlag(project, CompilerFlags.P_UNKNOWN_ELEMENT);
					if (severity != CompilerFlags.IGNORE)
						reportIllegalElement(element, severity);
				}
			}
		}
	}
	
	protected void validateTopLevelAttributes(Element element) {
		assertAttributeDefined(element, "id", CompilerFlags.ERROR); //$NON-NLS-1$
		if (assertAttributeDefined(element, "version", CompilerFlags.ERROR)) { //$NON-NLS-1$
			validateVersionAttribute(element, element.getAttributeNode("version")); //$NON-NLS-1$
		}
		if (assertAttributeDefined(element, "name", CompilerFlags.ERROR)) { //$NON-NLS-1$
			validateTranslatableString(element, element.getAttributeNode("name"), true); //$NON-NLS-1$
		}
		Attr attr = element.getAttributeNode("provider-name"); //$NON-NLS-1$
		if (attr != null)
			validateTranslatableString(element, attr, true);	
	}
		
	protected abstract String getRootElementName();

	protected void validateRequires(Element element) {
		int severity = CompilerFlags.getFlag(project, CompilerFlags.P_UNKNOWN_ELEMENT);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element)children.item(i);
			if (child.getNodeName().equals("import")) { //$NON-NLS-1$
				validateImport(child);
			} else if (severity != CompilerFlags.IGNORE) {
					reportIllegalElement(child, severity);			
			}
		}
	}
	
	protected void validateImport(Element element) {
		if (assertAttributeDefined(element, "plugin", CompilerFlags.ERROR)) { //$NON-NLS-1$
			validatePluginID(element, element.getAttributeNode("plugin")); //$NON-NLS-1$
		}
		Attr attr = element.getAttributeNode("version"); //$NON-NLS-1$
		if (attr != null)
			validateVersionAttribute(element, attr);
		
		attr = element.getAttributeNode("match"); //$NON-NLS-1$
		if (attr != null)
			validateMatch(element, attr);
		
		attr = element.getAttributeNode("export"); //$NON-NLS-1$
		if (attr != null)
			validateBoolean(element, attr);
		
		attr = element.getAttributeNode("optional"); //$NON-NLS-1$
		if (attr != null)
			validateBoolean(element, attr);
	}
	
	protected void validateRuntime(Element element) {
		int severity = CompilerFlags.getFlag(project, CompilerFlags.P_UNKNOWN_ELEMENT);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element)children.item(i);
			if (child.getNodeName().equals("library")) { //$NON-NLS-1$
				validateLibrary(child);
			} else if (severity != CompilerFlags.IGNORE) {
				reportIllegalElement(child, severity);			
			}
		}
		
	}
	
	protected void validateLibrary(Element element) {
		assertAttributeDefined(element, "name", CompilerFlags.ERROR); //$NON-NLS-1$
		
		int unknownSev = CompilerFlags.getFlag(project, CompilerFlags.P_UNKNOWN_ELEMENT);
		int deprecatedSev = CompilerFlags.getFlag(project, CompilerFlags.P_DEPRECATED);
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Element child = (Element)children.item(i);
			String elementName = child.getNodeName();
			if ("export".equals(elementName)) { //$NON-NLS-1$
				assertAttributeDefined(child, "name", CompilerFlags.ERROR); //$NON-NLS-1$
			} else if ("packages".equals(elementName)) { //$NON-NLS-1$
				if (deprecatedSev != CompilerFlags.IGNORE) {
					reportDeprecatedElement(child, deprecatedSev);
				}
			} else if (unknownSev != CompilerFlags.IGNORE) {
				reportIllegalElement(child, unknownSev);			
			}
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
	
	private void reportDeprecatedElement(Element element, int severity) {
		report(PDE.getFormattedMessage("Builders.Manifest.deprecated-3.0", //$NON-NLS-1$
				element.getNodeName()), getLine(element), severity);
	}

}
