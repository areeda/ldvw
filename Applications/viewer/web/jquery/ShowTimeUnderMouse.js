/* 
 * Copyright (C) 2013 Joseph Areeda <joseph.areeda at ligo.org>
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

/* blobal variables */
var topLoc = new Array();
var left = new Array();
var width = new Array();
var height = new Array();
var startGps = new Array();
var duration = new Array();
var xScale = new Array();
var imgId = new Array();
var timId = new Array();

/**
 * Initialize the time labeling variables and binds the click
 * @returns {undefined}
 */
function initTimeFactor(cls)
{
    
    
    
    jQuery(cls).bind('click', function(ev) 
    {
        var tdiv = jQuery(ev.target);
        var id = ev.target.id;          /* id of element receiving the click */
        var eleIdx = imgId.indexOf(id);
        
        if (eleIdx >=0)
        {
            var offset = tdiv.offset();
            var ox = offset.left;
            var oy = offset.top;
            var cx = ev.clientX;
            var cy = ev.clientY;
            var px = ev.pageX;
            var py = ev.pageY;
            var ix = Math.floor(px - ox);
            var iy = Math.floor(py - oy);
            
            var x = ix - left[eleIdx];
            var y = iy - topLoc[eleIdx];
            var timeStr = '?';
            var w = width[eleIdx];
            var h = height[eleIdx];

            if (x >= 0 && x < w && y >= 0 && y < h)
            {
                var t = x * xScale[eleIdx] + startGps[eleIdx] + 0.05;
                timeStr = t.toFixed(1);
            }
            var myEle = timId[eleIdx];
            var posStr = '';
            /* 
            posStr = posStr + 'cpos: (' + cx + ', '+ cy + '). Pos: (' + x + ', ' + y + '). ';
            posStr = posStr + ' ipos: (' + ix + ', ' + iy + '). ';
            posStr = posStr + ' opos: (' + ox + ', ' + oy + '). ';
            */
            jQuery(myEle).html(posStr + '  Time: '+ timeStr);
        }
    });
}
/**
 * For each image we're to find the time, call this to set factors for click operation
 * 
 * @param {int} x0 - relative position of left edge of area with plot
 * @param {int} y0 - relative position of top edge of plot area
 * @param {int} w - width of plot area
 * @param {int} h - height of plot area
 * @param {int} strt - gps start time
 * @param {int} dur - duration in seconds
 * @param {string} imgid - element id of the image
 * @param {string} timid - element id of text to replace with time
 * @returns {undefined}
 */
function setTimeFactor(x0, y0, w, h, strt, dur, imgid, timid)
{
    var idx = imgId.length;

    topLoc[idx] = y0;
    left[idx]  = x0;
    width[idx]  = w;
    height[idx]  = h;
    startGps[idx]  = strt;
    duration[idx]  = dur;
    xScale[idx]  = dur / w;
    imgId[idx] = imgid;
    timId[idx] = timid;
}
