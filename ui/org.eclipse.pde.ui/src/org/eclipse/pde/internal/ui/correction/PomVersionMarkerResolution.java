/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.pde.internal.ui.PDEUIMessages;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.ui.IMarkerResolution;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Marker resolution for when version in pom.xml does not match the plug-in version. 
 * Replaces the version string to one based on the version in the manifest.  The corrected
 * version must have been stored on the marker at creation time.
 * 
 * @since 4.3
 *
 */
public class PomVersionMarkerResolution implements IMarkerResolution {

	private static final String ELEMENT_VERSION = "version"; //$NON-NLS-1$
	private String correctedVersion;

	/**
	 * New marker resolution that will offer to replace the current POM version with corrected version
	 * @param correctedVersion new version to insert
	 */
	public PomVersionMarkerResolution(String correctedVersion) {
		this.correctedVersion = correctedVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		return NLS.bind(PDEUIMessages.PomVersionMarkerResolution_pomVersionResolutionLabel, correctedVersion);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		if (correctedVersion == null || correctedVersion.trim().length() == 0) {
			return;
		}
		IResource resource = marker.getResource();
		if (resource.exists() && resource.getType() == IResource.FILE) {
			IFile file = (IFile) resource;
			if (!file.isReadOnly()) {
				InputStream fileInput = null;
				try {

					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
					fileInput = file.getContents();
					Document doc = docBuilder.parse(fileInput);

					Node root = doc.getDocumentElement();
					NodeList list = root.getChildNodes();

					for (int i = 0; i < list.getLength(); i++) {
						Node node = list.item(i);
						if (ELEMENT_VERSION.equals(node.getNodeName())) {
							node.setTextContent(correctedVersion);
						}
					}

					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					TransformerFactory transformerFactory = TransformerFactory.newInstance();
					Transformer transformer = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(doc);
					StreamResult result = new StreamResult(outputStream);
					transformer.transform(source, result);

					IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] {file}, null);
					if (!status.isOK()) {
						throw new CoreException(status);
					}

					ByteArrayInputStream stream = new ByteArrayInputStream(outputStream.toByteArray());
					file.setContents(stream, true, false, null);

				} catch (ParserConfigurationException e) {
					PDECore.log(e);
				} catch (SAXException e) {
					PDECore.log(e);
				} catch (IOException e) {
					PDECore.log(e);
				} catch (TransformerException e) {
					PDECore.log(e);
				} catch (CoreException e) {
					PDECore.log(e);
				} finally {
					if (fileInput != null) {
						try {
							fileInput.close();
						} catch (IOException e) {
						}
					}
				}

			}

		}
	}
}
