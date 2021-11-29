package org.opengis.cite.ogcapiedr10;

import static io.restassured.RestAssured.given;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;


import org.opengis.cite.ogcapiedr10.util.ClientUtils;

import org.opengis.util.FactoryException;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.specification.RequestSpecification;

/**
 * A supporting base class that sets up a common test fixture. These configuration methods are invoked before those
 * defined in a subclass.
 */
public class CommonFixture {

    private ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();

    private ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();

    protected RequestLoggingFilter requestLoggingFilter;

    protected ResponseLoggingFilter responseLoggingFilter;

    protected URI rootUri;
    
	protected String testingWktPOINT = "POINT(-1.054687%2052.498649)";  //TODO change to user inputs
	protected String geoJSONTestingCollection = "gfs-surface-precip";   //TODO change to user inputs

    /**
     * Initializes the common test fixture with a client component for interacting with HTTP endpoints.
     *
     * @param testContext
     *            The test context that contains all the information for a test run, including suite attributes.
     */
    @BeforeClass
    public void initCommonFixture( ITestContext testContext ) {
        initLogging();
        rootUri = (URI) testContext.getSuite().getAttribute( SuiteAttribute.IUT.getName() );
        

        
        
    }

    @BeforeMethod
    public void clearMessages() {
        initLogging();
    }

    public String getRequest() {
        return requestOutputStream.toString();
    }

    public String getResponse() {
        return responseOutputStream.toString();
    }

    protected RequestSpecification init() {
        return given().filters( requestLoggingFilter, responseLoggingFilter ).log().all();
    }

    /**
     * Obtains the (XML) response entity as a DOM Document. This convenience method wraps a static method call to
     * facilitate unit testing (Mockito workaround).
     *
     * @param response
     *            A representation of an HTTP response message.
     * @param targetURI
     *            The target URI from which the entity was retrieved (may be null).
     * @return A Document representing the entity.
     *
     * @see ClientUtils#getResponseEntityAsDocument public Document getResponseEntityAsDocument( ClientResponse
     *      response, String targetURI ) { return ClientUtils.getResponseEntityAsDocument( response, targetURI ); }
     */

    /**
     * Builds an HTTP request message that uses the GET method. This convenience method wraps a static method call to
     * facilitate unit testing (Mockito workaround).
     *
     * @return A ClientRequest object.
     *
     * @see ClientUtils#buildGetRequest public ClientRequest buildGetRequest( URI endpoint, Map<String, String>
     *      qryParams, MediaType... mediaTypes ) { return ClientUtils.buildGetRequest( endpoint, qryParams, mediaTypes
     *      ); }
     */

    private void initLogging() {
        this.requestOutputStream = new ByteArrayOutputStream();
        this.responseOutputStream = new ByteArrayOutputStream();
        PrintStream requestPrintStream = new PrintStream( requestOutputStream, true );
        PrintStream responsePrintStream = new PrintStream( responseOutputStream, true );
        requestLoggingFilter = new RequestLoggingFilter( requestPrintStream );
        responseLoggingFilter = new ResponseLoggingFilter( responsePrintStream );
    }
    
	protected String constructDateTimeValue(String input) throws Exception {
		
		// Example input is R36/2021-10-05T03:00:00Z/PT3H
		
		String startDateOfInterval = null;
		String endDateOfInterval = null;
		

		if (!input.contains("00:00Z"))
			input = input.replace(":00Z", ":00:00Z"); // TODO For testing. REMOVE when done.

		String[] token = input.split("/");

		for (int i = 0; i < token.length; i++) {
			if (token[i].split("-").length == 3) {
				startDateOfInterval = token[i];
				i = token.length; // we found a valid token so we break the loop
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Calendar c = Calendar.getInstance();

		c.setTime(sdf.parse(startDateOfInterval));

		c.add(Calendar.HOUR, 3);
		endDateOfInterval = sdf.format(c.getTime());

		return startDateOfInterval + "/" + endDateOfInterval;
	}
}
