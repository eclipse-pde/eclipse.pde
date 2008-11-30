/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui;

import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.ua.core.ctxhelp.ICtxHelpConstants;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.toc.ITocConstants;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;

public class PDEUserAssistanceLabelProvider extends SharedLabelProvider {
	public PDEUserAssistanceLabelProvider() {
	}

	public String getText(Object obj) {
		if (obj instanceof ISimpleCSObject) {
			return getObjectText((ISimpleCSObject) obj);
		}
		if (obj instanceof ICompCSObject) {
			return getObjectText((ICompCSObject) obj);
		}
		if (obj instanceof TocObject) {
			return getObjectText((TocObject) obj);
		}
		if (obj instanceof CtxHelpObject) {
			return getObjectText((CtxHelpObject) obj);
		}
		return super.getText(obj);
	}

	public String getObjectText(ISimpleCSObject obj) {
		int limit = 50;

		if (obj.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
			limit = 40;
		} else if (obj.getType() == ISimpleCSConstants.TYPE_ITEM) {
			limit = 36;
		} else if (obj.getType() == ISimpleCSConstants.TYPE_INTRO) {
			limit = 36;
		} else if (obj.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			limit = 32;
		}
		return PDETextHelper.truncateAndTrailOffText(PDETextHelper.translateReadText(obj.getName()), limit);
	}

	public String getObjectText(ICompCSObject obj) {
		int limit = 40;
		ICompCSObject parent = obj.getParent();
		while (parent != null) {
			limit = limit - 4;
			parent = parent.getParent();
		}
		return PDETextHelper.truncateAndTrailOffText(PDETextHelper.translateReadText(obj.getName()), limit);
	}

	/**
	 * @param obj
	 */
	public String getObjectText(TocObject obj) {
		return PDETextHelper.translateReadText(obj.getName());
	}

	public String getObjectText(CtxHelpObject obj) {
		return PDETextHelper.translateReadText(obj.getName());
	}

	public Image getImage(Object obj) {
		if (obj instanceof ISimpleCSObject) {
			return getObjectImage((ISimpleCSObject) obj);
		}
		if (obj instanceof ICompCSObject) {
			return getObjectImage((ICompCSObject) obj);
		}
		if (obj instanceof TocObject) {
			return getObjectImage((TocObject) obj);
		}
		if (obj instanceof CtxHelpObject) {
			return getObjectImage((CtxHelpObject) obj);
		}
		return super.getImage(obj);
	}

	private Image getObjectImage(ISimpleCSObject object) {

		if (object.getType() == ISimpleCSConstants.TYPE_ITEM) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSITEM_OBJ);
		} else if (object.getType() == ISimpleCSConstants.TYPE_SUBITEM) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSSUBITEM_OBJ);
		} else if (object.getType() == ISimpleCSConstants.TYPE_REPEATED_SUBITEM) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSUNSUPPORTED_OBJ);
		} else if (object.getType() == ISimpleCSConstants.TYPE_CONDITIONAL_SUBITEM) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSUNSUPPORTED_OBJ);
		} else if (object.getType() == ISimpleCSConstants.TYPE_CHEAT_SHEET) {
			return get(PDEUserAssistanceUIPluginImages.DESC_SIMPLECS_OBJ);
		} else if (object.getType() == ISimpleCSConstants.TYPE_INTRO) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSINTRO_OBJ);
		} else if (object.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSUNSUPPORTED_OBJ);
		}
		return get(PDEUserAssistanceUIPluginImages.DESC_SIMPLECS_OBJ, F_ERROR);
	}

	/**
	 * @param object
	 */
	private Image getObjectImage(ICompCSObject object) {

		if (object.getType() == ICompCSConstants.TYPE_TASK) {
			return get(PDEUserAssistanceUIPluginImages.DESC_SIMPLECS_OBJ);
		} else if (object.getType() == ICompCSConstants.TYPE_TASKGROUP) {
			return get(PDEUserAssistanceUIPluginImages.DESC_CSTASKGROUP_OBJ);
		} else if (object.getType() == ICompCSConstants.TYPE_COMPOSITE_CHEATSHEET) {
			return get(PDEUserAssistanceUIPluginImages.DESC_COMPCS_OBJ);
		}
		return get(PDEUserAssistanceUIPluginImages.DESC_SIMPLECS_OBJ, F_ERROR);
	}

	/**
	 * @param object
	 */
	private Image getObjectImage(TocObject object) {
		switch (object.getType()) {
			case ITocConstants.TYPE_TOC : {
				return get(PDEUserAssistanceUIPluginImages.DESC_TOC_OBJ);
			}
			case ITocConstants.TYPE_TOPIC : { //Return the leaf topic icon for a topic with no children
				if (object.getChildren().isEmpty()) {
					return get(PDEUserAssistanceUIPluginImages.DESC_TOC_LEAFTOPIC_OBJ);
				}
				//Return the regular topic icon for a topic with children
				return get(PDEUserAssistanceUIPluginImages.DESC_TOC_TOPIC_OBJ);
			}
			case ITocConstants.TYPE_LINK : {
				return get(PDEUserAssistanceUIPluginImages.DESC_TOC_LINK_OBJ);
			}
			case ITocConstants.TYPE_ANCHOR : {
				return get(PDEUserAssistanceUIPluginImages.DESC_TOC_ANCHOR_OBJ);
			}
			default :
				return get(PDEUserAssistanceUIPluginImages.DESC_SIMPLECS_OBJ, F_ERROR);
		}
	}

	/**
	 * @param object
	 */
	private Image getObjectImage(CtxHelpObject object) {
		switch (object.getType()) {
			case ICtxHelpConstants.TYPE_ROOT : {
				return get(PDEUserAssistanceUIPluginImages.DESC_TOC_OBJ);
			}
			case ICtxHelpConstants.TYPE_CONTEXT : {
				return get(PDEUserAssistanceUIPluginImages.DESC_CTXHELP_CONTEXT_OBJ);
			}
			case ICtxHelpConstants.TYPE_DESCRIPTION : {
				return get(PDEUserAssistanceUIPluginImages.DESC_CTXHELP_DESC_OBJ);
			}
			case ICtxHelpConstants.TYPE_TOPIC : {
				return get(PDEUserAssistanceUIPluginImages.DESC_TOC_LEAFTOPIC_OBJ);
			}
			case ICtxHelpConstants.TYPE_COMMAND : {
				return get(PDEUserAssistanceUIPluginImages.DESC_CTXHELP_COMMAND_OBJ);
			}
			default :
				return get(PDEUserAssistanceUIPluginImages.DESC_SIMPLECS_OBJ, F_ERROR);
		}
	}
}
