/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.annotations;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;

/**
 * Processes {@link OSGiAnnotations#ANNOTATION_BUNDLE_HEADER}
 * and{@link OSGiAnnotations#ANNOTATION_BUNDLE_HEADERS}
 */
public class CustomHeaderAnnotationProcessor implements OSGiAnnotationProcessor {

	private final List<Entry<String, String>> headers = new ArrayList<>();

	@Override
	public void processAnnotation(Annotation annotation, String type) {
		if (OSGiAnnotations.ANNOTATION_BUNDLE_HEADERS.equals(type)) {
			OSGiAnnotationProcessor.expressions(annotation).filter(Annotation.class::isInstance)
					.map(Annotation.class::cast).forEach(this::addHeaderValue);
		} else if (OSGiAnnotations.ANNOTATION_BUNDLE_HEADER.equals(type)) {
			addHeaderValue(annotation);
		}
	}

	private void addHeaderValue(Annotation annotation) {
		OSGiAnnotationProcessor.member(annotation, "name").flatMap(OSGiAnnotationProcessor::stringValue) //$NON-NLS-1$
				.flatMap(name -> {
					return OSGiAnnotationProcessor.member(annotation, "value") //$NON-NLS-1$
							.flatMap(OSGiAnnotationProcessor::stringValue).map(value -> new SimpleEntry<>(name, value));
				}).ifPresent(headers::add);
	}

	@Override
	public void apply(IBaseModel model) {
		if (model instanceof IBundlePluginModelBase pluginModel) {
			IBundleModel bundleModel = pluginModel.getBundleModel();
			IBundle bundle = bundleModel.getBundle();
			for (Entry<String, String> entry : headers) {
				bundle.setHeader(entry.getKey(), entry.getValue());
			}
		}
	}

}
