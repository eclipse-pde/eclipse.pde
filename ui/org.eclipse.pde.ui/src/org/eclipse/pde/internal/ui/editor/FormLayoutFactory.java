/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * FormLayoutFactory
 *
 */
public class FormLayoutFactory {

	// UI Forms Standards
	
	public static final int FORM_BODY_MARGIN_HEIGHT = 0;
	public static final int FORM_BODY_MARGIN_WIDTH = 0;

	public static final int FORM_BODY_MARGIN_TOP = 12;
	public static final int FORM_BODY_MARGIN_BOTTOM = 12;
	public static final int FORM_BODY_MARGIN_LEFT = 6;
	public static final int FORM_BODY_MARGIN_RIGHT = 6;

	public static final int FORM_BODY_HORIZONTAL_SPACING = 20;
	// Should be 20; but, we minus 3 because the section automatically pads the 
	// bottom margin that amount 	
	public static final int FORM_BODY_VERTICAL_SPACING = 17;
	
	// Should be 6; but, we minus 4 because the section automatically pads the
	// left margin that amount
	public static final int SECTION_CLIENT_MARGIN_LEFT = 2;
	// Should be 6; but, we minus 4 because the section automatically pads the
	// right margin that amount	
	public static final int SECTION_CLIENT_MARGIN_RIGHT = 2;	
	public static final int SECTION_CLIENT_HORIZONTAL_SPACING = 5;
	public static final int SECTION_CLIENT_VERTICAL_SPACING = 5;
	public static final int SECTION_CLIENT_MARGIN_TOP = 5;
	public static final int SECTION_CLIENT_MARGIN_BOTTOM = 5;
	public static final int SECTION_CLIENT_MARGIN_HEIGHT = 5;

	public static final int SECTION_HEADER_VERTICAL_SPACING = 6;

	
	/**
	 * 
	 */
	private FormLayoutFactory() {
		// NO-OP
	}
	
    /**
     * For form bodies.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createFormGridLayout(boolean makeColumnsEqualWidth,
			int numColumns) {
		GridLayout layout = new GridLayout();

		layout.marginHeight = FORM_BODY_MARGIN_HEIGHT;
		layout.marginWidth = FORM_BODY_MARGIN_WIDTH;

		layout.marginTop = FORM_BODY_MARGIN_TOP;
		layout.marginBottom = FORM_BODY_MARGIN_BOTTOM;
		layout.marginLeft = FORM_BODY_MARGIN_LEFT;
		layout.marginRight = FORM_BODY_MARGIN_RIGHT;

		layout.horizontalSpacing = FORM_BODY_HORIZONTAL_SPACING;
		layout.verticalSpacing = FORM_BODY_VERTICAL_SPACING;   	
		
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;
		
		return layout;
    }

    /**
     * For miscellaneous grouping composites.
     * For sections (as a whole - header plus client).
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createClearGridLayout(boolean makeColumnsEqualWidth,
			int numColumns) {
		GridLayout layout = new GridLayout();

		layout.marginHeight = 0;
		layout.marginWidth = 0;

		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;

		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;   	
		
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;
		
		return layout;
    }    
    
    /**
     * For form bodies.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static TableWrapLayout createFormTableWrapLayout(
    		boolean makeColumnsEqualWidth,
			int numColumns) {
		TableWrapLayout layout = new TableWrapLayout();
		
		layout.topMargin = FORM_BODY_MARGIN_TOP;
		layout.bottomMargin = FORM_BODY_MARGIN_BOTTOM;
		layout.leftMargin = FORM_BODY_MARGIN_LEFT;
		layout.rightMargin = FORM_BODY_MARGIN_RIGHT;

		layout.horizontalSpacing = FORM_BODY_HORIZONTAL_SPACING;
		layout.verticalSpacing = FORM_BODY_VERTICAL_SPACING;   	

		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;

		return layout;
    }
    
    /**
     * For composites used to group sections in left and right panes.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static TableWrapLayout createFormPaneTableWrapLayout(
    		boolean makeColumnsEqualWidth,
			int numColumns) {
		TableWrapLayout layout = new TableWrapLayout();
		
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;

		layout.horizontalSpacing = FORM_BODY_HORIZONTAL_SPACING;
		layout.verticalSpacing = FORM_BODY_VERTICAL_SPACING;   	

		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;

		return layout;
    }    
    
    /**
     * For composites used to group sections in left and right panes.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createFormPaneGridLayout(boolean makeColumnsEqualWidth,
			int numColumns) {
		GridLayout layout = new GridLayout();

		layout.marginHeight = 0;
		layout.marginWidth = 0;

		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.marginRight = 0;

		layout.horizontalSpacing = FORM_BODY_HORIZONTAL_SPACING;
		layout.verticalSpacing = FORM_BODY_VERTICAL_SPACING;   	
		
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;
		
		return layout;
    }     
    
    /**
     * For miscellaneous grouping composites.
     * For sections (as a whole - header plus client).
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static TableWrapLayout createClearTableWrapLayout(
    		boolean makeColumnsEqualWidth,
    		int numColumns) {
		TableWrapLayout layout = new TableWrapLayout();
		layout.topMargin = 0;
		layout.bottomMargin = 0;
		layout.leftMargin = 0;
		layout.rightMargin = 0;

		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;   	

		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;   
		
		return layout;
    }	
    
    /**
     * For master sections belonging to a master details block.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createMasterGridLayout(
    		boolean makeColumnsEqualWidth,
    		int numColumns) {
		GridLayout layout = new GridLayout();

		layout.marginHeight = 0;
		layout.marginWidth = 0;

		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		// Cannot set layout on a sash form.
		// In order to replicate the horizontal spacing between sections,
		// divide the amount by 2 and set the master section right margin to 
		// half the amount and set the left details section margin to half
		// the amount.  The default sash width is currently set at 3.
		// Minus 1 pixel from each half.  Use the 1 left over pixel to separate
		// the details section from the vertical scollbar.
		int marginRight = FORM_BODY_HORIZONTAL_SPACING;
		if (marginRight > 0) {
			marginRight = marginRight / 2;
			if (marginRight > 0) {
				marginRight--;
			}
		}
		layout.marginRight = marginRight;

		layout.horizontalSpacing = FORM_BODY_HORIZONTAL_SPACING;
		layout.verticalSpacing = FORM_BODY_VERTICAL_SPACING;   	
		
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;
		
		return layout;
    }    
    
    /**
     * For details sections belonging to a master details block.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createDetailsGridLayout(
    		boolean makeColumnsEqualWidth,
    		int numColumns) {
		GridLayout layout = new GridLayout();

		layout.marginHeight = 0;
		layout.marginWidth = 0;

		layout.marginTop = 0;
		layout.marginBottom = 0;
		// Cannot set layout on a sash form.
		// In order to replicate the horizontal spacing between sections,
		// divide the amount by 2 and set the master section right margin to 
		// half the amount and set the left details section margin to half
		// the amount.  The default sash width is currently set at 3.
		// Minus 1 pixel from each half.  Use the 1 left over pixel to separate
		// the details section from the vertical scollbar.
		int marginLeft = FORM_BODY_HORIZONTAL_SPACING;
		if (marginLeft > 0) {
			marginLeft = marginLeft / 2;
			if (marginLeft > 0) {
				marginLeft--;
			}
		}		
		layout.marginLeft = marginLeft;
		layout.marginRight = 1;		
		layout.marginRight = 0;

		layout.horizontalSpacing = FORM_BODY_HORIZONTAL_SPACING;
		layout.verticalSpacing = FORM_BODY_VERTICAL_SPACING;   	
		
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;
		
		return layout;
    }       
    
    /**
     * For composites set as section clients.
     * For composites containg form text.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createSectionClientGridLayout(boolean makeColumnsEqualWidth,
			int numColumns) {
		GridLayout layout = new GridLayout();

		layout.marginHeight = SECTION_CLIENT_MARGIN_HEIGHT;
		layout.marginWidth = 0;

		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginLeft = SECTION_CLIENT_MARGIN_LEFT;
		layout.marginRight = SECTION_CLIENT_MARGIN_RIGHT;

		layout.horizontalSpacing = SECTION_CLIENT_HORIZONTAL_SPACING;
		layout.verticalSpacing = SECTION_CLIENT_VERTICAL_SPACING;   	
		
		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;
		
		return layout;
    }     
    
    /**
     * For composites set as section clients.
     * For composites containg form text.
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static TableWrapLayout createSectionClientTableWrapLayout(boolean makeColumnsEqualWidth,
			int numColumns) {
		TableWrapLayout layout = new TableWrapLayout();
		
		layout.topMargin = SECTION_CLIENT_MARGIN_TOP;
		layout.bottomMargin = SECTION_CLIENT_MARGIN_BOTTOM;
		layout.leftMargin = SECTION_CLIENT_MARGIN_LEFT;
		layout.rightMargin = SECTION_CLIENT_MARGIN_RIGHT;

		layout.horizontalSpacing = SECTION_CLIENT_HORIZONTAL_SPACING;
		layout.verticalSpacing = SECTION_CLIENT_VERTICAL_SPACING;   	

		layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
		layout.numColumns = numColumns;     	
		
		return layout;
    }      
    
    /**
     * Debug method.
     * 
     * MAGENTA = 11
     * CYAN = 13
     * GREEN = 5
     * @param container
     * @param color
     */
    public static void visualizeLayoutArea(Composite container, int color) {
    	container.setBackground(Display.getCurrent().getSystemColor(color));
    }
}
