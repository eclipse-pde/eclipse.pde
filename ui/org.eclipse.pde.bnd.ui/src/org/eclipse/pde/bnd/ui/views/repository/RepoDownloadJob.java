/*******************************************************************************
 * Copyright (c) 2015, 2024 bndtools project and others.
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
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.views.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundle;
import org.eclipse.pde.bnd.ui.model.repo.RepositoryBundleVersion;

import aQute.bnd.service.RemoteRepositoryPlugin;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResourceHandle;
import aQute.bnd.service.ResourceHandle.Location;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;

public class RepoDownloadJob extends Job {

	private static final Lock							LOCK	= new ReentrantLock(true);

	private final Collection<RemoteRepositoryPlugin>	repos;
	private final Collection<RepositoryBundle>			bundles;
	private final Collection<RepositoryBundleVersion>	bundleVersions;

	public RepoDownloadJob(Collection<RemoteRepositoryPlugin> repos, Collection<RepositoryBundle> bundles,
		Collection<RepositoryBundleVersion> bundleVersions) {
		super("Downloading Repository Contents");
		this.repos = repos;
		this.bundles = bundles;
		this.bundleVersions = bundleVersions;
	}

	@Override
	protected IStatus run(IProgressMonitor progress) {
		SubMonitor monitor = SubMonitor.convert(progress);

		boolean locked = LOCK.tryLock();
		try {
			while (!locked) {
				monitor.setBlocked(Status.info("Waiting for other download jobs to complete."));
				if (progress.isCanceled())
					return Status.CANCEL_STATUS;

				try {
					locked = LOCK.tryLock(5, TimeUnit.SECONDS);
				} catch (InterruptedException e) {}
			}
			monitor.clearBlocked();

			MultiStatus status = new MultiStatus(RepoDownloadJob.class, 0,
				"One or more repository files failed to download.", null);
			monitor.setTaskName("Expanding repository contents");
			List<RepositoryBundleVersion> rbvs = new LinkedList<>();
			try {
				for (RemoteRepositoryPlugin repo : repos) {
					expandContentsInto(repo, rbvs);
				}
				for (RepositoryBundle bundle : bundles) {
					expandContentsInto(bundle, rbvs);
				}
				rbvs.addAll(bundleVersions);
			} catch (Exception e) {
				return Status.error("Error listing repository contents", e);
			}

			monitor.setWorkRemaining(rbvs.size());
			for (RepositoryBundleVersion rbv : rbvs) {
				if (monitor.isCanceled())
					return Status.CANCEL_STATUS;

				String resourceName = "<<unknown>>";
				try {
					RemoteRepositoryPlugin repo = (RemoteRepositoryPlugin) rbv.getRepo();
					ResourceHandle handle = repo.getHandle(rbv.getBsn(), rbv.getVersion()
						.toString(), Strategy.EXACT, Collections.<String, String> emptyMap());
					resourceName = handle.getName();
					Location location = handle.getLocation();

					if (location == Location.remote) {
						monitor.setTaskName("Downloading " + handle.getName());
						handle.request();
					}
				} catch (Exception e) {
					status.add(Status.error(String.format("Download of %s:%s with remote name %s failed", rbv.getBsn(),
							rbv.getVersion(),
							resourceName),
						e));
				} finally {
					monitor.worked(1);
				}
			}
			return status;
		} finally {
			if (locked)
				LOCK.unlock();
		}

	}

	private void expandContentsInto(RemoteRepositoryPlugin repo, List<RepositoryBundleVersion> rbvs) throws Exception {
		List<String> bsns = repo.list(null);
		if (bsns != null) {
			for (String bsn : bsns) {
				RepositoryBundle bundle = new RepositoryBundle(repo, bsn);
				expandContentsInto(bundle, rbvs);
			}
		}
	}

	private void expandContentsInto(RepositoryBundle bundle, List<RepositoryBundleVersion> rbvs) throws Exception {
		RepositoryPlugin repo = bundle.getRepo();
		SortedSet<Version> versions = repo.versions(bundle.getBsn());
		if (versions != null) {
			for (Version version : versions) {
				RepositoryBundleVersion rbv = new RepositoryBundleVersion(bundle, version);
				rbvs.add(rbv);
			}
		}
	}

	@Override
	protected void canceling() {
		Thread myThread = getThread();
		if (myThread != null)
			myThread.interrupt();
	}

}
