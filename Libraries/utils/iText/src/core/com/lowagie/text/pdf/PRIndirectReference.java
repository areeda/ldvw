/*
 * $Id: PRIndirectReference.java 3721 2009-02-24 20:32:32Z mstorer $
 *
 * Copyright 2001, 2002 by Paulo Soares.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

public class PRIndirectReference extends PdfIndirectReference {

    protected PdfReader reader;
    // membervariables
    /**
     * Used when 'reader' is "non-partial", holding the entire PDF in memory.
     * Otherwise, we stick to PdfIndirectReference's "reffedObj".
     * @since 2.1.5
     */
    protected PdfObject hardReference;

    // constructors

/**
 * Constructs a <CODE>PdfIndirectReference</CODE>.
 *
 * @param		reader			a <CODE>PdfReader</CODE>
 * @param		number			the object number.
 * @param		generation		the generation number.
 */

    PRIndirectReference(PdfReader reader, int number, int generation) {
        type = INDIRECT;
        this.number = number;
        this.generation = generation;
        this.reader = reader;
    }

/**
 * Constructs a <CODE>PdfIndirectReference</CODE>.
 *
 * @param		reader			a <CODE>PdfReader</CODE>
 * @param		number			the object number.
 */

    PRIndirectReference(PdfReader reader, int number) {
        this(reader, number, 0);
    }

    // methods

    public void toPdf(PdfWriter writer, OutputStream os) throws IOException {
        int n = writer.getNewObjectNumber(reader, number, generation);
        os.write(PdfEncodings.convertToBytes(new StringBuffer().append(n).append(" 0 R").toString(), null));
    }

    public PdfReader getReader() {
        return reader;
    }

    public void setNumber(int number, int generation) {
        this.number = number;
        this.generation = generation;
        // blow out any previous internal object storage.
        hardReference = null;
        reffedObj = null;
    }

    /**
     * Find the direct object for this reference.  It'll look up
     * the correct one from its <code>PdfReader</code> if need be, but prefers to use
     * the internally stored <code>PdfObject</code>.
     * @return the direct object for this reference.
     * @since 2.1.5
     */
    public PdfObject getDirectObject() {
        PdfObject dirObj = getInternalObject();
        if (dirObj == null) {
            dirObj = reader.getPdfObject(number);
            if (reader.isPartial()) {
                // weak references are ignored for purposes of GC
                reffedObj = new WeakReference(dirObj);
                reader.releaseLastXrefPartial();
                hardReference = null;
            }
            else {
                reffedObj = null;
                hardReference = dirObj;
            }
        }

        return dirObj;
    }

    /**
     * Sorts out the current reference from either of the two
     * places it could be stored.
     * @return a valid object reference if there's one to get
     * @since 2.1.5
     */
    private PdfObject getInternalObject() {
        if (hardReference == null && (reffedObj == null || reffedObj.get() == null)) {
            return null;
        }

        return (hardReference != null) ? hardReference : (PdfObject) reffedObj.get();
    }

    /**
     * Block alteration of a PRIndRef's direct object.
     * @param obj ignored
     * @since 2.1.5
     */
    public void setDirectObject( PdfObject obj) {
    }
}