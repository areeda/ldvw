package mozilla.thunderbird;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single address in a Mozilla Thunderbird address book.
 *
 * The following list of properties is known in Thunderbird 1.5.0.9
 * <ul>
 * <li>FirstName</li>
 * <li>LastName</li>
 * <li>PrimaryEmail</li>
 * <li>DisplayName</li>
 * <li>Company</li>
 *
 * <li>ListName</li>
 * <li>ListNickName</li>
 * <li>ListDescription</li>
 * <li>ListTotalAddresses</li>
 * <li>LowercaseListName</li>
 * <li>ns:addrbk:db:table:kind:deleted</li>
 * <li>ns:addrbk:db:row:scope:card:all</li>
 * <li>ns:addrbk:db:row:scope:list:all</li>
 * <li>ns:addrbk:db:row:scope:data:all</li>
 * <li>PhoneticFirstName</li>
 * <li>PhoneticLastName</li>
 * <li>NickName</li>
 * <li>LowercasePrimaryEmail</li>
 * <li>SecondEmail</li>
 * <li>DefaultEmail</li>
 * <li>CardType</li>
 * <li>PreferMailFormat</li>
 * <li>PopularityIndex</li>
 * <li>WorkPhone</li>
 * <li>HomePhone</li>
 * <li>FaxNumber</li>
 * <li>PagerNumber</li>
 * <li>CellularNumber</li>
 * <li>WorkPhoneType</li>
 * <li>HomePhoneType</li>
 * <li>FaxNumberType</li>
 * <li>PagerNumberType</li>
 * <li>CellularNumberType</li>
 * <li>HomeAddress</li>
 * <li>HomeAddress2</li>
 * <li>HomeCity</li>
 * <li>HomeState</li>
 * <li>HomeZipCode</li>
 * <li>HomeCountry</li>
 * <li>WorkAddress</li>
 * <li>WorkAddress2</li>
 * <li>WorkCity</li>
 * <li>WorkState</li>
 * <li>WorkZipCode</li>
 * <li>WorkCountry</li>
 * <li>JobTitle</li>
 * <li>Department</li>
 * <li>_AimScreenName</li>
 * <li>AnniversaryYear</li>
 * <li>AnniversaryMonth</li>
 * <li>AnniversaryDay</li>
 * <li>SpouseName</li>
 * <li>FamilyName</li>
 * <li>DefaultAddress</li>
 * <li>Category</li>
 * <li>WebPage1</li>
 * <li>WebPage2</li>
 * <li>BirthYear</li>
 * <li>BirthMonth</li>
 * <li>BirthDay</li>
 * </ul>
 *
 * @author mhaller
 */
public class Address
{

    private final Map<String, String> values;

    public Address(Map<String, String> values)
    {
        this.values = values;
    }

    public HashMap<String, String> getValues()
    {
        HashMap<String, String> ret = new HashMap<String, String>();
        ret.putAll(values);
        return ret;
    }

    public String getFirstName()
    {
        return values.get("FirstName");
    }

    public String getPrimaryEmail()
    {
        return values.get("PrimaryEmail");
    }

    public String getLastName()
    {
        return values.get("LastName");
    }

    public String getDisplayName()
    {
        return values.get("DisplayName");
    }

    public String getCompany()
    {
        return values.get("Company");
    }
}
