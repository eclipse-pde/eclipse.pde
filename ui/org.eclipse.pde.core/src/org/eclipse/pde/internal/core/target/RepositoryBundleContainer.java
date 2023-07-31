/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.repository.CacheManager;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.util.PDEXmlProcessorFactory;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import aQute.bnd.osgi.repository.ResourcesRepository;
import aQute.bnd.osgi.repository.XMLResourceParser;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.osgi.resource.ResourceUtils.ContentCapability;

@SuppressWarnings("restriction")
public class RepositoryBundleContainer extends AbstractBundleContainer {
	public static final String ATTRIBUTE_URI = "uri"; //$NON-NLS-1$

	public static final String ELEMENT_REQUIRE = "require"; //$NON-NLS-1$

	public static final String TYPE = "Repository"; //$NON-NLS-1$

	private final String uri;

	private final Collection<Requirement> requirements;

	public RepositoryBundleContainer(String uri, Collection<Requirement> requirements) {
		this.uri = uri;
		this.requirements = requirements;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		ResourcesRepository repository = getRepository(monitor);
		Map<Requirement, Collection<Capability>> providers = repository.findProviders(getRequirements());
		List<TargetBundle> bundles = new ArrayList<>();
		List<ContentCapability> contentCapabilities = providers.values().stream().flatMap(Collection::stream)
				.map(Capability::getResource).distinct().map(ResourceUtils::getContentCapability)
				.filter(Objects::nonNull).toList();
		CacheManager cacheManager = getCacheManager();
		for (ContentCapability content : contentCapabilities) {
			URI url = content.url();
			try {
				File file;
				if (cacheManager != null) {
					file = cacheManager.createCacheFromFile(url, monitor);
				} else {
					file = new File(FileLocator.toFileURL(url.toURL()).toURI());
				}
				bundles.add(new TargetBundle(file));
			} catch (IOException | URISyntaxException e) {
				throw new CoreException(Status.error("Can't fetch bundle from " + url, e));
			}
		}
		return bundles.toArray(TargetBundle[]::new);
	}

	public String getUri() {
		return uri;
	}

	public Collection<Requirement> getRequirements() {
		return requirements;
	}

	public ResourcesRepository getRepository(IProgressMonitor monitor) throws CoreException {
		String location = getLocation(true);
		try {
			URI base = new URI(location);
			try {
				CacheManager cacheManager = getCacheManager();
				if (cacheManager != null) {
					File file = cacheManager.createCacheFromFile(base, monitor);
					return new ResourcesRepository(XMLResourceParser.getResources(file, base));
				}
				return new ResourcesRepository(XMLResourceParser.getResources(base));
			} catch (Exception e) {
				if (e instanceof CoreException core) {
					throw core;
				}
				if (e instanceof RuntimeException runtime) {
					throw runtime;
				}
				throw new CoreException(Status.error("Loading repository from " + location + " failed: " + e, e));
			}
		} catch (URISyntaxException e) {
			throw new CoreException(Status.error("Invalid URI: " + location, e));
		}
	}

	private CacheManager getCacheManager() throws CoreException {
		return P2TargetUtils.getAgent().getService(CacheManager.class);
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		return new TargetFeature[0];
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return resolveVariables(uri);
		}
		return uri;
	}

	@Override
	public String serialize() {
		try {
			DocumentBuilder docBuilder = PDEXmlProcessorFactory.createDocumentBuilderWithErrorOnDOCTYPE();
			Document document = docBuilder.newDocument();
			Element containerElement = document.createElement(TargetDefinitionPersistenceHelper.LOCATION);
			containerElement.setAttribute(TargetDefinitionPersistenceHelper.ATTR_LOCATION_TYPE, TYPE);
			containerElement.setAttribute(ATTRIBUTE_URI, getUri());
			for (Requirement requirement : requirements) {
				Element requireElement = document.createElement(ELEMENT_REQUIRE);
				requireElement.setTextContent(requirement.toString());
				containerElement.appendChild(requireElement);
			}
			document.appendChild(containerElement);
			StreamResult result = new StreamResult(new StringWriter());
			TransformerFactory f = PDEXmlProcessorFactory.createTransformerFactoryWithErrorOnDOCTYPE();
			Transformer transformer = f.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
			transformer.transform(new DOMSource(document), result);
			String xml = result.getWriter().toString();
			return xml;
		} catch (Exception e) {
			PDECore.log(e);
			return null;
		}
	}

	public void reload() {
		fResolutionStatus = null;
		fBundles = null;
	}
}
