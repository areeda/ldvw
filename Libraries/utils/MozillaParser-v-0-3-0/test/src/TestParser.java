import com.dappit.Dapper.parser.*;
import org.w3c.dom.Document;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;

import java.io.*;

public class TestParser
{
	private static boolean failed = false;

	public static void main(String[] argv)
		throws Exception
	{
		if( argv.length > 0 )
		{
			System.err.println("Explicitly calling MozillaParser.init(null, \"" + argv[0] + "\")");
			MozillaParser.init( null, argv[0] );
		}

		String html = "<html><body><p>Hello!</p><b style='background-color: blue;'>blah\u00a2";
		byte[] isoBytes = html.getBytes("ISO-8859-1");
		byte[] utfBytes = html.getBytes("UTF-8");

		doParse( isoBytes, "UTF-8", false );
		doParse( isoBytes, "utf-8", false );
		doParse( utfBytes, "UTF-8", false );

		String html2 = "<html><body><o:p>Blarg</o:p></body></html>";
		doParse( html2.getBytes("UTF-8"), "UTF-8", false );

		String html3 = "<html><body><o:p xmlns:o=\"http://example.com/\">Blarg</o:p></body></html>";
		doParse( html3.getBytes("UTF-8"), "UTF-8", false );

		byte[] data = readFile( "test/data/testHebrew.html" );
		doParse( data, "UTF-8", false );

		doParse( "<html><body>Blah".getBytes(), "windows-1255", false );
		doParse( "<html><body>Blah".getBytes(), "Windows-1255", false );
		doParse( "<html><body>Blah".getBytes(), "WINDOWS-1255", false );
	}

	private static byte[] readFile( String fileName )
		throws Exception
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		FileInputStream fis = new FileInputStream(fileName);
		byte[] buffer = new byte[4096];

		while( true )
		{
			int numBytes = fis.read(buffer);
			if( numBytes <= 0 )
				break;
			bos.write(buffer, 0, numBytes);
		}

		fis.close();
		return bos.toByteArray();
	}

	private static void doParse( byte[] htmlBytes, String htmlEncoding, boolean shouldFail )
	{
		Exception e = null;

		try
		{
			MozillaParser parser = new MozillaParser();
			Document doc = parser.parse( htmlBytes, htmlEncoding );

			TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(System.out));
			System.out.println("");
		}
		catch( Exception ex )
		{
			
			e = ex;
		}

		if( shouldFail && e == null )
		{
			failed = true;
			System.err.println( "FAIL: expected exception; none was thrown" );
		}

		if( !shouldFail && e != null )
		{
			failed = true;
			System.err.println( "FAIL:" );
			e.printStackTrace();
		}
	}
}
