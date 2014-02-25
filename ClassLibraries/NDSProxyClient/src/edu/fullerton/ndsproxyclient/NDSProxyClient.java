/*
 * Copyright (C) 2012 Joseph Areeda<joseph.areeda@ligo.org>
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

package edu.fullerton.ndsproxyclient;


import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * a Class to communicate with the Proxy Server and control NDS1 and NDS2 transactions
 * 
 * @author joe areeda
 */
public class NDSProxyClient
{
    private String server;
    private String proxy;
    private String channel;
    private Socket requestSocket=null;
    private OutputStream out;
    private InputStream ins;
    private InputStreamReader in;
    private DataInputStream dis;
    
    private String lastError="";
    private String lastCommand="";
    private boolean verbose=false;
    
    private double[] vals;
    private int vPtr, vSize;
    private long vStartGPS;
    private int dt;
   
    private File tmpChanFile;
    private FileOutputStream tmpChanOStream;
    private BufferedInputStream tmpChanIStream;
    
    // to record NDS transfer times
    private long totalMs;
    private long startMs;
    private long nBytes;
    
    
    /**
     * Create the client object and specify the server.  The client can span any number of connections and data transfers.
     * @param serv may be a URI or a numeric IP address
     */
    public NDSProxyClient(String serv)
    {
        server =serv;
        proxy = "localhost";
        vPtr=vSize=0;
        dt = 8;
        vals = null;
        tmpChanFile = null;
        tmpChanOStream = null;
        tmpChanIStream = null;
        
        startMs = 0;
        totalMs = 0;
        nBytes = 0;
    }

    /**
     * explicitly close the socket on garbage collection.  While we can't rely on this it may help
     * reduce the amount of resources we keep open.
     * 
     * @throws Throwable 
     */
    @Override
    public void finalize() throws Throwable
    {
        super.finalize();
        if (requestSocket != null)
        {
            requestSocket.shutdownInput();
            requestSocket.shutdownOutput();
            requestSocket.close();
            requestSocket = null;
        }
    }
    /**
     * After the data request has been made this will manage buffering and get the next value from
     * the server as a Double regardless of transfer data format
     * 
     * @return the next data point or throws an exception
     * @throws NDSException 
     */
    public Double getNextDouble() throws NDSException
    {
        Double it=0.;
        if (vPtr >= vSize || vals==null)
        {
            getNextBufBin();
        }
        if (vPtr < vSize)
        {
            it = vals[vPtr];
            vPtr++;
        }
        
        return it;
    }
    /**
     * After the data request has been made this will manage buffering and get the next value from
     * the server as a long regardless of transfer data format
     *
     * @return the next data point or throws an exception
     * @throws NDSException
     * 
     * @todo make the conversion on the server side or get data in the raw format, current method has 2 conversions
     */
    public long getNextLong() throws NDSException
    {
        long it = 0L;
        if (vPtr >= vSize || vals == null)
        {
            getNextBufBin();
        }
        if (vPtr < vSize)
        {
            it = (long)vals[vPtr];
            vPtr++;
        }

        return it;
    }
    /**
     * get the hostname of the proxy server
     * 
     * @return host name or null indicating loopback interface
     */
    public String getProxy()
    {
        return proxy;
    }

    /**
     * Set the host for the proxy connection
     * 
     * @param proxy host name, may be null indicating loopback interface
     */
    public void setProxy(String proxy)
    {
        this.proxy = proxy;
    }

    /**
     * Establish a connection with the proxy and ask it to open a connection to the server
     * The default proxy is local host, and the server is set in the constructor
     * @return true if the connections suscceded.
     * @throws NDSException 
     */
    public boolean connect() throws NDSException
    {
        return connect(60000);
    }
    public boolean connect(int timeout) throws NDSException
    {
        startMs = System.currentTimeMillis();
        boolean ret = false;
        try
        {
            if (requestSocket == null)
            {
                requestSocket = new Socket(proxy, 31300);
                requestSocket.setSoTimeout(timeout);
                if (verbose)
                {
                    System.out.println("Connected to localhost on port 31300");
                }
                //2. get Input and Output streams
                out = requestSocket.getOutputStream();
                out.flush();
                ins = requestSocket.getInputStream();
                in = new InputStreamReader(ins);
                dis = new DataInputStream(ins);
            }
            String conStr = "conn," + server + "\n";
            sendCmd(conStr);
            String line;
            boolean done = false;
            do
            {
                line=readLine();
                if (line != null && line.contains("OK"))
                {
                    if (verbose)
                    {
                        System.out.println("Connected");
                    }
                    done = true;
                    ret = true;
                }
                else if (line != null && line.toLowerCase().contains("error"))
                {
                    lastError = line;
                    throw new NDSException("Error, can't connect to " + server + " " + lastError);
                }
            } while(!done);
            
        }
        catch (IOException | NDSException ex)
        {
            lastError = ex.getClass().getSimpleName() + " - " +ex.getLocalizedMessage();
            out = null;
            updateTiming();
            throw new NDSException(ex);
        }
        updateTiming();
        return ret;
    }
    /**
     * Send the command to the proxy server to disconnect from the nds server.
     * Our connection to proxy server remains open
     * @throws NDSException possible reasons are the connection was closed
     */
    public void disconnect() throws NDSException
    {
        try
        {
            if (out != null)
            {
                sendCmd("DCON");
                Thread.sleep(750);
                flushInput();
            }
        }
        catch (Exception ex)
        {
            lastError = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            lastError = "Error on disconnect from " + server + " " + lastError;
            throw (new NDSException(lastError));
        }
    }
    /**
     * Send the command to close the channel to the proxy server
     * @throws NDSException 
     */
    public void bye()throws NDSException
    {
        try
        {
            disconnect();
        }
        catch (Exception ex)
        {
            
        }
        try
        {
            flushInput();

            if (out != null)
            {
                sendCmd("BYE");
                String line = readLine();
                if (line != null && line.toLowerCase().contains("error"))
                {
                    throw new NDSException(line);
                }
                out.close();
            }
            out = null;
            if (requestSocket != null)
            {
                requestSocket.close();
            }
        }
        catch (Exception ex)
        {
            if (requestSocket != null)
            {
                try
                {
                    requestSocket.shutdownInput();
                    requestSocket.shutdownOutput();
                }
                catch (Exception ex1)
                {
                    // errors in the error handler get ignored
                }
            }
        }
        
    }

    /**
     * Set up a data transfer.  You will have to call one of the getNext functions to actually 
     * transfer data.
     * @param chan the exact name of the channel
     * @param cType string representation of channel type must be unknown, raw, online, rds, second-trend, minute-trend, test-point or static
     * @param start gps start time or 0 for online data
     * @param stop stop time or 0 for online data
     * @param stride how many seconds of data to get in each buffer from the server
     * @return true if we have data available
     * @throws NDSException possible reasons are the connection was closed or a network error
     */
    public boolean requestData(String chan,String cType, long start, long stop, int stride)
    {
        startMs = System.currentTimeMillis();
        boolean ret;
        channel = chan;
        String req = "DATA," + channel + "," + cType + String.format(",%1$d,%2$d,%3$d",start,stop, stride) + "\n";
        try
        {
            vStartGPS = start;
            sendCmd(req);
            ret = true;
        }
        catch (NDSException ex)
        {
            lastError = String.format("Error: requesing data for %1$s from %2$s at %3$d - %4$s", 
                                         channel, server, start,
                                         ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
            Throwable cause = ex.getCause();
            if (cause != null)
            {
                lastError += " - Cause: " + cause.getLocalizedMessage();
            }
            //System.out.println(lastError);
            ret = false;
        }
        updateTiming();
        return ret;
    }
    public int getChanCount() throws NDSException
    {
        return getChanCount("unknown");
    }
    public int getChanCount(String cType) throws NDSException
    {
        int ret =0;
        String cmd = "CHCT";
        if (cType != null && cType.length() > 0)
        {
            cmd += "," + cType;
        }
        sendCmd(cmd);

        String[] nextLine;

        nextLine = csvReadNext();
        if (nextLine == null || nextLine.length < 1)
        {
            throw new NDSException("failed to read next buffer.");
        }
        else if (nextLine[0].toLowerCase().startsWith("error"))
        {
            throw new NDSException(nextLine[0]);
        }
        else if (nextLine[0].toLowerCase().contains("success"))
        {
            nextLine = csvReadNext();
        }
        
        if (nextLine[0].trim().matches("^\\d+$"))
        {
            ret = Integer.parseInt(nextLine[0].trim());
        }
        else
        {
            throw new NDSException("Unable to parse channel count.");
        }
        return ret;
    }
    /**
     * Get a list of all channels of the specified type
     * 
     * @param cType channel type as a string must be unknown, raw, online, rds, second-trend,
     * minute-trend, test-point or static.  Unknown will return all channels
     * 
     * @return a potentially long list of ChanInfo objects
     * @throws NDSException a command error or a network error
     */
    public ArrayList<ChanInfo> getChanList(String cType) throws NDSException, LdvTableException
    {
        ArrayList<ChanInfo> ret = new ArrayList<ChanInfo>();
        String cmd = "CHLS";
        if (cType != null && cType.length() > 0)
        {
            cmd += "," + cType;
        }
        sendCmd(cmd);

        String[] nextLine;
        boolean gotMore=true;

        nextLine = csvReadNext();
        if (nextLine == null || nextLine.length < 1)
        {
            throw new NDSException("failed to read next buffer.");
        }
        else if (nextLine[0].toLowerCase().startsWith("error"))
        {
            throw new NDSException(nextLine[0]);
        }
        do
        {
            if (nextLine.length == 1)
            {
                if (nextLine[0].equalsIgnoreCase("OK"))
                {
                    break;
                }
            }
            else if (nextLine[0].toLowerCase().startsWith("#")) {
                // column titles is one type of comment
                //@todo verify columns are what we expect
            }
            else if (nextLine.length > 7)
            {
                ChanInfo cInfo = new ChanInfo();
                cInfo.fill(nextLine);
                ret.add(cInfo);
            }

        } while ((nextLine = csvReadNext()) != null && gotMore);

        return ret;

    }
     /**
     * Get a list of all channels of the specified type and write to temporary file
     * 
     * @param cType channel type as a string must be unknown, raw, online, rds, second-trend,
     * minute-trend, test-point or static.  Unknown will return all channels
     * 
     * 
     * @throws NDSException a command error or a network error
     */
    public void getBufferedChanList(String cType) throws NDSException, LdvTableException, IOException
    {
        startMs = System.currentTimeMillis();
        tmpChanIStream = null;      // in case we error out a null list
        String cmd = "CHLS";
        if (cType != null && cType.length() > 0)
        {
            cmd += "," + cType;
        }
        sendCmd(cmd);

        String nextLine;
        boolean gotMore=true;

        nextLine = readLine();
        if (nextLine == null || nextLine.isEmpty())
        {
            updateTiming();
            throw new NDSException("failed to read chls results.");
        }
        else if (nextLine.toLowerCase().startsWith("error"))
        {
            updateTiming();
            throw new NDSException(nextLine);
        }

        do
        {
            if (nextLine.toLowerCase().startsWith("ok"))
            {
                break;  
            }
            else if (nextLine.toLowerCase().startsWith("#")) 
            {
                // column titles is one type of comment
                //@todo verify columns are what we expect
            }
            else
            {
                if (tmpChanOStream == null)
                {
                    tmpChanFile = File.createTempFile("ndsChan_", ".tmp");
                    tmpChanOStream = new FileOutputStream(tmpChanFile);
                }
                
                tmpChanOStream.write(nextLine.getBytes());
                tmpChanOStream.write('\n');
            }

        }while ((nextLine = readLine()) != null && gotMore);

        if (tmpChanOStream != null)
        {
            tmpChanOStream.close();
            tmpChanOStream = null;
            tmpChanIStream = new BufferedInputStream(new FileInputStream(tmpChanFile));
        }
        updateTiming();
    }
    /**
     * After a getBufferedChanList call this returns channels one at a time
     * @return next channel in list
     */
    public ChanInfo getNextChannel() throws LdvTableException
    {
        startMs = System.currentTimeMillis();
        ChanInfo ret = null;
        if (tmpChanIStream != null)
        {
            try
            {
                String line = readChanTempLine();
                if (!line.contains("eof"))
                {
                    String[] fields = csvParse(line);
                    ret = new ChanInfo();
                    ret.fill(fields);
                }
            }
            catch (NDSException ex)
            {
                try
                {
                    // end of file probably. just return null;
                    tmpChanIStream.close();
                }
                catch (IOException ex1)
                {
                    // don't care
                }
                tmpChanIStream = null;
            }
        }
        updateTiming();
        return ret;
    }
    public String getChannelSourceInfo(String[] channelNames) throws NDSException
    {
        startMs = System.currentTimeMillis();
        String ret = "";
        if (channelNames.length > 0)
        {
            String cmd = "";
            for(String c : channelNames)
            {
                cmd += cmd.length() > 0 ? "," : "";
                cmd += c;
            }
            if (cmd.length() > 0)
            {
                try
                {
                    sendCmd("ATIM," + cmd);
                }
                catch(NDSException ex)
                {
                    lastError = ex.getLocalizedMessage();
                    updateTiming();
                    throw ex;
                }
                String line = readLine();
                if (line == null || line.toLowerCase().startsWith("error"))
                {
                    lastError = line == null ? "EOF" : line;
                    updateTiming();
                    throw new NDSException(line);
                }
                
                while (line != null && !line.toLowerCase().startsWith("ok"))
                {
                    ret += line + "\n";
                    line = readLine();
                }
            }        
        }
        updateTiming();
        return ret;
    }

    /**
     * get the details of last error detected
     * @return error message
     */
    public String getLastError()
    {
        return lastError;
    }

    //=========================================================================
    /**
     * Send a string as the command to the proxy server
     * @param cmd full command line
     * @throws NDSException either a command error or network error talking to the proxy server
     */
    private void sendCmd(String cmd) throws NDSException
    {
        long start = System.currentTimeMillis();
        lastCommand = cmd;
        if (out == null)
        {
            throw new NDSException("Attempt to send a command to a connection that has been closed.");
        }
        if (verbose)
        {
            System.out.println("About to send command: "+cmd);
        }
        try
        {
            String line;
            if (cmd != null && cmd.length() > 0 && out != null)
            {
                String mycmd = cmd;
                if (!mycmd.endsWith("\n"))
                {
                    mycmd += '\n';
                }
                out.write(mycmd.getBytes("US-ASCII"));

            }
            else
            {
                throw new NDSException("sendCmd called with null command.");
            }
            out.flush();
            if (verbose)
            {
                System.out.println("sent, about to read response.");
            }
            
            line = readLine();
            if (line == null || line.toLowerCase().contains("error"))
            {
                line = line == null ? "EOF" : line;
                throw new NDSException(line);
            }
        }
        catch (IOException ex)
        {
            throw new NDSException(ex);
        }
        if (verbose)
        {
            float elap = (System.currentTimeMillis()-start)/1000.f;
            System.out.println(String.format("Command succeeded. %1$.3f sec",elap));
        }

    }

    private void getNextBuf() throws NDSException
    {
        startMs = System.currentTimeMillis();
        if (verbose)
        {
            System.out.println("request next ascii buffer.");
        }
        sendCmd("next,alpha\n");
        String [] nextLine;

        nextLine = csvReadNext();
        if (nextLine == null || nextLine.length < 1)
        {
            throw new NDSException("failed to read next buffer.");
        }
        else if (nextLine[0].toLowerCase().startsWith("error") || nextLine[0].toLowerCase().startsWith("> error"))
        {
            throw new NDSException(nextLine[0]);
        }
        else if (nextLine[0].toLowerCase().startsWith("ok") || nextLine[0].toLowerCase().startsWith("> ok"))
        {
            nextLine = csvReadNext();     // good, ignore that and get next
        }
        if (nextLine.length != 2)
        {
            throw new NDSException("Invalid data buffer format, for number of arguments for data buffer start.");
        }
        String ss = nextLine[0];
        if (ss.startsWith(">"))
        {
            ss=ss.substring(1).trim();
        }
        vSize = Integer.parseInt(ss);
        vStartGPS = Long.parseLong(nextLine[1].trim());
        if (vals == null || vals.length != vSize)
        {
            vals = new double[vSize];
        }
        vPtr =0;
        while(vPtr < vSize)
        {
            nextLine = csvReadNext();
            for(String vstr : nextLine)
            {
                vstr = vstr.trim();
                try
                {
                    vals[vPtr] = Double.parseDouble(vstr);
                }
                catch (NumberFormatException ex)
                {
                    System.err.println("Double format exception (" + vstr + ")");
                    vals[vPtr] = 0.;
                }
                vPtr++;
            }
        }
        vPtr=0;
        if (verbose)
        {
            System.out.println("got it.");
        }
        updateTiming();
    }
    
    private void getNextBufBin() throws NDSException
    {
        startMs = System.currentTimeMillis();
        if (verbose)
        {
            System.out.println("request next binary buffer.");
        }
        sendCmd("next,binary\n");
        try
        {
            vStartGPS = dis.readLong();
            vSize = dis.readInt();
            if (vals == null || vals.length != vSize)
            {
                vals = new double[vSize];
            }
            for(int i=0;i<vSize;i++)
            {
                vals[i] = dis.readDouble();
            }
            vPtr=0;
        }
        catch (IOException ex)
        {
            String ermsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            updateTiming();
            throw new NDSException(ermsg);
        }
        if (verbose)
        {
            System.out.println("got it.");
        }
        updateTiming();
    }
   /**
    * read next line from server
    * @return string containing next line
    * @throws NDSException closed connection or network error
    */
    private String readLine() throws NDSException
    {
        String ret = "";
        try
        {
            byte c = dis.readByte();
            while (c != '\n' )
            {
                ret += (char)c;
                c = dis.readByte();
            }
        }
        catch(EOFException eofex)
        {
            ret = null;
        }
        catch (IOException ex)
        {
            String ermsg = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            throw new NDSException(ermsg + ". Reading line from proxy server, last command: " + lastCommand);
        }
        
        
       
        return ret;   
    }
    /**
     * read next line from current, open, channel temporary file
     * @return string containing next line
     * @throws NDSException tried to read past end of line or read error
     */
    private String readChanTempLine() throws NDSException
    {
        StringBuilder ret = new StringBuilder();
        int c = 0;
        while (c != '\n' && c != -1)
        {
            try
            {
                c = tmpChanIStream.read();
                if (c != -1)
                {
                    char cc = (char) c;
                    ret.append(cc);
                }
            }
            catch (IOException ex)
            {
                throw new NDSException(ex);
            }
        }
        if (ret.length() == 0 && c == -1)
        {
            ret.append("eof\n");
        }
        return ret.toString();
    }
    /**
     * get start time of data in the last buffer
     * @return the starting GPS time 
     */
    public long getStartGPS()
    {
        return vStartGPS;
    }

    /**
     * @see NDSClient#setVerbose(boolean) 
     * @return current verbosity setting
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Set verbosity level to debugging or errors only
     * 
     * @param verbose if true print a lot of stuff to stdout
     */
    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * Get the next iline from the network connection and break on comma's into strings
     * @return each field as a separate string
     */
    private String[] csvReadNext() throws NDSException
    {
        String line = readLine();
        if (line == null)
        {
            return new String[0];
        }
        return csvParse(line);
    }
    private String[] csvParse(String line) throws NDSException
    {
        String[] ret=new String[0];
        ArrayList<String> strs = new ArrayList<String>();

        while (!line.isEmpty())
        {
            if (line.contains(","))
            {
                int cp = line.indexOf(",");
                String s = line.substring(0, cp).trim();
                strs.add(s);
                if (cp < line.length()-1)
                {
                    line = line.substring(cp+1);
                }
                else
                {
                    line = "";
                }
            }
            else
            {
                strs.add(line.trim());
                line = "";
            }
        }
        
        return strs.toArray(ret);
    }

    private void flushInput()
    {
        if (ins != null)
        {
            try
            {
                while (ins.available() > 0)
                {
                    ins.read();
                }
//                ins.close();
//                ins = null;
            }
            catch (IOException ex)
            {
                //we ignore these
            }
        }
    }

    private void updateTiming()
    {
        totalMs += System.currentTimeMillis() - startMs;
    }

    public long getTotalTimeMs()
    {
        return totalMs;
    }
    
}
