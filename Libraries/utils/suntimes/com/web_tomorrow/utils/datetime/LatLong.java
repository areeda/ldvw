package com.web_tomorrow.utils.datetime;
import java.util.*;
import java.io.*;

/**
LatLong contains methods for manipulating latitude/longitude
elements.

(c)2001 Kevin Boone/Wwb-Tomorrow
*/
public class LatLong 
{
/**
Gets a data element by the specified zone ID (e.g., Europe/London)
*/
public static LatLongDataElement getLatLongDataElement (String zoneId)
  {
  LatLongDataElement[] lld = LatLongData.elements;
  for (int i = 0; i < lld.length; i++)
    {
    LatLongDataElement llde = lld[i];
    if (zoneId.equalsIgnoreCase (llde.getId())) return llde;
    } 
  return null;
  }

}
