/*******************************************************************************
 * Copyright (c) 2010, 2024 bndtools project and others.
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
 *     Peter Kriens <peter.kriens@aqute.biz> - ongoing enhancements
 *     Sean Bright <sean@malleable.com> - ongoing enhancements
 *     Gregory Amerson <gregory.amerson@liferay.com> - ongoing enhancements
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Läubrich - Adapt to PDE codebase
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.repo;

import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.pde.bnd.ui.HyperlinkStyler;
import org.eclipse.pde.bnd.ui.Resources;
import org.eclipse.swt.graphics.Image;

import aQute.bnd.build.Project;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.service.Actionable;
import aQute.bnd.service.RepositoryPlugin;

public class RepositoryTreeLabelProvider extends StyledCellLabelProvider
	implements org.eclipse.jface.viewers.ILabelProvider {

	final Image arrowImg = Resources.getImage("arrow_down.png");
	final Image bundleImg = Resources.getImage("bundle.png");
	final Image matchImg = Resources.getImage("star-small.png");
	final Image projectImg = Resources.getImage("$IMG_OBJ_PROJECT");
	final Image loadingImg = Resources.getImage("loading_16x16.gif");

	private final boolean	showRepoId;

	public RepositoryTreeLabelProvider(boolean showRepoId) {
		this.showRepoId = showRepoId;
	}

	@Override
	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		int index = cell.getColumnIndex();
		StyledString label = new StyledString();
		Image image = null;
		try {
			if (element instanceof RepositoryPlugin) {
				if (index == 0) {
					RepositoryPlugin repo = (RepositoryPlugin) element;
					String status = repo.getStatus();
					String name = null;
					if (repo instanceof Actionable) {
						name = ((Actionable) repo).title();
					}
					if (name == null) {
						name = repo.getName();
					}
					label.append(name);

					if (status != null) {
						label.append(" : ");
						label.append(status, StyledString.QUALIFIER_STYLER);
					}
					image = Resources.getImage(repo.getIcon());
				}
			} else if (element instanceof Project) {
				if (index == 0) {
					@SuppressWarnings("resource")
					Project project = (Project) element;
					boolean isOk = project.isOk();

					label.append(project.getName());

					if (showRepoId) {
						label.append(" ");
						label.append("[Workspace]", StyledString.QUALIFIER_STYLER);
					}

					if (!isOk) {
						label.append(" ");
						label.append("Errors: " + project.getErrors()
							.size(), StyledString.COUNTER_STYLER);
					}

					image = projectImg;
				}
			} else if (element instanceof ProjectBundle) {
				if (index == 0) {
					ProjectBundle projectBundle = (ProjectBundle) element;

					label.append(projectBundle.getBsn());
					if (showRepoId) {
						label.append(" ");
						if (projectBundle.isSub()) {
							label.append("[Workspace:" + projectBundle.getProject() + "]",
								StyledString.QUALIFIER_STYLER);
						} else {
							label.append("[Workspace]", StyledString.QUALIFIER_STYLER);
						}
					}
					image = bundleImg;
				}
			} else if (element instanceof RepositoryBundle) {
				if (index == 0) {
					RepositoryBundle bundle = (RepositoryBundle) element;
					label.append(bundle.getText());
					if (showRepoId) {
						label.append(" ");
						label.append("[" + bundle.getRepo()
							.getName() + "]", StyledString.QUALIFIER_STYLER);
					}
					image = bundleImg;
				}
			} else if (element instanceof RepositoryBundleVersion) {
				if (index == 0) {
					RepositoryBundleVersion bundleVersion = (RepositoryBundleVersion) element;
					String versionText = bundleVersion.getText();

					if (versionText.contains(" \u21E9")) {
						versionText = versionText.replaceAll(" \u21E9", "");
						image = arrowImg;
					}
					label.append(versionText, StyledString.COUNTER_STYLER);
				}
			} else if (element instanceof RepositoryResourceElement resourceElem) {
				label.append(resourceElem.getIdentity())
					.append(" ");
				label.append(resourceElem.getVersionString(), StyledString.COUNTER_STYLER);

				image = matchImg;
			} else if (element instanceof ContinueSearchElement) {
				label.append("Continue Search on repository...", new HyperlinkStyler());
				image = null;
			} else if (element instanceof LoadingContentElement) {
				label.append(element.toString());
				image = loadingImg;
			} else if (element != null) {
				label.append(element.toString());
			}
		} catch (Exception e) {
			label.append("error: " + Exceptions.causes(e));
			image = Resources.getImage("error");
		}

		cell.setText(label.getString());
		cell.setStyleRanges(label.getStyleRanges());
		cell.setImage(image);
	}

	/**
	 * Return the text to be shown as a tooltip.
	 * <p>
	 * TODO allow markdown to be used. Not sure how to create a rich text
	 * tooltip though. Would also be nice if we could copy/paste from the
	 * tooltip like in the JDT.
	 * </p>
	 */
	@Override
	public String getToolTipText(Object element) {
		try {
			if (element instanceof Actionable) {
				return ((Actionable) element).tooltip();
			}
		} catch (Exception e) {
			// ignore, use default
		}
		return null;
	}

	@Override
	public Image getImage(Object element) {
		return arrowImg;
	}

	/**
	 *
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof RepositoryPlugin rp) {
			return rp.getName();
		}

		if (element instanceof Project pr) {
			return pr.getName();
		}

		if (element instanceof ProjectBundle pb) {
			return pb.getBsn();
		}

		if (element instanceof RepositoryBundle rb) {
			return rb.getText();
		}

		if (element instanceof RepositoryBundleVersion rbv) {
			return rbv.getText();
		}

		if (element instanceof RepositoryResourceElement re) {
			return re.getIdentity();
		}

		return element.toString();
	}
}
