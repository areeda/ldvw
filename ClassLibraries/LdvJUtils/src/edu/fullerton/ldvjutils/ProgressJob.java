/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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
package edu.fullerton.ldvjutils;

/**
 * The thread that contains the progress frame's event loop
 * It is a separate class so we can pass a reference to the object the the worker thread
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class ProgressJob implements Runnable
{
    private ProgressFrame progressFrame;

    public ProgressFrame getProgressFrame()
    {
        return progressFrame;
    }
    
    
    @Override
    public void run()
    {
        createAndShowGUI();
    }

    private void createAndShowGUI()
    {
        progressFrame = new ProgressFrame();
    }  
}
