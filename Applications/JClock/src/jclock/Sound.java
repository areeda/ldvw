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
package jclock;

import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Sound
{

    public static synchronized void playSound(final String url)
    {

        try
        {
            Clip clip;
            clip = AudioSystem.getClip();

            URL viaClass = Sound.class.getResource(url);

            AudioInputStream inputStream = AudioSystem.getAudioInputStream(viaClass);
            clip.open(inputStream);
            clip.addLineListener(new CloseClipWhenDone());
            FloatControl gainControl =
                         (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f); // Reduce volume by 10 decibels.
            clip.setFramePosition(0);
            clip.start();
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
        }
    }

    private static class CloseClipWhenDone implements LineListener
    {

        @Override
        public void update(LineEvent event)
        {
            if (event.getType().equals(LineEvent.Type.STOP))
            {
                Line soundClip = event.getLine();
                soundClip.close();

                //Just to prove that it is called...  
                System.out.println("Done playing " + soundClip.toString());
            }
        }
    }
}