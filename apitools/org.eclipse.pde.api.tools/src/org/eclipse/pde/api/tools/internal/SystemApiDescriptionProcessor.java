/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ScannerMessages;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Provides tools for scanning/loading/parsing component.xml files.
 * 
 * @since 1.0.0
 */
public class SystemApiDescriptionProcessor {
	/**
	 * Constructor
	 * can not be instantiated directly
	 */
	private SystemApiDescriptionProcessor() {}
	
	/**
	 * Parses a component XML into a string. The location may be a jar, directory containing the component.xml file, or 
	 * the component.xml file itself
	 * 
	 * @param location root location of the component.xml file, or the component.xml file itself
	 * @return component XML as a string or <code>null</code> if none
	 * @throws IOException if unable to parse
	 */
	public static String serializeComponentXml(File location) {
		if(location.exists()) {
			ZipFile jarFile = null;
			InputStream stream = null;
			try {
				String extension = new Path(location.getName()).getFileExtension();
				if (extension != null && extension.equals("jar") && location.isFile()) { //$NON-NLS-1$
					jarFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry manifestEntry = jarFile.getEntry(IApiCoreConstants.SYSTEM_API_DESCRIPTION_XML_NAME);
					if (manifestEntry != null) {
						stream = jarFile.getInputStream(manifestEntry);
					}
				} else if(location.isDirectory()) {
					File file = new File(location, IApiCoreConstants.SYSTEM_API_DESCRIPTION_XML_NAME);
					if (file.exists()) {
						stream = new FileInputStream(file);
					}
				}
				else if(location.isFile()) {
					if(location.getName().equals(IApiCoreConstants.SYSTEM_API_DESCRIPTION_XML_NAME)) {
						stream = new FileInputStream(location);
					}
				}
				if(stream != null) {
						return new String(Util.getInputStreamAsCharArray(stream, -1, IApiCoreConstants.UTF_8));
				}
			} catch(IOException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					if (stream != null) {
						stream.close();
					}
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
				try {
					if (jarFile != null) {
						jarFile.close();
					}
				} catch (IOException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return null;
	}
	/**
	 * Throws an exception with the given message and underlying exception.
	 * 
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException
	 */
	private static void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, message, exception);
		throw new CoreException(status);
	}

	/**
	 * Parses the given xml document (in string format), and annotates the specified 
	 * {@link IApiDescription} with {@link IPackageDescriptor}s, {@link IReferenceTypeDescriptor}s, {@link IMethodDescriptor}s
	 * and {@link IFieldDescriptor}s.
	 * 
	 * @param settings API settings to annotate
	 * @param xml XML used to generate settings
	 * @throws CoreException 
	 */
	public static void annotateApiSettings(IApiDescription settings, String xml) throws CoreException {
		Element root = null;
		try {
			root = Util.parseDocument(xml);
		}
		catch(CoreException ce) {
			abort("Failed to parse API description xml file", ce); //$NON-NLS-1$
		}
		if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
			abort(ScannerMessages.ComponentXMLScanner_0, null); 
		}
		NodeList packages = root.getElementsByTagName(IApiXmlConstants.ELEMENT_PACKAGE);
		NodeList types = null;
		IPackageDescriptor packdesc = null;
		Element type = null;
		for (int i = 0; i < packages.getLength(); i++) {
			Element pkg = (Element) packages.item(i);
			// package visibility comes from the MANIFEST.MF
			String pkgName = pkg.getAttribute(IApiXmlConstants.ATTR_NAME);
			packdesc = Factory.packageDescriptor(pkgName);
			annotateDescriptor(settings, packdesc, pkg);
			types = pkg.getElementsByTagName(IApiXmlConstants.ELEMENT_TYPE);
			for (int j = 0; j < types.getLength(); j++) {
				type = (Element) types.item(j);
				String name = type.getAttribute(IApiXmlConstants.ATTR_NAME);
				if (name.length() == 0) {
					abort("Missing type name", null); //$NON-NLS-1$
				}
				IReferenceTypeDescriptor typedesc = packdesc.getType(name); 
				annotateDescriptor(settings, typedesc, type);
				annotateMethodSettings(settings, typedesc, type);
				annotateFieldSettings(settings, typedesc, type);
			}
		}
	}
	
	/**
	 * Annotates the backing {@link IApiDescription} from the given {@link Element}, by adding the visibility
	 * and restriction attributes to the specified {@link IElementDescriptor}
	 * 
	 * @param settings the settings to annotate
	 * @param descriptor the current descriptor context
	 * @param element the current element to annotate from
	 */
	private static void annotateDescriptor(IApiDescription settings, IElementDescriptor descriptor, Element element) {
		settings.setVisibility(descriptor, VisibilityModifiers.API);
		settings.setRestrictions(descriptor, RestrictionModifiers.NO_RESTRICTIONS);
		settings.setAddedProfile(descriptor, retrieveElementAttribute(element, IApiXmlConstants.ATTR_ADDED_PROFILE));
		settings.setRemovedProfile(descriptor, retrieveElementAttribute(element, IApiXmlConstants.ATTR_REMOVED_PROFILE));
		settings.setSuperclass(descriptor, retrieveStringElementAttribute(element, IApiXmlConstants.ATTR_SUPER_CLASS));
		settings.setSuperinterfaces(descriptor, retrieveStringElementAttribute(element, IApiXmlConstants.ATTR_SUPER_INTERFACES));
		settings.setInterface(descriptor, retrieveBooleanElementAttribute(element, IApiXmlConstants.ATTR_INTERFACE));
	}
	/**
	 * Tests if the given restriction exists for the given element
	 * and returns an updated restrictions flag.
	 * 
	 * @param element XML element
	 * @param name attribute to test
	 * @param flag bit mask for attribute
	 * @param res flag to combine with 
	 * @return updated flags
	 */
	private static int retrieveElementAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if (value.length() > 0) {
			return Integer.parseInt(value);
		}
		return ProfileModifiers.NO_PROFILE_VALUE;
	}
	/**
	 * Tests if the given restriction exists for the given element
	 * and returns an updated restrictions flag.
	 * 
	 * @param element XML element
	 * @param name attribute to test
	 * @param flag bit mask for attribute
	 * @param res flag to combine with 
	 * @return updated flags
	 */
	private static String retrieveStringElementAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if (value.length() > 0) {
			return value;
		}
		return null;
	}
	/**
	 * Tests if the given restriction exists for the given element
	 * and returns an updated restrictions flag.
	 * 
	 * @param element XML element
	 * @param name attribute to test
	 * @param flag bit mask for attribute
	 * @param res flag to combine with 
	 * @return updated flags
	 */
	private static boolean retrieveBooleanElementAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if (value.length() > 0) {
			return Boolean.toString(true).equals(value);
		}
		return false;
	}
	/**
	 * Annotates the supplied {@link IApiDescription} from all of the field elements
	 * that are direct children of the specified {@link Element}. {@link IFieldDescriptor}s are created
	 * as needed and added as children of the specified {@link IReferenceTypeDescriptor}.
	 * 
	 * @param settings the {@link IApiDescription} to add the new {@link IFieldDescriptor} to
	 * @param typedesc the containing type descriptor for this field
	 * @param type the parent {@link Element}
	 * @throws CoreException
	 */
	private static void annotateFieldSettings(IApiDescription settings, IReferenceTypeDescriptor typedesc, Element type) throws CoreException {
		NodeList fields = type.getElementsByTagName(IApiXmlConstants.ELEMENT_FIELD);
		Element field = null;
		IFieldDescriptor fielddesc = null;
		String name = null;
		for(int i = 0; i < fields.getLength(); i++) {
			field = (Element) fields.item(i);
			name = field.getAttribute(IApiXmlConstants.ATTR_NAME);
			if(name == null) {
				abort(ScannerMessages.ComponentXMLScanner_1, null); 
			}
			fielddesc = typedesc.getField(name);
			annotateDescriptor(settings, fielddesc, field);
		}
	}
	
	/**
	 * Annotates the supplied {@link IApiDescription} from all of the method elements
	 * that are direct children of the specified {@link Element}. {@link IMethodDescriptor}s are created
	 * as needed and added as children of the specified {@link IReferenceTypeDescriptor}.
	 * 
	 * @param settings the {@link IApiDescription} to add the new {@link IMethodDescriptor} to 
	 * @param typedesc the containing type descriptor for this method
	 * @param type the parent {@link Element}
	 * @throws CoreException
	 */
	private static void annotateMethodSettings(IApiDescription settings, IReferenceTypeDescriptor typedesc, Element type) throws CoreException {
		NodeList methods = type.getElementsByTagName(IApiXmlConstants.ELEMENT_METHOD);
		Element method = null;
		IMethodDescriptor methoddesc = null;
		String name, signature;
		for(int i = 0; i < methods.getLength(); i++) {
			method = (Element) methods.item(i);
			name = method.getAttribute(IApiXmlConstants.ATTR_NAME);
			if(name == null) {
				abort(ScannerMessages.ComponentXMLScanner_2, null); 
			}
			signature = method.getAttribute(IApiXmlConstants.ATTR_SIGNATURE);
			if(signature == null) {
				abort(ScannerMessages.ComponentXMLScanner_3, null); 
			}
			methoddesc = typedesc.getMethod(name, signature);
			annotateDescriptor(settings, methoddesc, method);
		}
	}
}
