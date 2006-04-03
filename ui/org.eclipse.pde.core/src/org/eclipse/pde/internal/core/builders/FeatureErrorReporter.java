/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;


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
		NodeList list = getChildrenByName(parent, "data"); //$NON-NLS-1$
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
		NodeList list = getChildrenByName(parent, "plugin"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element plugin = (Element)list.item(i);
			assertAttributeDefined(plugin, "id", CompilerFlags.ERROR); //$NON-NLS-1$
			assertAttributeDefined(plugin, "version", CompilerFlags.ERROR); //$NON-NLS-1$
			NamedNodeMap attributes = plugin.getAttributes();
			boolean isFragment = plugin.getAttribute("fragment").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$
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
			validateUnpack(plugin);
		}
	}

	private void validateRequires(Element parent) {
		NodeList list = getChildrenByName(parent, "requires"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			validateImports((Element)list.item(0));
			reportExtraneousElements(list, 1);
		}
	}
	
	private void validateImports(Element parent) {
		NodeList list = getChildrenByName(parent, "import"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element element = (Element)list.item(i);
			Attr plugin = element.getAttributeNode("plugin"); //$NON-NLS-1$
			Attr feature = element.getAttributeNode("feature"); //$NON-NLS-1$
			if (plugin == null && feature == null) {
				assertAttributeDefined(element, "plugin", CompilerFlags.ERROR); //$NON-NLS-1$
			} else if (plugin != null && feature != null){
				reportExclusiveAttributes(element, "plugin", "feature", CompilerFlags.ERROR);  //$NON-NLS-1$//$NON-NLS-2$
			} else if (plugin != null) {
				validatePluginID(element, plugin, false);
			} else if (feature != null) {
				validateFeatureID(element, feature);
			}
			NamedNodeMap attributes = element.getAttributes();
			for (int j = 0; j < attributes.getLength(); j++) {
				Attr attr = (Attr) attributes.item(j);
				String name = attr.getName();
				if (name.equals("version")) { //$NON-NLS-1$
					validateVersionAttribute(element, attr);
				} else if (name.equals("match")) { //$NON-NLS-1$
					if (element.getAttributeNode("patch") != null) { //$NON-NLS-1$
						report(
								NLS.bind(PDECoreMessages.Builders_Feature_patchedMatch, attr.getValue()), 
								getLine(element, attr.getValue()), 
								CompilerFlags.ERROR);
					} else {
						validateMatch(element, attr);
					}
				} else if (name.equals("patch")) { //$NON-NLS-1$
					if ("true".equalsIgnoreCase(attr.getValue()) && feature == null) { //$NON-NLS-1$
						report(
								NLS.bind(PDECoreMessages.Builders_Feature_patchPlugin, attr.getValue()), 
								getLine(element, attr.getValue()), 
								CompilerFlags.ERROR);
					} else if ("true".equalsIgnoreCase(attr.getValue()) && element.getAttributeNode("version") == null) { //$NON-NLS-1$ //$NON-NLS-2$
						report(
								NLS.bind(PDECoreMessages.Builders_Feature_patchedVersion, attr.getValue()), 
								getLine(element, attr.getValue()), 
								CompilerFlags.ERROR);
					} else {
						validateBoolean(element, attr);
					}
				} else if (!name.equals("plugin") && !name.equals("feature")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(element, name, CompilerFlags.ERROR);
				}
			}
			
		}
		
	}

	private void validateIncludes(Element parent) {
		NodeList list = getChildrenByName(parent, "includes"); //$NON-NLS-1$
		for (int i = 0; i < list.getLength(); i++) {
			if (fMonitor.isCanceled())
				return;
			Element include = (Element)list.item(i);
			if (assertAttributeDefined(include, "id", CompilerFlags.ERROR) //$NON-NLS-1$
					&& assertAttributeDefined(include, "version", //$NON-NLS-1$
							CompilerFlags.ERROR)) {

				validateFeatureID(include, include.getAttributeNode("id")); //$NON-NLS-1$
			}
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
				} else if (!name.equals("id") && !name.equals("name") && !name.equals("os") && !name.equals("ws") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						&& !name.equals("nl") && !name.equals("arch")) { //$NON-NLS-1$ //$NON-NLS-2$
					reportUnknownAttribute(include, name, CompilerFlags.ERROR);
				}
			}
		}
	}

	private void validateURLElement(Element parent) {
		NodeList list = getChildrenByName(parent, "url"); //$NON-NLS-1$
		if (list.getLength() > 0) {
			Element url = (Element)list.item(0);
			validateUpdateURL(url);
			validateDiscoveryURL(url);
			reportExtraneousElements(list, 1);
		}
	}
	
	private void validateUpdateURL(Element parent) {
		NodeList list = getChildrenByName(parent, "update"); //$NON-NLS-1$
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
		NodeList list = getChildrenByName(parent, "discovery"); //$NON-NLS-1$
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
					reportDeprecatedAttribute(discovery, discovery.getAttributeNode("type")); //$NON-NLS-1$
				} else if (!name.equals("label")) { //$NON-NLS-1$
					reportUnknownAttribute(discovery, name, CompilerFlags.ERROR);
				}
			}
		}		
	}
	
	private void validateCopyright(Element parent) {
		NodeList list = getChildrenByName(parent, "copyright"); //$NON-NLS-1$
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
		NodeList list = getChildrenByName(parent, "license"); //$NON-NLS-1$
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
		NodeList list = getChildrenByName(parent, "description"); //$NON-NLS-1$
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
		NodeList elements = getChildrenByName(element, "install-handler"); //$NON-NLS-1$
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
			} else if (name.equals("id")){ //$NON-NLS-1$
				validatePluginID(element, (Attr)attributes.item(i)); 
			} else if (name.equals("primary") || name.equals("exclusive")){ //$NON-NLS-1$ //$NON-NLS-2$
				validateBoolean(element, (Attr)attributes.item(i));
			} else if (name.equals("version")) { //$NON-NLS-1$
				validateVersionAttribute(element, (Attr)attributes.item(i));
			}
			if (name.equals("primary")){ //$NON-NLS-1$ 
				reportDeprecatedAttribute(element, (Attr)attributes.item(i));
			} else if (name.equals("plugin")){ //$NON-NLS-1$ 
				validatePluginID(element, (Attr)attributes.item(i), false);
			}
		}
	}
	
	private void validatePluginID(Element element, Attr attr, boolean isFragment) {
		String id = attr.getValue();
		if(!validatePluginID(element, attr)){
			return;
		}
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.F_UNRESOLVED_PLUGINS);
		if (severity != CompilerFlags.IGNORE) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(id);
			if (model == null 
					|| !model.isEnabled() 
					|| (isFragment && !model.isFragmentModel())
					|| (!isFragment && model.isFragmentModel())) {
				report(NLS.bind(PDECoreMessages.Builders_Feature_reference, id),  
						getLine(element, attr.getName()),
						severity);
			}
		}
	}

	private void validateFeatureID(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.F_UNRESOLVED_FEATURES);
		if (severity != CompilerFlags.IGNORE) {
			IFeature feature = PDECore.getDefault().findFeature(attr.getValue());	
			if (feature == null) {
				report(NLS.bind(PDECoreMessages.Builders_Feature_freference, attr.getValue()),  
						getLine(element, attr.getName()),
						severity);
			}
		}
	}
	protected void reportExclusiveAttributes(Element element, String attName1, String attName2, int severity) {
		String message = NLS.bind(PDECoreMessages.Builders_Feature_exclusiveAttributes, (new String[] {attName1, attName2}));
		report(message, getLine(element, attName2), severity);
	}

	private void validateUnpack(Element parent) {
		int severity = CompilerFlags.getFlag(fProject,
				CompilerFlags.F_UNRESOLVED_PLUGINS);
		if (severity == CompilerFlags.IGNORE) {
			return;
		}
		if( severity == CompilerFlags.ERROR){
			// this might not be an error, so max the flag at WARNING level.
			severity = CompilerFlags.WARNING;
		}
		String unpack = parent.getAttribute("unpack"); //$NON-NLS-1$
		if ("false".equals(unpack)) //$NON-NLS-1$
			return;
		IPluginModel pModel = PDECore.getDefault().getModelManager()
				.findPluginModel(parent.getAttribute("id")); //$NON-NLS-1$
		if (pModel == null) {
			return;
		}
		if(!CoreUtility.guessUnpack(pModel.getBundleDescription())){
						String message = NLS
						.bind(
								PDECoreMessages.Builders_Feature_missingUnpackFalse,
								(new String[] {
										parent.getAttribute("id"), "unpack=\"false\"" })); //$NON-NLS-1$ //$NON-NLS-2$			
				report(message, getLine(parent), severity);
		}
	}

}
