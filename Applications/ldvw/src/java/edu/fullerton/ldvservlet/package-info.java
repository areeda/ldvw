/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * The user callable servlets that make up LigoDV-web.  In version 0.1.* we used a single scriptlet
 * demonstrating the author's lack of experience.  Moving to servlets increased performance and 
 * encapsulation.
 * 
 * As a long term more commands are being moved from the dispatcher in the main servlet to individual
 * servlets.  
 * 
 * If you write a servlet much of the global User Interface is implemented in the ServletSupport POJO
 * Also remember a servlet must  be thread safe, so pretty much no non-final fields in the class.
 * @since 0.2.0
 */
package edu.fullerton.ldvservlet;
