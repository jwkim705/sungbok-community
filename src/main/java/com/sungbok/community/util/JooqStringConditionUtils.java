package com.sungbok.community.util;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class JooqStringConditionUtils {

    public static Condition containsIfNotBlank(Field<String> field, String value) {
        if (StringUtils.isBlank(value)) {
            return DSL.noCondition();
        }

        return field.like("%" + value + "%");
    }

    public static Condition eqIfNotBlank(Field<String> field, String value) {
        if (StringUtils.isBlank(value)) {
            return DSL.noCondition();
        }
        return field.eq(value);
    }
}