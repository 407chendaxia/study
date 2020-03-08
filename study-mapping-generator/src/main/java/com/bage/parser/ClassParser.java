package com.bage.parser;

import java.util.ArrayList;
import java.util.List;

public class ClassParser {

    /**
     * 当前类所有的超类
     *
     * @param cls
     * @return
     */
    public static Class[] getAllSuperClass(Class cls) {
        List<Class> list = new ArrayList<>();
        Class superCls = cls.getSuperclass();
        while (superCls !=  Object.class) {
            list.add(superCls);
            superCls = superCls.getSuperclass();
        }

        return list.toArray(new Class[list.size()]);
    }

}
