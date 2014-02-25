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

function showHelp(event,text)
{
   window.alert(text);
}
function showHelpDiv(id,mytitle)
{
    jQuery(id).dialog(
        {
            modal: true,
            dialogClass: "helpDlg",
            closeOnEscape: true,
            width: 600,
            height: 500,
            closeText: "hide",
            draggable: true,
            resizable: true,
            show: "slow",
            title: mytitle
        }
    );
}
