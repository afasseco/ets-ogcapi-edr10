package org.opengis.cite.ogcapiedr10.queries;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import static io.restassured.http.ContentType.JSON;
import static io.restassured.http.Method.GET;

public class CorridorQueryProcessor extends AbstractProcessor{

    double sizeOfLensSide = 1d; //in degrees
    double corridorWidth = 2;
    String corridorWidthUnit = "m";
    public final String queryTypeNotSupported = "None of the collections support this query type. Increase the number of collections to parse.";


    public String validateCorridorQueryUsingParameters(Set<String> collectionIds, String rootUri, int noOfCollections, RequestSpecification ini){
        StringBuffer sb = new StringBuffer();

        ArrayList<String> collectionsList = new ArrayList<String>();
        collectionsList.addAll(collectionIds);
        
        int numberOfCollectionsWithCorridorSupport = 0;

        for (int c = 0; c < Math.min(noOfCollections,collectionsList.size()); c++) {

            String collectionId = collectionsList.get(c);



            boolean supportsCorridorQuery = false;

            String url = rootUri.toString() + "/collections/" + collectionId;

            Response response = ini.baseUri(url).accept(JSON).when().request(GET);
            JsonPath jsonResponse = response.jsonPath();
            HashMap dataQueries = jsonResponse.getJsonObject("data_queries");
            supportsCorridorQuery = dataQueries.containsKey("corridor");





            if (supportsCorridorQuery) {
            	
            	numberOfCollectionsWithCorridorSupport++;

                HashMap parameterNames = jsonResponse.getJsonObject("parameter_names");
                Set parameterNamesSet = parameterNames.keySet();
                Iterator<String> parameterNamesIterator = parameterNamesSet.iterator();

                parameterNamesIterator.hasNext();
                String sampleParamaterName = parameterNamesIterator.next();

                List<String> crsList = jsonResponse.getList("crs");

                String supportedCRS = null;
                for (int q = 0; q < crsList.size(); q++) {
                    if (crsList.get(q).equals("CRS:84") || crsList.get(q).equals("CRS84") || crsList.get(q).equals("EPSG:4326")) {
                        supportedCRS = "CRS84";
                    }
                }
                if (supportedCRS == null) {
                    sb.append(collectionId + " does not support CRS84 CRS. \n");

                }

                HashMap corridorQuery = (HashMap) dataQueries.get("corridor");
                HashMap link = (HashMap) corridorQuery.get("link");
                HashMap variables = (HashMap) link.get("variables");
                ArrayList<String> outputFormatList = (ArrayList<String>) variables.get("output_formats");
                String supportedFormat = null;
                for (int f = 0; f < outputFormatList.size(); f++) {
                    if (outputFormatList.get(f).equals("CoverageJSON")) {  //preference for CoverageJSON if supported
                        supportedFormat = outputFormatList.get(f);
                    }
                    else if (outputFormatList.get(f).equals("GeoJSON")) {
                        supportedFormat = outputFormatList.get(f);
                    }
                }

                double medianx = 0d;
                double mediany = 0d;
                double lminx = 0d; //lens
                double lminy = 0d; //lens
                double lmaxx = 0d; //lens
                double lmaxy = 0d; //lens

                HashMap extent = jsonResponse.getJsonObject("extent");
                if (extent.containsKey("spatial")) {

                    HashMap spatial = (HashMap) extent.get("spatial");

                    if (!spatial.containsKey("bbox"))
                    {
                        sb.append("spatial extent of collection "+collectionId+" missing bbox. \n");
                        continue;
                    }

                    ArrayList bboxEnv = (ArrayList) spatial.get("bbox"); // for some unknown reason the library returns JSON types as Integers only


                    ArrayList bbox = null;

                    if(bboxEnv.get(0).getClass().toString().contains("java.lang.Integer") ||
                            bboxEnv.get(0).getClass().toString().contains("java.lang.Double")||
                            bboxEnv.get(0).getClass().toString().contains("java.lang.Float")) {	//for EDR API V1.0.0
                        bbox = bboxEnv;

                    }
                    else if(bboxEnv.get(0).getClass().toString().contains("java.util.ArrayList")) {  //for EDR API V1.0.1
                        bbox = (ArrayList) bboxEnv.get(0);
                    }



                    if (bbox.size() > 3) {

                        if (bbox.get(0).getClass().toString().contains("Integer")
                                || bbox.get(0).getClass().toString().contains("Double")
                                || bbox.get(0).getClass().toString().contains("Float")) {
                            double minx = Double.parseDouble(bbox.get(0).toString());
                            double miny = Double.parseDouble(bbox.get(1).toString());
                            double maxx = Double.parseDouble(bbox.get(2).toString());
                            double maxy = Double.parseDouble(bbox.get(3).toString());

                            medianx = minx + ((maxx - minx) / 2d);
                            mediany = miny + ((maxy - miny) / 2d);

                            lminx = medianx - sizeOfLensSide;
                            lminy = mediany - sizeOfLensSide;
                            lmaxx = medianx + sizeOfLensSide;
                            lmaxy = mediany + sizeOfLensSide;



                        }

                    } else {
                        sb.append("bbox of spatial extent of collection" + collectionId
                                + " has fewer than four coordinates. \n");
                    }


                }


                String sampleParamaterNameSafe = null;
                try {
                    sampleParamaterNameSafe = URLEncoder.encode(sampleParamaterName,"UTF8");
                }
                catch(Exception ex) {ex.printStackTrace();}


                String sampleDateTime = null;
                if (extent.containsKey("temporal")) {


                    HashMap temporal = (HashMap) extent.get("temporal");

                    if (!temporal.containsKey("interval"))
                    {

                        sb.append("Temporal extent of collection "+collectionId+" missing interval. \n");
                        continue;
                    }


                    ArrayList intervalEnv = (ArrayList) temporal.get("interval");


                    ArrayList interval = null;

                    if(intervalEnv.get(0).getClass().toString().contains("java.lang.String")) {
                        interval = intervalEnv;
                    }
                    else if(intervalEnv.get(0).getClass().toString().contains("java.util.ArrayList")) {

                        interval = (ArrayList) intervalEnv.get(0);
                    }


                    if (interval.size() > 1) {


                        sampleDateTime = interval.get(0)+"/"+interval.get(1);


                    }

                }




                String constructedURL = url + "/corridor?parameter-name="
                        + sampleParamaterNameSafe + "&coords=" + "LINESTRING(" +
                        lminx + "+"+ lminy + ","+
                        medianx + "+"+ mediany + ","+
                        lmaxx + "+"+ lmaxy +
                        ")" + "&corridor-width="+corridorWidth+"&width-units="+corridorWidthUnit+"&f=" + supportedFormat+"&datetime="+sampleDateTime;
                System.out.println("C "+constructedURL);



                String pageContent = null;
                try {
                    pageContent = readStringFromURL(constructedURL,10);  //you can use Integer.MAX_VALUE for no limit
                    
                    }
                catch(Exception ex) { ex.printStackTrace();}

                if(pageContent!=null) {

                    if(pageContent.contains("Coverage") || pageContent.contains("Feature")) {
                        //do nothing
                    }
                    else {
                        sb.append("Response of Corridor Query on collection " + collectionId
                                + " did not contain a recognised encoding. \n");
                    }

                }
                else {
                    sb.append("Response of Corridor Query on collection " + collectionId
                            + " was null. \n");
                }



            }
            else { //does not support Corridor queries
            	continue;
            }



        }
        if(numberOfCollectionsWithCorridorSupport==0) {
        	sb.append(queryTypeNotSupported+"\n");
        }


        return sb.toString();
    }


 
}