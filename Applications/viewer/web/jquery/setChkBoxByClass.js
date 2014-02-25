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
 * set or clear all checkboxes in a class
 * @param {string} cl class name
 * @param {boolean} val true to check false to clear
 * @returns nothing
 */
function setChkBoxByClass(cl, val)
{
    var list = document.getElementsByClassName(cl);
    for (var i = 0; i < list.length; i++) 
        {
            list[i].checked = val;
        }
}
/**
 * Set all drop down menu (Select) to a certain value
 * @param {type} cl class
 * @param {type} idx index of item to select
 * @returns nothing
 */
function setSelectByClass(cl, idx)
{
    var nodeList = document.querySelectorAll(cl);
    for (var i = 0, length = nodeList.length; i < length; i++) 
    {
        nodeList[i].selectedIndex = idx;
    }
}
function setSelByClasses(cl1, val1, cl2, val2)
{
    setChkBoxByClass(cl1,val1);
    setSelectByClass(cl2,val2);
}
/**
 * Add a hidden field to a form
 * @param {string} frm name of the form
 * @param {string} name name of the field
 * @param {string} val value of the field
 * @returns {undefined} nothing
 */
function addHidden(frm, name, val)
{
    
    var input = document.createElement("input");

    input.setAttribute("type", "hidden");

    input.setAttribute("name", name);

    input.setAttribute("value", val);

    //append to form element 
    frmEle = document.getElementById(frm);
    if (frmEle)
        {
            frmEle.appendChild(input);
        }
}
function historySubmit(act,ele)
{
    frm=ele.form;
    frmId = frm.id;
    addHidden(frmId,'submitAct', act);
    
    frmEle = document.getElementById(frmId);
    frmEle.submit();
}
