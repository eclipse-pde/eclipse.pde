/*******************************************************************************
 * Copyright (c) 2016, 2024 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 531918] filter suggestions
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.pde.internal.genericeditor.target.extension.p2.P2Fetcher;

/**
 * This class is used to cache the p2 repositories completion information order
 * to minimize IO round trips and have instant completion for IUs and their
 * versions.
 *
 * There will be only one cache shared between editors. In the future a function
 * will be added for the user to be able to flush this cache.
 */
public class RepositoryCache {

	private RepositoryCache() {
		// avoid instantiation
	}

	private static final Map<String, List<UnitNode>> CACHE = new ConcurrentHashMap<>();

	/**
	 * Fetches information and caches it.
	 *
	 * @return list of IUs available in the 'repo' repository. Never
	 *         <code>null</code>.
	 */
	public static List<UnitNode> fetchP2UnitsFromRepo(String repo) {
		return CACHE.computeIfAbsent(repo, P2Fetcher::fetchAvailableUnits);
	}

	/**
	 *
	 * Method used to narrow down proposals in case a prefix is provided.
	 * Example:
	 *
	 * <pre>
	 *  &lt;unit id="org.^
	 * </pre>
	 *
	 * where ^ is an autocomplete call. Prefix in this case will be 'org.'
	 *
	 * @param repo
	 *            repository URL
	 * @param prefix
	 *            A prefix used to narrow down the match list
	 * @return A list of IUs whose id starts with 'prefix'
	 */
	public static List<UnitNode> getUnitsByPrefix(String repo, String prefix) {
		List<UnitNode> allUnits = fetchP2UnitsFromRepo(repo);
		return allUnits.stream().filter(unit -> unit.getId().startsWith(prefix)).toList();
	}

	/**
	 *
	 * Method used to narrow down proposals in case a prefix is provided.
	 * Example:
	 *
	 * <pre>
	 *  &lt;unit id="eclipse^
	 * </pre>
	 *
	 * where ^ is an autocomplete call. Search term in this case will be
	 * '*eclipse*'
	 *
	 * @param repo
	 *            repository URL
	 * @param searchTerm
	 *            A prefix used to narrow down the match list
	 * @return A list of IUs whose id contains 'searchTerm'
	 */
	public static List<UnitNode> getUnitsBySearchTerm(String repo, String searchTerm) {
		List<UnitNode> allUnits = fetchP2UnitsFromRepo(repo);
		return allUnits.stream().filter(unit -> unit.getId().contains(searchTerm)).toList();
	}

	/**
	 * Classic cache up-to-date check.
	 *
	 * @param repo
	 *            repository URL
	 * @return whether the cache is up to date for this repo
	 */
	public static boolean isUpToDate(String repo) {
		return CACHE.get(repo) != null;
	}
}
