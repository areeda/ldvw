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

/**
 * Allows a section of the page to be hidden by default and displayed on command
 * To use set the Display attribute in CSS to 'none' and add a button that calls this function
 * 
 * @param {type} className class of hidden tags
 * @param {type} obj the tag that calls this function will be hidden when items are shown
 * @returns {undefined} none
 */
function showByClass(className,obj)
{
    var theElements = document.getElementsByClassName(className);
    for (var i = 0; i < theElements.length; ++i) 
    {
        var item = theElements[i];
        item.style.display='';
    }  
    obj.style.display='none';
}
/**
 * Show hide a element based on its id.  The element (obj) should have text eithe Show or Hide
 * and it will change the verb to the other (hopefully appropriate one)
 * @param {type} idName id of the object to show/hide
 * @param {type} btnName the jQuery selector (#<element id>) that calls us containing the word Show or Hide
 * @returns {undefined}
 */
function toggleShowById(idName, btnName)
{
    var hash = '#';
    var elem = jQuery(hash.concat(idName));
    var btnTxt = jQuery(btnName).text();
    if( elem.is(":visible") )
    {
        elem.hide();
        btnTxt = btnTxt.replace("Hide", "Show");
        jQuery(btnName).text(btnTxt);
    }
    else
    {
        elem.show();
        btnTxt = btnTxt.replace("Show", "Hide");
        jQuery(btnName).text(btnTxt);
    }
}

function showById(idName)
{
    var elem = document.getElementById(idName);
    elem.style.display='';
}

function hideById(idName)
{
    var elem = document.getElementById(idName);
    elem.style.display='none';
}