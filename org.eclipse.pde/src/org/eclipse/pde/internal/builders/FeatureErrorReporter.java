package org.eclipse.pde.internal.builders;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;


public class FeatureErrorReporter extends ManifestErrorReporter {
	
	static HashSet attrs = new HashSet();

	static String[] attrNames = { "id", "version", "label", "provider-name", "image", "os", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"ws", "arch", "nl", "colocation-affinity", "primary", "exclusive", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"plugin", "application" }; //$NON-NLS-1$ //$NON-NLS-2$

	private IProgressMonitor fMonitor;
	
	public FeatureErrorReporter(IFile file) {
		super(file);
		if (attrs.isEmpty())
			attrs.addAll(Arrays.asList(attrNames));	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.builders.XMLErrorReporter#validateContent(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void validateContent(IProgressMonitor monitor) {
		fMonitor = monitor;
		Element element = getDocumentRoot();
		if (element == null)
			return;
		String elementName = element.getNodeName();
		if (!"feature".equals(elementName)) { //$NON-NLS-1$
			reportIllegalElement(element, CompilerFlags.ERROR);
		} else {
			validateFeatureAttributes(element);
			validateInstallHandler(element);
			validateDescription(element);
			validateLicense(element);
			validateCopyright(element);
			validateURLElement(element);
			validateIncludes(element);
			validateRequires(element);
			validatePlugins(element);
			validateData(element);
		}
	}

	private void validateData(Element parent) {
		NodeList list = parent.getElementsByTagName("data"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element data = (Element)list.item(i);
			assertAttributeDefined(data, "id", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = data.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr)attributes.item(j);
				String name = attr.getName();
				if (!name.equals("id") && !name.equals("os") && !name.equals("ws") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					&& !name.equals("nl") && !name.equals("arch")  //$NON-NLS-1$ //$NON-NLS-2$
					&& !name.equals("download-size") && !name.equals("install-size")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(data, name, CompilerFlags.ERROR);
				}
			}
		}
	}

	/**
	 * @param element
	 */
	private void validatePlugins(Element parent) {
		NodeList list = parent.getElementsByTagName("plugin"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element plugin = (Element)list.item(i);
			assertAttributeDefined(plugin, "id", CompilerFlags.ERROR); //$NON-NLS-1$
			assertAttributeDefined(plugin, "version", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = plugin.getAttributes();
			boolean isFragment = plugin.getAttribute("fragment").equals("false"); //$NON-NLS-1$ //$NON-NLS-2$
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr)attributes.item(j);
				String name = attr.getName();
				if (name.equals("id")) { //$NON-NLS-1$
					validatePluginID(plugin, attr, isFragment);
				} else if (name.equals("version")) { //$NON-NLS-1$
					validateVersionAttribute(plugin, attr);
				} else if (name.equals("fragment") || name.equals("unpack")) { //$NON-NLS-1$ //$NON-NLS-2$
					validateBoolean(plugin, attr);
				} else if (!name.equals("os") && !name.equals("ws") && !name.equals("nl") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						&& !name.equals("arch") && !name.equals("download-size") //$NON-NLS-1$ //$NON-NLS-2$
						&& !name.equals("install-size")){ //$NON-NLS-1$
					reportUnknownAttribute(plugin, name, CompilerFlags.ERROR);
				}
			}
		}
	}

	private void validateRequires(Element parent) {
		NodeList list = parent.getElementsByTagName("requires"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			validateImports((Element)list.item(0));
			reportExtraneousElements(list, 1);
		}
	}
	
	private void validateImports(Element parent) {
		NodeList list = parent.getElementsByTagName("import"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element)list.item(i);
			Attr plugin = element.getAttributeNode("plugin"); //$NON-NLS-1$
			Attr feature = element.getAttributeNode("feature"); //$NON-NLS-1$
			if (plugin == null && feature == null) {
				assertAttributeDefined(element, "plugin", CompilerFlags.ERROR); //$NON-NLS-1$
			} else if (plugin != null) {
				validatePluginID(element, plugin, false);
			} else if (feature != null) {
				validateFeatureID(element, feature);
			}
			NamedNodeMap attributes = element.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr)attributes.item(j);
				String name = attr.getName();
				if (name.equals("version")) { //$NON-NLS-1$
					validateVersionAttribute(element, attr);
				} else if (name.equals("match")) { //$NON-NLS-1$
					validateMatch(element, attr);
				} else if (name.equals("patch")) { //$NON-NLS-1$
					validateBoolean(element, attr);
				} else if (!name.equals("plugin") && !name.equals("feature")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			
		}
		
	}

	private void validateIncludes(Element parent) {
		NodeList list = parent.getElementsByTagName("includes"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element include = (Element)list.item(i);
			assertAttributeDefined(include, "id", CompilerFlags.ERROR); //$NON-NLS-1$
			assertAttributeDefined(include, "version", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = include.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr)attributes.item(j);
				String name = attr.getName();
				if (name.equals("version")) { //$NON-NLS-1$
					validateVersionAttribute(include, attr);
				} else if (name.equals("optional")) { //$NON-NLS-1$
					validateBoolean(include, attr);
				} else if (name.equals("search-location")) { //$NON-NLS-1$
					String value = include.getAttribute("search-location"); //$NON-NLS-1$
					if (!value.equals("root") && !value.equals("self") && !value.equals("both")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						reportIllegalAttributeValue(include, attr);
					}
				} else if (!name.equals("name") && !name.equals("os") && !name.equals("ws") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						&& !name.equals("nl") && !name.equals("arch")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(include, name, CompilerFlags.ERROR);
				}
			}
		}
	}

	private void validateURLElement(Element parent) {
		NodeList list = parent.getElementsByTagName("url"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			Element url = (Element)list.item(0);
			validateUpdateURL(url);
			validateDiscoveryURL(url);
			reportExtraneousElements(list, 1);
		}
	}
	
	private void validateUpdateURL(Element parent) {
		NodeList list = parent.getElementsByTagName("update"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element update = (Element)list.item(0);
			assertAttributeDefined(update, "url", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = update.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				String name = attributes.item(i).getNodeName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(update, "url"); //$NON-NLS-1$
				} else if (!name.equals("label")) { //$NON-NLS-1$
					reportUnknownAttribute(update, name, CompilerFlags.ERROR);
				}
			}
			reportExtraneousElements(list, 1);
		}		
	}
	
	private void validateDiscoveryURL(Element parent) {
		NodeList list = parent.getElementsByTagName("discovery"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element discovery = (Element)list.item(0);
			assertAttributeDefined(discovery, "url", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = discovery.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				String name = attributes.item(i).getNodeName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(discovery, "url"); //$NON-NLS-1$
				} else if (name.equals("type")) { //$NON-NLS-1$
					String value = discovery.getAttribute("type"); //$NON-NLS-1$
					if (!value.equals("web") && !value.equals("update")) { //$NON-NLS-1$ //$NON-NLS-2$
						reportIllegalAttributeValue(discovery, (Attr)attributes.item(i));
					}
				} if (!name.equals("label")) { //$NON-NLS-1$
					reportUnknownAttribute(discovery, name, CompilerFlags.ERROR);
				}
			}
		}		
	}
	
	private void validateCopyright(Element parent) {
		NodeList list = parent.getElementsByTagName("copyright"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element)list.item(0);
			validateElementWithContent((Element)list.item(0), true);
			NamedNodeMap attributes = element.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Attr attr = (Attr)attributes.item(i);
				String name = attr.getName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(element, name);
				} else {
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			reportExtraneousElements(list, 1);
		}
	}

	private void validateLicense(Element parent) {
		NodeList list = parent.getElementsByTagName("license"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element)list.item(0);
			validateElementWithContent((Element)list.item(0), true);
			NamedNodeMap attributes = element.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Attr attr = (Attr)attributes.item(i);
				String name = attr.getName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(element, name);
				} else {
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			reportExtraneousElements(list, 1);
		}
	}
	
	private void validateDescription(Element parent) {
		NodeList list = parent.getElementsByTagName("description"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element)list.item(0);
			validateElementWithContent((Element)list.item(0), true);
			NamedNodeMap attributes = element.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				Attr attr = (Attr)attributes.item(i);
				String name = attr.getName();
				if (name.equals("url")) { //$NON-NLS-1$
					validateURL(element, name);
				} else {
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			reportExtraneousElements(list, 1);
		}
	}


	private void validateInstallHandler(Element element) {
		NodeList elements = element.getElementsByTagName("install-handler"); //$NON-NLS-1$
		if (elements.getLength() > 0) {
			if (fMonitor.isCanceled())
				return;
			Element handler = (Element)elements.item(0);
			NamedNodeMap attributes = handler.getAttributes();
			for (int i = 0; i < attributes.getLength(); i++) {
				String name = attributes.item(i).getNodeName();
				if (!name.equals("library") && !name.equals("handler")) //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(handler, name, CompilerFlags.ERROR);
			}		
			reportExtraneousElements(elements, 1);
		}
	}
	
	private void validateFeatureAttributes(Element element) {
		if (fMonitor.isCanceled())
			return;
		assertAttributeDefined(element, "id", CompilerFlags.ERROR); //$NON-NLS-1$
		assertAttributeDefined(element, "version", CompilerFlags.ERROR); //$NON-NLS-1$
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			String name = attributes.item(i).getNodeName();
			if (!attrs.contains(name)) {
				reportUnknownAttribute(element, name, CompilerFlags.ERROR);
			} else if (name.equals("primary") || name.equals("exclusive")){ //$NON-NLS-1$ //$NON-NLS-2$
				validateBoolean(element, (Attr)attributes.item(i));
			} else if (name.equals("version")) { //$NON-NLS-1$
				validateVersionAttribute(element, (Attr)attributes.item(i));
			}
		}
	}
	
	private void validatePluginID(Element element, Attr attr, boolean isFragment) {
		int severity = CompilerFlags.getFlag(project, CompilerFlags.F_UNRESOLVED_PLUGINS);
		if (severity != CompilerFlags.IGNORE) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(attr.getValue());
			if (model == null 
					|| !model.isEnabled() 
					|| (isFragment && !model.isFragmentModel())
					|| (!isFragment && model.isFragmentModel())) {
				report(PDE.getFormattedMessage("Builders.Feature.reference", attr.getValue()),  //$NON-NLS-1$
						getLine(element, attr.getName()),
						severity);
			}
		}
	}

	private void validateFeatureID(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(project, CompilerFlags.F_UNRESOLVED_FEATURES);
		if (severity != CompilerFlags.IGNORE) {
			IFeature feature = PDECore.getDefault().findFeature(attr.getValue());	
			if (feature == null) {
				report(PDE.getFormattedMessage("Builders.Feature.freference", attr.getValue()),  //$NON-NLS-1$
						getLine(element, attr.getName()),
						severity);
			}
		}
	}

}
