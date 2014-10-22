package ru.ees.Indexer.utils;

import java.util.Arrays;
import java.util.List;

public class SQLiteUtils {
    public static String getString(List<?> list) {
        StringBuffer result = new StringBuffer();
        for (Object i : list) {
            result.append(i.toString() + ',');
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
        System.out.println(SQLiteUtils.getString(list));
    }
}
