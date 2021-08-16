package pt.isec;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class Utils {
    public static ArrayList<String> parseString(String string, String token) {
        return new ArrayList<>(Arrays.asList(string.split(token)));
    }
}
