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
package edu.fullerton.ldvservlet;

import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemBSNavBar;
import edu.fullerton.jspWebUtils.WebUtilException;

/**
 * Construct the LDVW navigation bar (commands)
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NavBar
{

    private String mainHelpUrl;
    // Top row commands 2 element rows "Text", "get parameters" or "http*://"
    private final String[][] commands =
    {
        {
            "Home", "basechan"
        }
    };
    private final String[][] historyMenu =
    {
        {
            "Saved plots (mine)", "ImageHistory&amp;size=med"
        },
        {
            "Saved plots (all)", "ImageHistory&amp;size=med&amp;usrSel=All"
        },
        {
            "Upload", "upload"
        }
    };
    private final String[][] statusMenu =
    {
        {
            "Channel stats", "ChannelStats"
        },
        {
            "NDS status", "ndsStatus"
        },
    };
    private final String[][] helpMenu =
    {
        {
            "Help", mainHelpUrl
        },
        {
            "Contact us", "contactUs"
        },
        {
            "LDVW paper", "https://arxiv.org/abs/1611.01089"
        },
        {
            "Release notes", "https://ligodv.areeda.com/ligodv/dokuwiki/doku.php?id=ldvrelnotes"
        },
        {
            "gwpy-plot docs", "https://ligodv.areeda.com/gwpy/cli/index.html"
        },
        {
            "gwpy-info", "gwpy-info"
        }
            
    };
    // Commands only available to admin group
    private final String[][] adminCommands =
    {
        {
            "User stats", "Stats"
        },
        {
            "Edit help", "EditHelp"
        },
        {
            "Enter/exit maintance mode", "/MaintMode"
        },
        {
            "DB stats", "dbstats"
        },
        {
            "Servers", "serverManager"
        }
    };
    // commands available to admins or testers
    private final String[][] experimentalCommands =
    {

    };

    private boolean isAdmin;
    private boolean isTester;


    //-------------

    enum AUTH
    {

        anyone, tester, admin
    };

    private class Menu
    {

        public String title;
        public String[][] menuItems;
        public AUTH auth;

        Menu(String title, String[][] menuItems, AUTH auth)
        {
            this.title = title;
            this.menuItems = menuItems;
            this.auth = auth;
        }
    }

    Menu[] menus =
    {
        new Menu("", commands, AUTH.anyone),
        new Menu("History", historyMenu, AUTH.anyone),
        new Menu("Status", statusMenu, AUTH.anyone),
        new Menu("Help", helpMenu, AUTH.anyone),
        new Menu("Admin", adminCommands, AUTH.admin)
    };
    PageItem navBar;

    PageItem getNavBar(String contextPath, boolean isTester, boolean isAdmin) throws WebUtilException
    {
        this.isTester = isTester;
        this.isAdmin = isAdmin;
        PageItemBSNavBar bsnav = new PageItemBSNavBar();
        String baseUrl = contextPath + "/view?act=";
        String cmdUrl;

        for (Menu menu : menus)
        {
            if (menu.title.isEmpty() && checkAuth(menu.auth))
            {
                if (menu.menuItems.length != 1)
                {
                    throw new WebUtilException("Creatng nav bar: link entry has multiple commands");
                }
                String[] command = menu.menuItems[0];
                // Add single command to nav bar as link
                if (command[1].matches("^http.?://.*"))
                {
                    bsnav.addLink(command[1], command[0], "_blank");
                }
                else
                {
                    cmdUrl = baseUrl + command[1];
                    bsnav.addLink(cmdUrl, command[0]);
                }

            }
            if (!menu.title.isEmpty() && checkAuth(menu.auth))
            {
                bsnav.createNewSubmenu(menu.title);

                for (String[] command : menu.menuItems)
                {
                    if (command[1].matches("^http.?://.*"))
                    {
                        bsnav.addSubmenuLink(command[1], command[0], "_blank");
                    }
                    else if (command[1].startsWith("/"))
                    {
                        bsnav.addSubmenuLink(contextPath + command[1], command[0]);
                    }
                    else
                    {
                        cmdUrl = baseUrl + command[1];
                        bsnav.addSubmenuLink(cmdUrl, command[0]);
                    }
                }
                bsnav.addCurSubmenu();
            }

        }
        return bsnav;
    }
    /**
     * Using object's fields verify user is authorized for this command
     * @param auth - needed authorization level
     * @return 
     */
    private boolean checkAuth(AUTH auth) throws WebUtilException
    {
        boolean ret;
        switch (auth)
        {
            case admin:
                ret = isAdmin;
                break;
            case anyone:
                ret = true;
                break;
            case tester:
                ret = isAdmin || isTester;
                break;
            default:
                throw new WebUtilException("Creating navbar: unknow authorization for menu command");
        }
        return ret;
    }
    /**
     * For dynamic elements like help we set the link or command
     * 
     * @param command - the text string that appears on the nav bar
     * @param link - what to do if they choose it
     */
    public void setLink(String command, String link)
    {
        for (Menu menu : menus)
        {
            for(int i=0;i<menu.menuItems.length;i++)
            {
                if (menu.menuItems[i][0].equalsIgnoreCase(command))
                {
                    menu.menuItems[i][1] = link;
                }
            }
        }
    }
}
