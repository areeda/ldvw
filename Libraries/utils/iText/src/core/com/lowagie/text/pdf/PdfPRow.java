/*
 * $Id: PdfPRow.java 3777 2009-03-07 17:03:57Z blowagie $
 *
 * Copyright 2001, 2002 Paulo Soares
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the License.
 *
 * The Original Code is 'iText, a free JAVA-PDF library'.
 *
 * The Initial Developer of the Original Code is Bruno Lowagie. Portions created by
 * the Initial Developer are Copyright (C) 1999, 2000, 2001, 2002 by Bruno Lowagie.
 * All Rights Reserved.
 * Co-Developer of the code is Paulo Soares. Portions created by the Co-Developer
 * are Copyright (C) 2000, 2001, 2002 by Paulo Soares. All Rights Reserved.
 *
 * Contributor(s): all the names of the contributors are added in the source code
 * where applicable.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * LGPL license (the "GNU LIBRARY GENERAL PUBLIC LICENSE"), in which case the
 * provisions of LGPL are applicable instead of those above.  If you wish to
 * allow use of your version of this file only under the terms of the LGPL
 * License and not to allow others to use your version of this file under
 * the MPL, indicate your decision by deleting the provisions above and
 * replace them with the notice and other provisions required by the LGPL.
 * If you do not delete the provisions above, a recipient may use your version
 * of this file under either the MPL or the GNU LIBRARY GENERAL PUBLIC LICENSE.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the MPL as stated above or under the terms of the GNU
 * Library General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library general Public License for more
 * details.
 *
 * If you didn't download this code from the following link, you should check if
 * you aren't using an obsolete version:
 * http://www.lowagie.com/iText/
 */

package com.lowagie.text.pdf;

import java.awt.Color;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.ExceptionConverter;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;

/**
 * A row in a PdfPTable.
 * 
 * @author Paulo Soares (psoares@consiste.pt)
 */
public class PdfPRow {

	/** the bottom limit (bottom right y) */
	public static final float BOTTOM_LIMIT = -(1 << 30);
	/**
	 * the right limit
	 * @since	2.1.5
	 */
	public static final float RIGHT_LIMIT = 20000;

	protected PdfPCell cells[];

	protected float widths[];

	protected float maxHeight = 0;

	protected boolean calculated = false;
    
    private int[] canvasesPos;

	/**
	 * Constructs a new PdfPRow with the cells in the array that was passed
	 * as a parameter.
	 * 
	 * @param cells
	 */
	public PdfPRow(PdfPCell cells[]) {
		this.cells = cells;
		widths = new float[cells.length];
	}

	/**
	 * Makes a copy of an existing row.
	 * 
	 * @param row
	 */
	public PdfPRow(PdfPRow row) {
		maxHeight = row.maxHeight;
		calculated = row.calculated;
		cells = new PdfPCell[row.cells.length];
		for (int k = 0; k < cells.length; ++k) {
			if (row.cells[k] != null)
				cells[k] = new PdfPCell(row.cells[k]);
		}
		widths = new float[cells.length];
		System.arraycopy(row.widths, 0, widths, 0, cells.length);
	}

	/**
	 * Sets the widths of the columns in the row.
	 * 
	 * @param widths
	 * @return true if everything went right
	 */
	public boolean setWidths(float widths[]) {
		if (widths.length != cells.length)
			return false;
		System.arraycopy(widths, 0, this.widths, 0, cells.length);
		float total = 0;
		calculated = false;
		for (int k = 0; k < widths.length; ++k) {
			PdfPCell cell = cells[k];
			cell.setLeft(total);
			int last = k + cell.getColspan();
			for (; k < last; ++k)
				total += widths[k];
			--k;
			cell.setRight(total);
			cell.setTop(0);
		}
		return true;
	}

	/**
	 * Calculates the heights of each cell in the row.
	 * 
	 * @return the maximum height of the row.
	 */
	public float calculateHeights() {
		maxHeight = 0;
		for (int k = 0; k < cells.length; ++k) {
			PdfPCell cell = cells[k];
			if (cell == null)
				continue;
			boolean pivoted = (cell.getRotation() == 90 || cell.getRotation() == 270);
			Image img = cell.getImage();
			if (img != null) {
				img.scalePercent(100);
				float refWidth = pivoted ? img.getScaledHeight() : img.getScaledWidth();
				float scale = (cell.getRight() - cell.getEffectivePaddingRight()
	                    - cell.getEffectivePaddingLeft() - cell.getLeft()) / refWidth;
				img.scalePercent(scale * 100);
				float refHeight = pivoted ? img.getScaledWidth() : img.getScaledHeight();
				cell.setBottom(cell.getTop() - cell.getEffectivePaddingTop()
						- cell.getEffectivePaddingBottom() - refHeight);
			}
			else {
				if (pivoted && cell.hasFixedHeight())
					cell.setBottom(cell.getTop() - cell.getFixedHeight());
				else {
					ColumnText ct = ColumnText.duplicate(cell.getColumn());
					float right, top, left, bottom;
                	if (pivoted) {
                		right = RIGHT_LIMIT;
                		top = cell.getRight() - cell.getEffectivePaddingRight();
                		left = 0;
                		bottom = cell.getLeft() + cell.getEffectivePaddingLeft();
                	}
                	else {
                		right = cell.isNoWrap() ? RIGHT_LIMIT : cell.getRight() - cell.getEffectivePaddingRight();
                		top = cell.getTop() - cell.getEffectivePaddingTop();
                		left = cell.getLeft() + cell.getEffectivePaddingLeft();
                		bottom = cell.hasFixedHeight() ? top + cell.getEffectivePaddingBottom() - cell.getFixedHeight() : BOTTOM_LIMIT;
                	}
                	setColumn(ct, left, bottom, right, top);
					try {
						ct.go(true);
					} catch (DocumentException e) {
						throw new ExceptionConverter(e);
					}
					if (pivoted)
						cell.setBottom(cell.getTop() - cell.getEffectivePaddingTop() - cell.getEffectivePaddingBottom() - ct.getFilledWidth());
					else {
						float yLine = ct.getYLine();
						if (cell.isUseDescender())
							yLine += ct.getDescender();
						cell.setBottom(yLine - cell.getEffectivePaddingBottom());
					}
				}
			}
			float height = cell.getFixedHeight();
			if (height <= 0)
				height = cell.getHeight();
			if (height < cell.getFixedHeight())
				height = cell.getFixedHeight();
			else if (height < cell.getMinimumHeight())
				height = cell.getMinimumHeight();
			if (height > maxHeight)
				maxHeight = height;
		}
		calculated = true;
		return maxHeight;
	}

	/**
	 * Writes the border and background of one cell in the row.
	 * 
	 * @param xPos The x-coordinate where the table starts on the canvas
	 * @param yPos The y-coordinate where the table starts on the canvas
	 * @param cell
	 * @param canvases
	 */
	public void writeBorderAndBackground(float xPos, float yPos, PdfPCell cell, PdfContentByte[] canvases) {
		Color background = cell.getBackgroundColor();
		if (background != null || cell.hasBorders()) {
			// Add xPos resp. yPos to the cell's coordinates for absolute coordinates
			float right = cell.getRight() + xPos;
			float top = cell.getTop() + yPos;
			float left = cell.getLeft() + xPos;
			float bottom = top - maxHeight;
			if (background != null) {
				PdfContentByte backgr = canvases[PdfPTable.BACKGROUNDCANVAS];
				backgr.setColorFill(background);
				backgr.rectangle(left, bottom, right - left, top - bottom);
				backgr.fill();
			}
			if (cell.hasBorders()) {
				Rectangle newRect = new Rectangle(left, bottom, right, top);
				// Clone non-position parameters except for the background color
				newRect.cloneNonPositionParameters(cell);
				newRect.setBackgroundColor(null);
				// Write the borders on the line canvas
				PdfContentByte lineCanvas = canvases[PdfPTable.LINECANVAS];
				lineCanvas.rectangle(newRect);
			}
		}
	}

    private void saveAndRotateCanvases(PdfContentByte[] canvases, float a, float b, float c, float d, float e, float f) {
        int last = PdfPTable.TEXTCANVAS + 1;
        if (canvasesPos == null)
            canvasesPos = new int[last * 2];
        for (int k = 0; k < last; ++k) {
            ByteBuffer bb = canvases[k].getInternalBuffer();
            canvasesPos[k * 2] = bb.size();
            canvases[k].saveState();
            canvases[k].concatCTM(a, b, c, d, e, f);
            canvasesPos[k * 2 + 1] = bb.size();
        }
    }
    
    private void restoreCanvases(PdfContentByte[] canvases) {
        int last = PdfPTable.TEXTCANVAS + 1;
        for (int k = 0; k < last; ++k) {
            ByteBuffer bb = canvases[k].getInternalBuffer();
            int p1 = bb.size();
            canvases[k].restoreState();
            if (p1 == canvasesPos[k * 2 + 1])
                bb.setSize(canvasesPos[k * 2]);
        }
    }
    
    private float setColumn(ColumnText ct, float left, float bottom, float right, float top) {
        if (left > right)
            right = left;
        if (bottom > top)
            top = bottom;
        ct.setSimpleColumn(left, bottom, right, top);
        return top;
    }
    
	/**
	 * Writes a number of cells (not necessarily all cells).
	 * 
	 * @param	colStart The first column to be written.
	 * Remember that the column index starts with 0.
	 * @param	colEnd The last column to be written.
	 * Remember that the column index starts with 0.
	 * If -1, all the columns to the end are written.
	 * @param	xPos The x-coordinate where the table starts on the canvas
	 * @param	yPos The y-coordinate where the table starts on the canvas
	 */
	public void writeCells(int colStart, int colEnd, float xPos, float yPos, PdfContentByte[] canvases) {
		if (!calculated)
			calculateHeights();
		if (colEnd < 0)
			colEnd = cells.length;
		else
			colEnd = Math.min(colEnd, cells.length);
		if (colStart < 0)
			colStart = 0;
		if (colStart >= colEnd)
			return;
		int newStart;
		for (newStart = colStart; newStart >= 0; --newStart) {
			if (cells[newStart] != null)
				break;
			xPos -= widths[newStart - 1];
		}
		xPos -= cells[newStart].getLeft();
		
		for (int k = newStart; k < colEnd; ++k) {
			PdfPCell cell = cells[k];
			if (cell == null)
				continue;
			writeBorderAndBackground(xPos, yPos, cell, canvases);
			Image img = cell.getImage();
			float tly = 0;
			switch (cell.getVerticalAlignment()) {
			case Element.ALIGN_BOTTOM:
				tly = cell.getTop() + yPos - maxHeight + cell.getHeight()
						- cell.getEffectivePaddingTop();
				break;
			case Element.ALIGN_MIDDLE:
				tly = cell.getTop() + yPos + (cell.getHeight() - maxHeight) / 2
						- cell.getEffectivePaddingTop();
				break;
			default:
				tly = cell.getTop() + yPos - cell.getEffectivePaddingTop();
				break;
			}
			if (img != null) {
                if (cell.getRotation() != 0) {
                    img = Image.getInstance(img);
                    img.setRotation(img.getImageRotation() + (float)(cell.getRotation() * Math.PI / 180.0));
                }
				boolean vf = false;
				if (cell.getHeight() > maxHeight) {
					img.scalePercent(100);
					float scale = (maxHeight - cell.getEffectivePaddingTop() - cell
							.getEffectivePaddingBottom())
							/ img.getScaledHeight();
					img.scalePercent(scale * 100);
					vf = true;
				}
				float left = cell.getLeft() + xPos
						+ cell.getEffectivePaddingLeft();
				if (vf) {
					switch (cell.getHorizontalAlignment()) {
					case Element.ALIGN_CENTER:
						left = xPos
								+ (cell.getLeft() + cell.getEffectivePaddingLeft()
										+ cell.getRight()
										- cell.getEffectivePaddingRight() - img
										.getScaledWidth()) / 2;
						break;
					case Element.ALIGN_RIGHT:
						left = xPos + cell.getRight()
								- cell.getEffectivePaddingRight()
								- img.getScaledWidth();
						break;
					default:
						break;
					}
					tly = cell.getTop() + yPos - cell.getEffectivePaddingTop();
				}
				img.setAbsolutePosition(left, tly - img.getScaledHeight());
				try {
					canvases[PdfPTable.TEXTCANVAS].addImage(img);
				} catch (DocumentException e) {
					throw new ExceptionConverter(e);
				}
			} else {
                // rotation sponsored by Connection GmbH
                if (cell.getRotation() == 90 || cell.getRotation() == 270) {
                    float netWidth = maxHeight - cell.getEffectivePaddingTop() - cell.getEffectivePaddingBottom();
                    float netHeight = cell.getWidth() - cell.getEffectivePaddingLeft() - cell.getEffectivePaddingRight();
                    ColumnText ct = ColumnText.duplicate(cell.getColumn());
                    ct.setCanvases(canvases);
                    ct.setSimpleColumn(0, 0, netWidth + 0.001f, -netHeight);
                    try {
                        ct.go(true);
                    } catch (DocumentException e) {
                        throw new ExceptionConverter(e);
                    }
                    float calcHeight = -ct.getYLine();
                    if (netWidth <= 0 || netHeight <= 0)
                        calcHeight = 0;
                    if (calcHeight > 0) {
                        if (cell.isUseDescender())
                            calcHeight -= ct.getDescender();
                        ct = ColumnText.duplicate(cell.getColumn());
                        ct.setCanvases(canvases);
                        ct.setSimpleColumn(0, -0.001f, netWidth + 0.001f, calcHeight);
                        float pivotX;
                        float pivotY;
                        if (cell.getRotation() == 90) {
                            pivotY = cell.getTop() + yPos - maxHeight + cell.getEffectivePaddingBottom();
                            switch (cell.getVerticalAlignment()) {
                            case Element.ALIGN_BOTTOM:
                                pivotX = cell.getLeft() + xPos + cell.getWidth() - cell.getEffectivePaddingRight();
                                break;
                            case Element.ALIGN_MIDDLE:
                                pivotX = cell.getLeft() + xPos + (cell.getWidth() + cell.getEffectivePaddingLeft() - cell.getEffectivePaddingRight() + calcHeight) / 2;
                                break;
                            default: //top
                                pivotX = cell.getLeft() + xPos + cell.getEffectivePaddingLeft() + calcHeight;
                                break;
                            }
                            saveAndRotateCanvases(canvases, 0,1,-1,0,pivotX,pivotY);
                        }
                        else {
                            pivotY = cell.getTop() + yPos - cell.getEffectivePaddingTop();
                            switch (cell.getVerticalAlignment()) {
                            case Element.ALIGN_BOTTOM:
                                pivotX = cell.getLeft() + xPos + cell.getEffectivePaddingLeft();
                                break;
                            case Element.ALIGN_MIDDLE:
                                pivotX = cell.getLeft() + xPos + (cell.getWidth() + cell.getEffectivePaddingLeft() - cell.getEffectivePaddingRight() - calcHeight) / 2;
                                break;
                            default: //top
                                pivotX = cell.getLeft() + xPos + cell.getWidth() - cell.getEffectivePaddingRight() - calcHeight;
                                break;
                            }
                            saveAndRotateCanvases(canvases, 0,-1,1,0,pivotX,pivotY);
                        }
                        try {
                            ct.go();
                        } catch (DocumentException e) {
                            throw new ExceptionConverter(e);
                        } finally {
                            restoreCanvases(canvases);
                        }
                    }
                } 
                else {
                    float fixedHeight = cell.getFixedHeight();
                    float rightLimit = cell.getRight() + xPos
                            - cell.getEffectivePaddingRight();
                    float leftLimit = cell.getLeft() + xPos
                            + cell.getEffectivePaddingLeft();
                    if (cell.isNoWrap()) {
                        switch (cell.getHorizontalAlignment()) {
                            case Element.ALIGN_CENTER:
                                rightLimit += 10000;
                                leftLimit -= 10000;
                                break;
                            case Element.ALIGN_RIGHT:
                            	if (cell.getRotation() == 180) {
                            		rightLimit += RIGHT_LIMIT;
                            	}
                            	else {
                            		leftLimit -= RIGHT_LIMIT;
                            	}
                                break;
                            default:
                            	if (cell.getRotation() == 180) {
                            		leftLimit -= RIGHT_LIMIT;
                            	}
                            	else {
                            		rightLimit += RIGHT_LIMIT;
                            	}
                                break;
                        }
                    }
                    ColumnText ct = ColumnText.duplicate(cell.getColumn());
                    ct.setCanvases(canvases);
                    float bry = tly
                            - (maxHeight /* cell.height() */
                            - cell.getEffectivePaddingTop() - cell.getEffectivePaddingBottom());
                    if (fixedHeight > 0) {
                        if (cell.getHeight() > maxHeight) {
                            tly = cell.getTop() + yPos - cell.getEffectivePaddingTop();
                            bry = cell.getTop() + yPos - maxHeight + cell.getEffectivePaddingBottom();
                        }
                    }
                    if ((tly > bry || ct.zeroHeightElement()) && leftLimit < rightLimit) {
                        ct.setSimpleColumn(leftLimit, bry - 0.001f,	rightLimit, tly);
                        if (cell.getRotation() == 180) {
                            float shx = leftLimit + rightLimit;
                            float shy = yPos + yPos - maxHeight + cell.getEffectivePaddingBottom() - cell.getEffectivePaddingTop();
                            saveAndRotateCanvases(canvases, -1,0,0,-1,shx,shy);
                        }
                        try {
                            ct.go();
                        } catch (DocumentException e) {
                            throw new ExceptionConverter(e);
                        } finally {
                            if (cell.getRotation() == 180) {
                                restoreCanvases(canvases);
                            }
                        }
                    }
                }
            }
			PdfPCellEvent evt = cell.getCellEvent();
			if (evt != null) {
				Rectangle rect = new Rectangle(cell.getLeft() + xPos, cell.getTop()
						+ yPos - maxHeight, cell.getRight() + xPos, cell.getTop()
						+ yPos);
				evt.cellLayout(cell, rect, canvases);
			}
		}
	}

	/**
	 * Checks if the dimensions of the columns were calculated.
	 * 
	 * @return true if the dimensions of the columns were calculated
	 */
	public boolean isCalculated() {
		return calculated;
	}

	/**
	 * Gets the maximum height of the row (i.e. of the 'highest' cell).
	 * 
	 * @return the maximum height of the row
	 */
	public float getMaxHeights() {
		if (calculated)
			return maxHeight;
		return calculateHeights();
	}

	/**
	 * Changes the maximum height of the row (to make it higher).
	 * (added by Jin-Hsia Yang)
	 * 
	 * @param maxHeight the new maximum height
	 */
	public void setMaxHeights(float maxHeight) {
		this.maxHeight = maxHeight;
	}

	//end add

	float[] getEventWidth(float xPos) {
		int n = 0;
		for (int k = 0; k < cells.length; ++k) {
			if (cells[k] != null)
				++n;
		}
		float width[] = new float[n + 1];
		n = 0;
		width[n++] = xPos;
		for (int k = 0; k < cells.length; ++k) {
			if (cells[k] != null) {
				width[n] = width[n - 1] + cells[k].getWidth();
				++n;
			}
		}
		return width;
	}

	/**
	 * Splits a row to newHeight.
	 * The returned row is the remainder. It will return null if the newHeight
	 * was so small that only an empty row would result.
	 * 
	 * @param newHeight	the new height
	 * @return the remainder row or null if the newHeight was so small that only
	 * an empty row would result
	 */
	public PdfPRow splitRow(float newHeight) {
		PdfPCell newCells[] = new PdfPCell[cells.length];
		float fixHs[] = new float[cells.length];
		float minHs[] = new float[cells.length];
		boolean allEmpty = true;
		for (int k = 0; k < cells.length; ++k) {
			PdfPCell cell = cells[k];
			if (cell == null)
				continue;
			fixHs[k] = cell.getFixedHeight();
			minHs[k] = cell.getMinimumHeight();
			Image img = cell.getImage();
			PdfPCell newCell = new PdfPCell(cell);
			if (img != null) {
				if (newHeight > cell.getEffectivePaddingBottom() + cell.getEffectivePaddingTop() + 2) {
					newCell.setPhrase(null);
					allEmpty = false;
				}
			}
			else {
                float y;
				ColumnText ct = ColumnText.duplicate(cell.getColumn());
	            float left = cell.getLeft() + cell.getEffectivePaddingLeft();
	            float bottom = cell.getTop() + cell.getEffectivePaddingBottom() - newHeight;
	            float right = cell.getRight() - cell.getEffectivePaddingRight();
	            float top = cell.getTop() - cell.getEffectivePaddingTop();
	            switch (cell.getRotation()) {
	                case 90:
	                case 270:
	                    y = setColumn(ct, bottom, left, top, right);
	                    break;
	                default:
	                    y = setColumn(ct, left, bottom, cell.isNoWrap() ? RIGHT_LIMIT : right, top);
	                    break;
	            }
	            int status;
				try {
					status = ct.go(true);
				}
				catch (DocumentException e) {
					throw new ExceptionConverter(e);
				}
				boolean thisEmpty = (ct.getYLine() == y);
				if (thisEmpty) {
					newCell.setColumn(ColumnText.duplicate(cell.getColumn()));
					ct.setFilledWidth(0);
				}
				else if ((status & ColumnText.NO_MORE_TEXT) == 0) {
					newCell.setColumn(ct);
                    ct.setFilledWidth(0);
				}
				else
					newCell.setPhrase(null);
				allEmpty = (allEmpty && thisEmpty);
			}
			newCells[k] = newCell;
			cell.setFixedHeight(newHeight);
		}
		if (allEmpty) {
			for (int k = 0; k < cells.length; ++k) {
				PdfPCell cell = cells[k];
				if (cell == null)
					continue;
				if (fixHs[k] > 0)
					cell.setFixedHeight(fixHs[k]);
				else
					cell.setMinimumHeight(minHs[k]);;
			}
			return null;
		}
		calculateHeights();
		PdfPRow split = new PdfPRow(newCells);
		split.widths = (float[]) widths.clone();
		split.calculateHeights();
		return split;
	}
	
	/**
	 * Returns the array of cells in the row.
	 * Please be extremely careful with this method.
	 * Use the cells as read only objects.
	 * 
	 * @return	an array of cells
	 * @since	2.1.1
	 */
	public PdfPCell[] getCells() {
		return cells;
	}
}
