package org.fd;

import burp.api.montoya.core.ByteArray;
import burp.api.montoya.logging.Logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String replaceFirstChars(String tag_name, String from, ByteArray reqRes,
                                           boolean debug, Logging logging) {

        // Pattern.quote(tag_name) protects against regex metacharacters in the tag name.
        // Drop it if you intentionally pass regex through tag_name.
        Pattern pattern = Pattern.compile("\\{\\{" + Pattern.quote(tag_name) + "_first_([\\d]+)\\}\\}");
        Matcher matcher = pattern.matcher(from);

        StringBuilder result = new StringBuilder();
        int reqResSize = reqRes.getBytes().length;

        // find() advances through the input on each call, so the loop always terminates.
        while (matcher.find()) {
            int number_of_chars = Integer.parseInt(matcher.group(1));
            String fullMarker = matcher.group(0);

            if (debug) {
                logging.logToOutput("[AI Reporter debug] GET FIRST CHARS: " + tag_name);
                logging.logToOutput("[AI Reporter debug] Marker: " + fullMarker);
                logging.logToOutput("[AI Reporter debug] # chars: " + number_of_chars);
            }

            String replacement;
            if (reqResSize > number_of_chars)
                replacement = reqRes.subArray(0, number_of_chars).toString();
            else
                replacement = reqRes.toString();

            // quoteReplacement prevents $ and \ in the request bytes from being
            // interpreted as group references / escapes by appendReplacement.
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));

            if (debug) {
                logging.logToOutput("[AI Reporter debug] Replaced with " + replacement.length() + " chars");
                logging.logToOutput("[AI Reporter debug] Edited: ");
                logging.logToOutput(result);
                logging.logToOutput("[AI Reporter debug] END");
                logging.logToOutput("");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    // Iterative method (not recursive): handles multiple different "last" tags
    // in one pass (e.g. last_20 and last_15).
    public static String replaceLastChars(String tag_name, String from, ByteArray reqRes,
                                          boolean debug, Logging logging) {

        // Pattern.quote(tag_name) protects against regex metacharacters in the tag name.
        // Drop it if you intentionally pass regex through tag_name.
        Pattern pattern = Pattern.compile("\\{\\{" + Pattern.quote(tag_name) + "_last_([\\d]+)\\}\\}");
        Matcher matcher = pattern.matcher(from);

        StringBuilder result = new StringBuilder();
        int reqResSize = reqRes.getBytes().length;

        // find() advances through the input on each call, so the loop always terminates.
        while (matcher.find()) {
            int number_of_chars = Integer.parseInt(matcher.group(1));
            String fullMarker = matcher.group(0);

            if (debug) {
                logging.logToOutput("[AI Reporter debug] GET LAST CHARS: " + tag_name);
                logging.logToOutput("[AI Reporter debug] Marker: " + fullMarker);
                logging.logToOutput("[AI Reporter debug] # chars: " + number_of_chars);
            }

            String replacement;
            if (reqResSize > number_of_chars)
                replacement = reqRes.subArray(reqResSize - number_of_chars, reqResSize).toString();
            else
                replacement = reqRes.toString();

            // quoteReplacement prevents $ and \ in the request bytes from being
            // interpreted as group references / escapes by appendReplacement.
            // It does NOT alter the bytes in the output — they land verbatim.
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));

            if (debug) {
                logging.logToOutput("[AI Reporter debug] Replaced with " + replacement.length() + " chars");
                logging.logToOutput("[AI Reporter debug] END");
                logging.logToOutput("");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

}
