package dev.sunbirdrc.registry.util;

import java.util.ArrayList;
import java.util.List;

public class CommonUtils {

    public static List getEntityName() {
        List<String> entityList = new ArrayList<>();
        entityList.add("StudentFromUP");
        entityList.add("StudentGoodstanding");
        entityList.add("StudentForeignVerification");
        entityList.add("StudentOutsideUP");
        return entityList;
    }

}
