/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Search reporter that outputs results to an XML file
 * 
 * @since 1.0.1
 */
public class XMLApiSearchReporter implements IApiSearchReporter {

	/**
	 * file names for the output reference files
	 */
	public static final String TYPE_REFERENCES = "type_references"; //$NON-NLS-1$
	public static final String METHOD_REFERENCES = "method_references"; //$NON-NLS-1$
	public static final String FIELD_REFERENCES = "field_references"; //$NON-NLS-1$
	
	private String fLocation = null;
	private HashMap fReferenceMap = null;
	private IApiDescription fDescription = null;
	private DocumentBuilder parser = null;
	private boolean debug = false;
	
	/**
	 * Constructor
	 * 
	 * @param location the absolute path in the local file system to the folder to write the reports to 
	 * @param debug if debugging infos should be written out to the console
	 */
	public XMLApiSearchReporter(String location, boolean debug) {
		fLocation = location;
		this.debug = debug;
		try {
			parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
		}
		catch(FactoryConfigurationError fce) {} 
		catch (ParserConfigurationException e) {}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter#reportResults(org.eclipse.pde.api.tools.internal.provisional.builder.IReference[])
	 */
	public void reportResults(IApiElement element, final IReference[] references) {
		if(fLocation != null) {
			try {
				File parent = new File(fLocation);
				if(!parent.exists()) {
					parent.mkdirs();
				}
				collateResults(references);
				writeXML(parent);
			} 
			catch (Exception e) {
				ApiPlugin.log(e);
			}
			finally {
				if(fReferenceMap != null) {
					fReferenceMap.clear();
					fReferenceMap = null;
				}
			}
		}
	}
	
	/**
	 * Collates the results into like reference kinds
	 * @param references
	 */
	private void collateResults(IReference[] references) throws CoreException {
		if(fReferenceMap == null) {
			fReferenceMap = new HashMap();
		}
		Integer type = null;
		Integer visibility = null;
		String id = null;
		String tname = null;
		HashMap rmap = null;
		HashMap mmap = null;
		HashMap vmap = null;
		HashMap tmap = null;
		HashSet reflist = null;
		IApiAnnotations annot = null;
		IApiComponent rcomponent = null;
		IApiComponent mcomponent = null;
		for (int i = 0; i < references.length; i++) {
			rcomponent = references[i].getResolvedReference().getApiComponent(); 
			id = getId(rcomponent);
			rmap = (HashMap) fReferenceMap.get(id);
			if(rmap == null) {
				rmap = new HashMap();
				fReferenceMap.put(id, rmap);
			}
			mcomponent = references[i].getMember().getApiComponent(); 
			id = getId(mcomponent);
			mmap = (HashMap) rmap.get(id);
			if(mmap == null) {
				mmap = new HashMap();
				rmap.put(id, mmap);
			}
			fDescription = rcomponent.getApiDescription();
			annot = fDescription.resolveAnnotations(references[i].getResolvedReference().getHandle());
			if(annot != null) {
				visibility = new Integer(annot.getVisibility());
				if(annot.getVisibility() == VisibilityModifiers.PRIVATE) {
					IApiComponent host = mcomponent.getHost();
					if(host != null && host.getId().equals(rcomponent.getId())) {
						visibility = new Integer(ApiUseReportConverter.FRAGMENT_PERMISSIBLE);
					}
					else {
						IApiAccess access = fDescription.resolveAccessLevel(
								mcomponent.getHandle(), 
								getPackageDescriptor(references[i].getResolvedReference()));
						if(access != null && access.getAccessLevel() == IApiAccess.FRIEND) {
							visibility = new Integer(VisibilityModifiers.PRIVATE_PERMISSIBLE);
						}
					}
				}
			}
			else {
				//overflow for those references that cannot be resolved
				visibility = new Integer(VisibilityModifiers.ALL_VISIBILITIES);
			}
			vmap = (HashMap) mmap.get(visibility);
			if(vmap == null) {
				vmap = new HashMap();
				mmap.put(visibility, vmap);
			}
			type = new Integer(references[i].getReferenceType());
			tmap = (HashMap) vmap.get(type);
			if(tmap == null) {
				tmap = new HashMap();
				vmap.put(type, tmap);
			}
			tname = getText(references[i].getResolvedReference());
			reflist = (HashSet) tmap.get(tname);
			if(reflist == null) {
				reflist = new HashSet();
				tmap.put(tname, reflist);
			}
			reflist.add(references[i]);
		}
	}
	
	/**
	 * Resolves the id to use for the component in the mapping
	 * @param component
	 * @return the id to use for the component in the mapping, includes the version information as well
	 * @throws CoreException
	 */
	String getId(IApiComponent component) throws CoreException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(component.getId()).append(" ").append('(').append(component.getVersion()).append(')'); //$NON-NLS-1$
		return buffer.toString();
	}
	
	/**
	 * Returns the {@link IPackageDescriptor} for the package that contains the given {@link IApiMember}
	 * @param member
	 * @return a new package descriptor for the given {@link IApiMember}
	 * @throws CoreException
	 */
	private IPackageDescriptor getPackageDescriptor(IApiMember member) throws CoreException {
		IApiType type = null;
		if(member.getType() != IApiElement.TYPE) {
			 type = member.getEnclosingType();
		}
		else {
			type = (IApiType) member;
		}
		return Factory.packageDescriptor(type.getPackageName());
	}
	
	/**
	 * Returns a formatted version of the references xml file name for use during conversion via the default
	 * XSLT file
	 * @param groupname
	 * @return a formatted version of the references file name
	 */
	private String getFormattedTypeName(String groupname) {
		if(TYPE_REFERENCES.equals(groupname)) {
			return "Types"; //$NON-NLS-1$
		}
		if(METHOD_REFERENCES.equals(groupname)) {
			return "Methods"; //$NON-NLS-1$
		}
		if(FIELD_REFERENCES.equals(groupname)) {
			return "Fields"; //$NON-NLS-1$
		}
		return "unknown references"; //$NON-NLS-1$
	}
	
	/**
	 * Returns the name for the file of references base on the given type
	 * @param type
	 * @return
	 */
	private String getRefTypeName(int type) {
		switch(type) {
			case IReference.T_TYPE_REFERENCE: return TYPE_REFERENCES;
			case IReference.T_METHOD_REFERENCE: return METHOD_REFERENCES;
			case IReference.T_FIELD_REFERENCE: return FIELD_REFERENCES;
		}
		return "unknown_reference_kinds"; //$NON-NLS-1$
	}
	
	/**
	 * Writes out the XML for the given api element using the collated {@link IReference}s
	 * @param parent
	 * @throws CoreException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeXML(File parent) throws CoreException, FileNotFoundException, IOException {
		HashMap vismap = null;
		HashMap typemap = null;
		HashMap rmap = null;
		HashMap mmap = null;
		Integer type = null;
		Integer vis = null;
		String id = null;
		String referee = null;
		File root = null;
		File location = null;
		for(Iterator iter = fReferenceMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			id = (String) entry.getKey();
			referee = id;
			location = new File(parent, id);
			if(!location.exists()) {
				location.mkdir();
			}
			rmap = (HashMap) entry.getValue();
			for(Iterator iter2 = rmap.entrySet().iterator(); iter2.hasNext();) {
				Map.Entry entry2 = (Map.Entry) iter2.next();
				id = (String) entry2.getKey();
				root = new File(location, id);
				if(!root.exists()) {
					root.mkdir();
				}
				mmap = (HashMap) entry2.getValue();
				for(Iterator iter4 = mmap.entrySet().iterator(); iter4.hasNext();) {
					Map.Entry entry3 = (Map.Entry) iter4.next();
					vis = (Integer) entry3.getKey();
					location = new File(root, VisibilityModifiers.getVisibilityName(vis.intValue()));
					if(!location.exists()) {
						location.mkdir();
					}
					vismap = (HashMap) entry3.getValue();
					for(Iterator iter3 = vismap.entrySet().iterator(); iter3.hasNext();) {
						Map.Entry entry4 = (Map.Entry) iter3.next();
						type = (Integer) entry4.getKey();
						typemap = (HashMap) entry4.getValue();
						writeGroup(id, referee, location, getRefTypeName(type.intValue()), typemap, vis.intValue());
					}
				}
			}
		}
	}
	
	/**
	 * Writes out a group of references under the newly created element with the given name
	 * @param origin the name of the bundle that has the references in it
	 * @param referee the name of the bundle that is referenced
	 * @param parent
	 * @param name
	 * @param map
	 * @param visibility
	 */
	private void writeGroup(String origin, String referee, File parent, String name, HashMap map, int visibility) throws CoreException, FileNotFoundException, IOException {
		if(parent.exists()) {
			BufferedWriter writer = null;
			try {
				Document doc = null;
				Element root = null;
				int count = 0;
				File out = new File(parent, name+".xml"); //$NON-NLS-1$
				if(out.exists()) {
					try {
						FileInputStream inputStream = null;
						try {
							inputStream = new FileInputStream(out);
							doc = this.parser.parse(inputStream);
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							if (inputStream != null) {
								inputStream.close();
							}
						}
						if (doc == null) {
							return;
						}
						root = doc.getDocumentElement();
						String value = root.getAttribute(IApiXmlConstants.ATTR_REFERENCE_COUNT);
						count = Integer.parseInt(value);
					}
					catch(SAXException se) {
						se.printStackTrace();
					}
				}
				else {
					doc = Util.newDocument();
					root = doc.createElement(IApiXmlConstants.REFERENCES);
					doc.appendChild(root);
					root.setAttribute(IApiXmlConstants.ATTR_REFERENCE_VISIBILITY, Integer.toString(visibility));
					root.setAttribute(IApiXmlConstants.ATTR_ORIGIN, origin);
					root.setAttribute(IApiXmlConstants.ATTR_REFEREE, referee);
					root.setAttribute(IApiXmlConstants.ATTR_NAME, getFormattedTypeName(name));
				}
				if(doc == null) {
					return;
				}
				String tname = null;
				HashSet refs = null;
				Element telement = null;
				for(Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
					Map.Entry entry = (Map.Entry) iter.next();
					tname = (String) entry.getKey();
					telement = findTypeElement(root, tname);
					if(telement == null) {
						telement = doc.createElement(IApiXmlConstants.ATTR_NAME_TYPE_NAME);
						telement.setAttribute(IApiXmlConstants.ATTR_NAME, tname);
						root.appendChild(telement);
					}
					refs = (HashSet) entry.getValue();
					if(refs != null) {
						for(Iterator iter2 = refs.iterator(); iter2.hasNext();) {
							count++;
							writeReference(doc, telement, (IReference) iter2.next());
						}
					}
				}
				root.setAttribute(IApiXmlConstants.ATTR_REFERENCE_COUNT, Integer.toString(count));
				writer = new BufferedWriter(new FileWriter(out));
				writer.write(Util.serializeDocument(doc));
				writer.flush();
			}
			finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
	}
	
	/**
	 * gets the root kind element
	 * @param root
	 * @param kind
	 * @return
	 */
	private Element findTypeElement(Element root, String tname) {
		if(tname == null) {
			return null;
		}
		Element kelement = null;
		NodeList nodes = root.getElementsByTagName(IApiXmlConstants.ATTR_NAME_TYPE_NAME);
		for (int i = 0; i < nodes.getLength(); i++) {
			kelement = (Element) nodes.item(i);
			if(tname.equals(kelement.getAttribute(IApiXmlConstants.ATTR_NAME))) {
				return kelement;
			}
		}
		return null;
	}
	
	/**
	 * gets the root kind element
	 * @param root
	 * @param kind
	 * @return
	 */
	private Element findKindElement(Element root, Integer kind) {
		Element kelement = null;
		NodeList nodes = root.getElementsByTagName(IApiXmlConstants.REFERENCE_KIND);
		for (int i = 0; i < nodes.getLength(); i++) {
			kelement = (Element) nodes.item(i);
			if(kind.toString().equals(kelement.getAttribute(IApiXmlConstants.ATTR_KIND))) {
				return kelement;
			}
		}
		return null;
	}
	
	/**
	 * Writes the attributes from the given {@link IReference} into a new {@link Element} that is added to 
	 * the given parent.
	 * 
	 * @param document
	 * @param parent
	 * @param reference
	 */
	private void writeReference(Document document, Element parent, IReference reference) throws CoreException {
		Element kelement = null;
		Integer kind = new Integer(reference.getReferenceKind());
		kelement = findKindElement(parent, kind);
		if(kelement == null) {
			kelement = document.createElement(IApiXmlConstants.REFERENCE_KIND);
			kelement.setAttribute(IApiXmlConstants.ATTR_REFERENCE_KIND_NAME, Reference.getReferenceText(kind.intValue()));
			kelement.setAttribute(IApiXmlConstants.ATTR_KIND, kind.toString());
			parent.appendChild(kelement);
		}
		Element relement = document.createElement(IApiXmlConstants.ATTR_REFERENCE);
		IApiMember member = reference.getMember();
		relement.setAttribute(IApiXmlConstants.ATTR_ORIGIN, getText(member));
		member = reference.getResolvedReference();
		if(member != null) {
			relement.setAttribute(IApiXmlConstants.ATTR_REFEREE, getText(member));
			relement.setAttribute(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(reference.getLineNumber()));
			String sig = reference.getReferencedSignature();
			if(sig != null) {
				relement.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, sig);
			}
			kelement.appendChild(relement);
		}
	}
	
	/**
	 * Returns the text to set in the attribute for the given {@link IApiMember}
	 * @param member
	 * @return
	 * @throws CoreException
	 */
	private String getText(IApiMember member) throws CoreException {
		switch(member.getType()) {
			case IApiElement.TYPE: return Signatures.getQualifiedTypeSignature((IApiType) member);
			case IApiElement.METHOD: return Signatures.getQualifiedMethodSignature((IApiMethod) member);
			case IApiElement.FIELD: return Signatures.getQualifiedFieldSignature((IApiField) member);
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter#reportNotSearched(org.eclipse.pde.api.tools.internal.provisional.model.IApiElement[])
	 */
	public void reportNotSearched(IApiElement[] elements) {
		if(elements == null) {
			return;
		}
		BufferedWriter writer = null;
		try {
			if(this.debug) {
				System.out.println("Writing file for projects that were not searched..."); //$NON-NLS-1$
			}
			File rootfile = new File(fLocation);
			if(!rootfile.exists()) {
				rootfile.mkdirs();
			}
			File file = new File(rootfile, "not_searched.xml"); //$NON-NLS-1$
			if(!file.exists()) {
				file.createNewFile();
			}
			Document doc = Util.newDocument();
			Element root = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENTS);
			doc.appendChild(root);
			Element comp = null;
			SkippedComponent component = null;
			for(int i = 0; i < elements.length; i++) {
				component = (SkippedComponent)elements[i];
				comp = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
				comp.setAttribute(IApiXmlConstants.ATTR_ID, component.getComponentId());
				comp.setAttribute(IApiXmlConstants.NO_API_DESCRIPTION, Boolean.toString(component.hasNoApiDescription()));
				comp.setAttribute(IApiXmlConstants.EXCLUDED, Boolean.toString(component.wasExcluded()));
				comp.setAttribute(IApiXmlConstants.RESOLUTION_ERRORS, Boolean.toString(component.hasResolutionErrors()));
				root.appendChild(comp);
			}
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(Util.serializeDocument(doc));
			writer.flush();
		}
		catch(FileNotFoundException fnfe) {}
		catch(IOException ioe) {}
		catch(CoreException ce) {}
		finally {
			try {
				writer.close();
			} 
			catch (IOException e) {}
		}
	}
}
