/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private static RepositoryCache instance;

	Map<String, List<UnitNode>> cache = new HashMap<>();

	private RepositoryCache() {
		//avoid instantiation
	}

	/**
	 * @return default instance of this cache.
	 */

	public static RepositoryCache getDefault() {
		if (instance == null) {
			instance = new RepositoryCache();
		}
		return instance;
	}

	/**
	 * Fetches information and caches it.
	 *
	 * @param repo
	 *            repository URL
	 * @param flush
	 *            whether a flush is needed
	 * @return list of IUs available in the 'repo' repository. Never
	 *         <code>null</code>.
	 */
	public List<UnitNode> fetchP2UnitsFromRepo(String repo, boolean flush) {
		if ((flush) || (cache.get(repo) == null)) {
			List<UnitNode> units = P2Fetcher.fetchAvailableUnits(repo);
			cache.put(repo, units);
		}
		return cache.get(repo);
	}

	/**
	 *
	 * Method used to narrow down proposals in case a prefix is provided.
	 * Example:
	 *
	 * <pre>
	 *  &ltunit id="org.^
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
	public List<UnitNode> getUnitsByPrefix(String repo, String prefix) {
		List<UnitNode> allUnits = fetchP2UnitsFromRepo(repo, false);
		List<UnitNode> result = new ArrayList<>();
		for (UnitNode unit : allUnits) {
			if (unit.getId().startsWith(prefix)) {
				result.add(unit);
			}
		}
		return result;
	}

	/**
	 *
	 * Method used to narrow down proposals in case a prefix is provided. Example:
	 *
	 * <pre>
	 *  &ltunit id="eclipse^
	 * </pre>
	 *
	 * where ^ is an autocomplete call. Search term in this case will be '*eclipse*'
	 *
	 * @param repo
	 *            repository URL
	 * @param searchTerm
	 *            A prefix used to narrow down the match list
	 * @return A list of IUs whose id contains 'searchTerm'
	 */
	public List<UnitNode> getUnitsBySearchTerm(String repo, String searchTerm) {
		List<UnitNode> allUnits = fetchP2UnitsFromRepo(repo, false);
		List<UnitNode> result = new ArrayList<>();
		for (UnitNode unit : allUnits) {
			if (unit.getId().contains(searchTerm)) {
				result.add(unit);
			}
		}
		return result;
	}

	/**
	 * Classic cache up-to-date check.
	 *
	 * @param repo
	 *            repository URL
	 * @return whether the cache is up to date for this repo
	 */
	public boolean isUpToDate(String repo) {
		return cache.get(repo) != null;
	}

	/**
	 * Used to flush cache in case P2 repo information is considered stale.
	 */
	public void flush() {
		cache.clear();
	}
}
