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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter;
import org.eclipse.pde.api.tools.internal.provisional.search.IMetadata;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Search reporter that outputs results to an XML file
 * 
 * @since 1.0.1
 */
public class XmlSearchReporter implements IApiSearchReporter {
	
	private String fLocation = null;
	private DocumentBuilder parser = null;
	private boolean debug = false; 
	
	/**
	 * Constructor
	 * 
	 * @param location the absolute path in the local file system to the folder to write the reports to 
	 * @param debug if debugging infos should be written out to the console
	 */
	public XmlSearchReporter(String location, boolean debug) {
		fLocation = location;
		this.debug = debug;
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter#reportResults(org.eclipse.pde.api.tools.internal.provisional.builder.IReference[])
	 */
	public void reportResults(IApiElement element, final IReference[] references) {
		if(fLocation != null) {
			XmlReferenceDescriptorWriter writer = new XmlReferenceDescriptorWriter(fLocation);
			List descriptors = new ArrayList(references.length + 1);
			for (int i = 0; i < references.length; i++) {
				Reference reference = (Reference) references[i];
				try {
					descriptors.add(reference.getReferenceDescriptor());
				} catch (CoreException e) {
					ApiPlugin.log(e.getStatus());
				}
			}
			writer.writeReferences((IReferenceDescriptor[]) descriptors.toArray(new IReferenceDescriptor[descriptors.size()]));
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
				comp.setAttribute(IApiXmlConstants.ATTR_VERSION, component.getVersion());
				comp.setAttribute(IApiXmlConstants.SKIPPED_DETAILS, component.getErrorDetails());
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
				if(writer != null) {
					writer.close();
				}
			} 
			catch (IOException e) {}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchReporter#reportMetadata(org.eclipse.pde.api.tools.internal.provisional.search.IMetadata)
	 */
	public void reportMetadata(IMetadata data) {
		if(data == null) {
			return;
		}
		try {
			if(this.debug) {
				System.out.println("Writing file for projects that were not searched..."); //$NON-NLS-1$
			}
			File rootfile = new File(fLocation);
			if(!rootfile.exists()) {
				rootfile.mkdirs();
			}
			File file = new File(rootfile, "meta.xml"); //$NON-NLS-1$
			if(!file.exists()) {
				file.createNewFile();
			}
			data.serializeToFile(file);
		}
		catch(FileNotFoundException fnfe) {
			ApiPlugin.log(fnfe);
		}
		catch(IOException ioe) {
			ApiPlugin.log(ioe);
		}
		catch (CoreException ce) {
			ApiPlugin.log(ce);
		}
	}
}
