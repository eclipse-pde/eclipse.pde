/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Writes reference descriptions to XML files.
 * 
 * @since 1.0.1
 */
public class XmlReferenceDescriptorWriter {

	/**
	 * file names for the output reference files
	 */
	public static final String TYPE_REFERENCES = "type_references"; //$NON-NLS-1$
	public static final String METHOD_REFERENCES = "method_references"; //$NON-NLS-1$
	public static final String FIELD_REFERENCES = "field_references"; //$NON-NLS-1$
	private static final Integer V_ILLEGAL = new Integer(VisibilityModifiers.ILLEGAL_API);
	private String fLocation = null;
	private HashMap fReferenceMap = null;
	private DocumentBuilder parser = null;
	
	/**
	 * Alternate API component where references were unresolved, or <code>null</code>
	 * if not to be reported.
	 */
	private IComponentDescriptor alternate;
	
	/**
	 * Constructor
	 * 
	 * @param location the absolute path in the local file system to the folder to write the reports to 
	 * @param debug if debugging infos should be written out to the console
	 */
	public XmlReferenceDescriptorWriter(String location) {
		fLocation = location;
		try {
			parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
		}
		catch(FactoryConfigurationError fce) {
			ApiPlugin.log(fce);
		} 
		catch (ParserConfigurationException pce) {
			ApiPlugin.log(pce);
		}
	}

	/**
	 * Writes the given references to XML files.
	 * 
	 * @param references
	 */
	public void writeReferences(IReferenceDescriptor[] references) {
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
	 * Collates the results into like reference kinds.  If two references have the same reference, referencer, type,
	 * visibility, and member, one will be removed (even if the line numbers differ).  Updates {@link #fReferenceMap}
	 * with a map based tree structure as follows:
	 * <pre>
	 * Returned Map (Referenced Component ID -> rmap)
	 * rmap (Referencing Component ID -> mmap)
	 * mmap (Visibility -> vmap)
	 * vmap (Reference Type -> tmap)
	 * tmap (Referenced Member -> Reference Descriptor) 
	 * </pre>
	 * @param references
	 */
	private void collateResults(IReferenceDescriptor[] references) throws CoreException {
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
		IComponentDescriptor rcomponent = null;
		IComponentDescriptor mcomponent = null;
		for (int i = 0; i < references.length; i++) {
			rcomponent = references[i].getReferencedComponent(); 
			id = getId(rcomponent);
			rmap = (HashMap) fReferenceMap.get(id);
			if(rmap == null) {
				rmap = new HashMap();
				fReferenceMap.put(id, rmap);
			}
			mcomponent = references[i].getComponent(); 
			id = getId(mcomponent);
			mmap = (HashMap) rmap.get(id);
			if(mmap == null) {
				mmap = new HashMap();
				rmap.put(id, mmap);
			}
			if((references[i].getReferenceFlags() & IReference.F_ILLEGAL) > 0) {
				visibility = V_ILLEGAL;
			}
			else {
				visibility = new Integer(references[i].getVisibility());
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
			tname = getText(references[i].getReferencedMember());
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
	String getId(IComponentDescriptor component) throws CoreException {
		StringBuffer buffer = new StringBuffer();
		buffer.append(component.getId()).append(" ").append('(').append(component.getVersion()).append(')'); //$NON-NLS-1$
		return buffer.toString();
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
		File base = null;
		for(Iterator iter = fReferenceMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			id = (String) entry.getKey();
			referee = id;
			base = new File(parent, id);
			if(!base.exists()) {
				base.mkdir();
			}
			rmap = (HashMap) entry.getValue();
			for(Iterator iter2 = rmap.entrySet().iterator(); iter2.hasNext();) {
				Map.Entry entry2 = (Map.Entry) iter2.next();
				id = (String) entry2.getKey();
				root = new File(base, id);
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
					if (alternate != null) {
						root.setAttribute(IApiXmlConstants.ATTR_ALTERNATE, getId(alternate));
					}
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
						telement = doc.createElement(IApiXmlConstants.ELEMENT_TARGET);
						telement.setAttribute(IApiXmlConstants.ATTR_NAME, tname);
						root.appendChild(telement);
					}
					refs = (HashSet) entry.getValue();
					if(refs != null) {
						for(Iterator iter2 = refs.iterator(); iter2.hasNext();) {
							count++;
							IReferenceDescriptor ref = (IReferenceDescriptor) iter2.next();
							writeReference(doc, telement, ref);
							if (!iter2.hasNext()) {
								// set qualified referenced attributes
								IMemberDescriptor resolved  = ref.getReferencedMember();
								if (resolved != null) {
									addMemberDetails(telement, resolved);
								}
							}
						}
					}
				}
				root.setAttribute(IApiXmlConstants.ATTR_REFERENCE_COUNT, Integer.toString(count));
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), IApiCoreConstants.UTF_8));
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
	 * Add member descriptor details to the given element.
	 * 
	 * @param element XML element
	 * @param member member to add details for
	 */
	private void addMemberDetails(Element element, IMemberDescriptor member) {
		switch (member.getElementType()) {
		case IElementDescriptor.TYPE:
			element.setAttribute(IApiXmlConstants.ATTR_TYPE, ((IReferenceTypeDescriptor)member).getQualifiedName());
			break;
		case IElementDescriptor.FIELD:
			IReferenceTypeDescriptor encl = member.getEnclosingType();
			element.setAttribute(IApiXmlConstants.ATTR_TYPE, encl.getQualifiedName());
			element.setAttribute(IApiXmlConstants.ATTR_MEMBER_NAME, member.getName());
			break;
		case IElementDescriptor.METHOD:
			encl = member.getEnclosingType();
			element.setAttribute(IApiXmlConstants.ATTR_TYPE, encl.getQualifiedName());
			element.setAttribute(IApiXmlConstants.ATTR_MEMBER_NAME, member.getName());
			element.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, ((IMethodDescriptor)member).getSignature());
			break;
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
		NodeList nodes = root.getElementsByTagName(IApiXmlConstants.ELEMENT_TARGET);
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
	private void writeReference(Document document, Element parent, IReferenceDescriptor reference) throws CoreException {
		Element kelement = null;
		Integer kind = new Integer(reference.getReferenceKind());
		kelement = findKindElement(parent, kind);
		if(kelement == null) {
			kelement = document.createElement(IApiXmlConstants.REFERENCE_KIND);
			kelement.setAttribute(IApiXmlConstants.ATTR_REFERENCE_KIND_NAME, Reference.getReferenceText(kind.intValue()));
			kelement.setAttribute(IApiXmlConstants.ATTR_KIND, kind.toString());
			kelement.setAttribute(IApiXmlConstants.ATTR_FLAGS, Integer.toString(reference.getReferenceFlags()));
			parent.appendChild(kelement);
		}
		Element relement = document.createElement(IApiXmlConstants.ATTR_REFERENCE);
		IMemberDescriptor member = reference.getMember();
		relement.setAttribute(IApiXmlConstants.ATTR_ORIGIN, getText(member));
		String[] messages = reference.getProblemMessages();
		if(messages != null) {
			relement.setAttribute(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS, getText(messages));
		}
		// add detailed information about origin
		addMemberDetails(relement, member);
		member = reference.getReferencedMember();
		if(member != null) {
			relement.setAttribute(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(reference.getLineNumber()));
			kelement.appendChild(relement);
		}
	}
	
	/**
	 * Gets the {@link String} value of the given array by calling {@link #toString()} on each of the elements
	 * in the array.
	 * 
	 * @param array the array to convert to a string
	 * @return the {@link String} or an empty {@link String} never <code>null</code>
	 * @since 1.1
	 */
	String getText(Object[] array) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < array.length; i++) {
			buffer.append(array[i].toString());
			if(i < array.length-1) {
				buffer.append(","); //$NON-NLS-1$
			}
		}
		return buffer.toString();
	}
	
	/**
	 * Returns the text to set in the attribute for the given {@link IApiMember}
	 * @param member
	 * @return
	 * @throws CoreException
	 */
	private String getText(IMemberDescriptor member) throws CoreException {
		switch(member.getElementType()) {
			case IElementDescriptor.TYPE: return Signatures.getQualifiedTypeSignature((IReferenceTypeDescriptor) member);
			case IElementDescriptor.METHOD: return Signatures.getQualifiedMethodSignature((IMethodDescriptor) member);
			case IElementDescriptor.FIELD: return Signatures.getQualifiedFieldSignature((IFieldDescriptor) member);
		}
		return null;
	}

	/**
	 * Sets the alternate component where references were unresolved, or <code>null</code>
	 * if none.
	 * 
	 * @param other component descriptor or <code>null</code>
	 */
	public void setAlternate(IComponentDescriptor other) {
		alternate = other;
	}
}
