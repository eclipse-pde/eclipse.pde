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
package org.eclipse.pde.bnd.ui.preferences;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public interface ReposPreference {

	public static final String KEY_TEMPLATE_REPO_URI_LIST = "templateRepoUriList";

	public static final boolean DEF_ENABLE_TEMPLATE_REPOSITORIES = false;

	public static final String KEY_ENABLE_TEMPLATE_REPOSITORIES = "enableTemplateRepositories";

	public static final String TEMPLATE_LOADER_NODE = "repoTemplateLoader";

	public static final Function<String, List<String>> TEMPLATE_REPOSITORIES_PARSER = s -> s == null || s.isBlank()
			? List.of()
			: Arrays.asList(s.split("\\s"));
}