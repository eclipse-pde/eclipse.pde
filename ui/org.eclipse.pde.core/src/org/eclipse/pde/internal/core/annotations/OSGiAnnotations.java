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

import java.util.Collection;
import java.util.List;

public interface OSGiAnnotations {

	String ANNOTATION_BUNDLE_EXPORT = "org.osgi.annotation.bundle.Export"; //$NON-NLS-1$
	String ANNOTATION_BUNDLE_HEADER = "org.osgi.annotation.bundle.Header"; //$NON-NLS-1$
	String ANNOTATION_BUNDLE_HEADERS = "org.osgi.annotation.bundle.Headers"; //$NON-NLS-1$
	String ANNOTATION_VERSIONING_VERSION = "org.osgi.annotation.versioning.Version"; //$NON-NLS-1$
	Collection<String> SUPPORTED_ANNOTATIONS = List.of(ANNOTATION_VERSIONING_VERSION,
			ANNOTATION_BUNDLE_EXPORT, ANNOTATION_BUNDLE_HEADER, ANNOTATION_BUNDLE_HEADERS);

}
