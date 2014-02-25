package com.web_tomorrow.utils.datetime;
import java.util.*;
import java.io.*;

/**

(c)2001 Kevin Boone/Wwb-Tomorrow
*/
public class LatLongDataElement implements Comparable
{
boolean posLat;
int dLat;
int mLat;
int sLat;
boolean posLong;
int dLong;
int mLong;
int sLong;
String id;

public String toString ()
  {
  return ("id=" + id + ", lat=" + (posLat ? "+" : "-") + dLat + ":" + mLat + ":" + sLat + ", long=" + (posLong ? "+" : "-") + dLong + ":" + mLong + ":" + sLong); 
  }

public int compareTo (Object o)
  {
  LatLongDataElement other = (LatLongDataElement)o;
  return id.compareTo(other.id);
  }

public String getId() { return id; }
public boolean getPosLat() { return posLat; }
public int getDLat() { return dLat; }
public int getMLat() { return mLat; }
public int getSLat() { return sLat; }
public boolean getPosLong() { return posLong; }
public int getDLong() { return dLong; }
public int getMLong() { return mLong; }
public int getSLong() { return sLong; }

public LatLongDataElement ()
  {
  }

public LatLongDataElement (String _id, boolean _posLat, int _dLat, int _mLat, int _sLat, 
      boolean _posLong, int _dLong, int _mLong, int _sLong)
  {
  id = _id;
  posLat = _posLat;
  dLat = _dLat;
  mLat = _mLat;
  sLat = _sLat;
  posLong = _posLong;
  dLong = _dLong;
  mLong = _mLong;
  sLong = _sLong;
  }
}

