package ru.ees.Indexer.utils;

import java.util.ArrayList;
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

    public static List<Integer> getList(String separated) {
        String[] numbers = separated.split(",");
        List<Integer> result = new ArrayList<>();
        for (String number : numbers) {
            result.add(Integer.parseInt(number));
        }
        return result;
    }

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
        String str = SQLiteUtils.getString(list);
        System.out.println(str);
        List<Integer> back = SQLiteUtils.getList(str);
        System.out.println(back);

        String alone = "2";
        System.out.println(SQLiteUtils.getList(alone));
    }
}
