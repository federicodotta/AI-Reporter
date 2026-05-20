package org.fd;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.logging.Logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    // It is a recursive method, in order to handle multiple different "first" tags (es. first_20 and first_15)
    public static String replaceFirstChars(String tag_name, String from, ByteArray reqRes,
                                       boolean debug, Logging logging) {

        String result = from;

        while(result.contains("{{" + tag_name + "_first")) {
            Pattern pattern = Pattern.compile("\\{\\{" + tag_name + "_first_([\\d]+)\\}\\}");
            Matcher matcher = pattern.matcher(result);
            if(matcher.find()) {
                int number_of_chars = Integer.parseInt(matcher.group(1));
                String fullMarker = matcher.group(0);

                if(debug) {
                    logging.logToOutput("[AI Reporter debug] GET FIRST CHARS: " + tag_name);
                    logging.logToOutput("[AI Reporter debug] Marker: " + fullMarker);
                    logging.logToOutput("[AI Reporter debug] # chars: " + number_of_chars);
                    logging.logToOutput("[AI Reporter debug] Original: ");
                    logging.logToOutput(result);
                }

                int reqResSize = reqRes.getBytes().length;
                if(reqResSize > number_of_chars)
                    result = result.replace(fullMarker, reqRes.subArray(0,number_of_chars).toString());
                else
                    result = result.replace(fullMarker, reqRes.toString());

                if(debug) {
                    logging.logToOutput("[AI Reporter debug] Edited: ");
                    logging.logToOutput(result);
                    logging.logToOutput("[AI Reporter debug] END");
                    logging.logToOutput("");
                }
            }
        }

        return result;

    }

    // It is a recursive method, in order to handle multiple different "last" tags (es. last_20 and last_15)
    public static String replaceLastChars(String tag_name, String from, ByteArray reqRes,
                                       boolean debug, Logging logging) {

        String result = from;

        while(result.contains("{{" + tag_name + "_last")) {
            Pattern pattern = Pattern.compile("\\{\\{" + tag_name + "_last_([\\d]+)\\}\\}");
            Matcher matcher = pattern.matcher(result);
            if(matcher.find()) {
                int number_of_chars = Integer.parseInt(matcher.group(1));
                String fullMarker = matcher.group(0);

                if(debug) {
                    logging.logToOutput("[AI Reporter debug] GET LAST CHARS: " + tag_name);
                    logging.logToOutput("[AI Reporter debug] Marker: " + fullMarker);
                    logging.logToOutput("[AI Reporter debug] # chars: " + number_of_chars);
                    logging.logToOutput("[AI Reporter debug] Original: ");
                    logging.logToOutput(result);
                }

                int reqResSize = reqRes.getBytes().length;
                if(reqResSize > number_of_chars)
                    result = result.replace(fullMarker, reqRes.subArray(reqResSize-number_of_chars,reqResSize).toString());
                else
                    result = result.replace(fullMarker, reqRes.toString());

                if(debug) {
                    logging.logToOutput("[AI Reporter debug] Edited: ");
                    logging.logToOutput(result);
                    logging.logToOutput("[AI Reporter debug] END");
                    logging.logToOutput("");
                }
            }
        }

        return result;

    }

}
