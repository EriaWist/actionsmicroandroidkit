package com.actionsmicro.media.webplaylist.youtubeplaylist;

public class PlayListUtils {

    public static int peekCurrent(String index) {
        if (index.contains("-")) {
            String result = index.substring(index.lastIndexOf("(") + 1, index.lastIndexOf("-"));
            return Integer.valueOf(result) - 1;
        } else {
            //return Integer.valueOf(index) - 1;
            return -1;
        }
    }

    public static int peekSize(String index) {
        if (index.contains("-")) {
            String result = index.substring(index.lastIndexOf("-") + 1, index.lastIndexOf(")"));
            return Integer.valueOf(result);
        } else {
            return -1;
        }
    }

    public static String removeTopIndex(String index) {
        if (index.contains("/")) {
            return index.substring(0, index.lastIndexOf("/"));
            //return index.substring(index.indexOf("/")+1);
        } else {
            return "";
        }
    }
}
