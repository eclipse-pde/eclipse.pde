/*******************************************************************************
 * Copyright (c) 2010, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     Ferry Huberts <ferry.huberts@pelagic.nl> - ongoing enhancements
 *     Peter Kriens <Peter.Kriens@aqute.biz> - ongoing enhancements
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.model.repo;

import java.util.Map;
import java.util.SortedSet;

import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;

/**
 * Abstracts the Bundle in repository views, it wraps the underlying Repository
 * Plugin with the bsn of the bundle. It supports {@code Actionable} by
 * implementing its methods but forwarding them to the Repository Plugin.
 */
public class RepositoryBundle extends RepositoryEntry implements Actionable {

	public RepositoryBundle(final RepositoryPlugin repo, final String bsn) {
		super(repo, bsn, new VersionFinder("latest", Strategy.HIGHEST) {
			@Override
			Version findVersion() throws Exception {
				SortedSet<Version> vs = repo.versions(bsn);
				if (vs == null || vs.isEmpty()) {
					return null;
				}
				return vs.last();
			}
		});
	}

	@Override
	public String toString() {
		return "RepositoryBundle [repo=" + getRepo() + ", bsn=" + getBsn() + "]";
	}

	@Override
	public String title(Object... target) throws Exception {
		try {
			if (getRepo() instanceof Actionable) {
				String s = ((Actionable) getRepo()).title(getBsn());
				if (s != null) {
					return s;
				}
			}
		} catch (Exception e) {
			// just default
		}
		return getBsn();
	}

	@Override
	public String tooltip(Object... target) throws Exception {
		if (getRepo() instanceof Actionable) {
			String s = ((Actionable) getRepo()).tooltip(getBsn());
			if (s != null) {
				return s;
			}
		}
		return null;
	}

	@Override
	public Map<String, Runnable> actions(Object... target) throws Exception {
		Map<String, Runnable> map = null;
		try {
			if (getRepo() instanceof Actionable) {
				map = ((Actionable) getRepo()).actions(getBsn());
			}
		} catch (Exception e) {
			// just default
		}
		return map;
	}

	public String getText() {
		try {
			return title();
		} catch (Exception e) {
			return getBsn();
		}
	}
}
