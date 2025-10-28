package com.zephyrcicd.tdengineorm.util;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.zephyrcicd.tdengineorm.annotation.TdColumn;
import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.annotation.TdTag;
import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.constant.TdColumnConstant;
import com.zephyrcicd.tdengineorm.constant.TdSqlConstant;
import com.zephyrcicd.tdengineorm.enums.TdFieldTypeEnum;
import com.zephyrcicd.tdengineorm.enums.TdSelectFuncEnum;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.func.GetterFunction;
import com.zephyrcicd.tdengineorm.strategy.DynamicNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * @author Zephyr
 */
@Slf4j
public class TdSqlUtil {

    public static String getTbName(Class<?> entityClass) {
        String tbNameByAnno = getTbNameByAnno(entityClass);
        return StringUtils.hasText(tbNameByAnno) ?
                tbNameByAnno : StrUtil.toUnderlineCase(entityClass.getSimpleName());
    }

    public static String getTbNameByAnno(Class<?> entityClass) {
        TdTable annotation = entityClass.getAnnotation(TdTable.class);
        return annotation == null ? StrUtil.EMPTY : annotation.value();
    }

    public static Collector<CharSequence, ?, String> getParenthesisCollector() {
        return Collectors.joining(SqlConstant.COMMA, SqlConstant.LEFT_BRACKET, SqlConstant.RIGHT_BRACKET);
    }

    /**
     * 将列表字符使用逗号分隔
     *
     * @param itemList 参数列表
     * @return 拼接后的字符串, 如(1,2,3,4,5,6,7)
     */
    public static String separateByCommas(List<String> itemList, boolean bracket) {
        return bracket ? itemList.stream().collect(TdSqlUtil.getParenthesisCollector()) : String.join(SqlConstant.COMMA, itemList);
    }

    /**
     * 根据字段解析获取对应字段名和参数名部分SQL
     * 如 (column1, column2, column3) 和 (:param1, :param2, :param3)
     *
     * @param fields List<Field>
     * @return Pair<String, String>
     */
    public static Pair<String, String> getColumnNameSqlAndParamNameSqlPair(List<Field> fields) {
        // 获取字段名称部分SQL
        List<String> paramNameList = new ArrayList<>(fields.size());
        return Pair.of(
                fields.stream()
                        .map(field -> {
                            paramNameList.add(":" + field.getName());
                            return getColumnName(field);
                        })
                        .collect(TdSqlUtil.getParenthesisCollector()),
                TdSqlUtil.separateByCommas(paramNameList, true)
        );
    }


    /**
     * 获取插入到SQL前缀, 截止到VALUES
     * <p>
     * <p>
     * 如 INSERT INFO tb_a (a,b,c,d) VALUES
     *
     * @param tbName 表名称
     * @param fields 字段
     * @return {@link StringBuilder }
     */
    public static StringBuilder getInsertIntoSqlPrefix(String tbName, List<Field> fields) {
        return new StringBuilder(SqlConstant.INSERT_INTO)
                .append(tbName)
                .append(fields.stream().map(TdSqlUtil::getColumnName).collect(getColumnWithBracketCollector()))
                .append(SqlConstant.VALUES);
    }

    /**
     * 获取插入语句的VALUES后的后缀部分
     * <p>
     * <p>
     * 例如 (:a, :b, :c)
     *
     * @param entity 需要入库的实体对象
     * @param fields 字段
     * @param index  目的是为了避免参数重复, 适用于需要批量插入的场景, 如只需要插入一行数据, 可以为任何值, 不过建议直接使用getInsertIntoSql方法
     * @return {@link Pair }<{@link String }, {@link Map }<{@link String }, {@link Object }>> Key是SQL部分, Value是参数名称和参数值Map
     */
    public static <T> Pair<String, Map<String, Object>> getInsertSqlSuffix(T entity, List<Field> fields, int index) {
        if (fields.isEmpty()) {
            fields = ClassUtil.getAllFields(entity.getClass());
        }
        Map<String, Object> paramsMapList = new HashMap<>();
        String suffixSql = fields.stream()
                .map(field -> {
                    String fieldName = field.getName();
                    paramsMapList.put(fieldName + index, ReflectUtil.getFieldValue(entity, field));
                    return ":" + fieldName + index;
                })
                .collect(getColumnWithBracketCollector());
        return Pair.of(suffixSql, paramsMapList);
    }

    /**
     * 根据字段解析获取对应字段名和参数名部分SQL
     * 如 (column1, column2, column3) 和 (:param1, :param2, :param3) ON DUPLICATE KEY UPDATE column2 = :param2, column3 = :param3
     *
     * @param fields List<Field>
     * @return Pair<String, String>
     */
    public static <T> Pair<String, String> getColumnNameSqlAndParamNameWithUpdateSqlPair(List<Field> fields, Class<T> entityClass) {
        List<String> paramNameList = new ArrayList<>(fields.size());
        List<String> updateColumnList = new ArrayList<>(fields.size());

        String columnNameSql = fields.stream()
                .map(field -> buildColumnSql(field, TdColumnConstant.TS, paramNameList, updateColumnList))
                .collect(TdSqlUtil.getParenthesisCollector());

        // 拼接参数部分SQL
        String valuesSql = separateByCommas(paramNameList, true);
        String updateSql = SqlConstant.ON_DUPLICATE_KEY_UPDATE + separateByCommas(updateColumnList, false);

        return Pair.of(columnNameSql, valuesSql + updateSql);
    }

    /**
     * 根据字段解析获取对应字段名和参数名部分SQL
     * 如 (column1, column2, column3) 和 (:param1, :param2, :param3)
     *
     * @param map Map<String, Object>
     * @return Pair<String, String>
     */
    public static Pair<String, String> getColumnNameSqlAndParamNameSqlPair(Map<String, Object> map, String idFieldName, boolean update) {
        List<String> paramNameList = new ArrayList<>(map.size());
        List<String> updateColumnList = new ArrayList<>(map.size());

        String columnNameSql = map.keySet().stream()
                .map(fieldName -> buildColumnSql(idFieldName, paramNameList, updateColumnList, fieldName))
                .collect(TdSqlUtil.getParenthesisCollector());

        // 拼接参数部分SQL
        String valuesSql = separateByCommas(paramNameList, true);
        String rightSql = update ? valuesSql + SqlConstant.ON_DUPLICATE_KEY_UPDATE + separateByCommas(updateColumnList, false) : valuesSql;
        return Pair.of(columnNameSql, rightSql);
    }

    private static String buildColumnSql(String idFieldName, List<String> paramNameList, List<String> updateColumnList, String fieldName) {
        String columnName = StrUtil.toUnderlineCase(fieldName);
        String paramName = SqlConstant.COLON + fieldName;
        paramNameList.add(paramName);
        if (!Arrays.asList("createTime", idFieldName).contains(fieldName)) {
            updateColumnList.add(columnName + SqlConstant.EQUAL + paramName);
        }
        return columnName;
    }

    private static String buildColumnSql(Field field, String idFieldName, List<String> paramNameList, List<String> updateColumnList) {
        String fieldName = field.getName();
        String columnName = getColumnName(field, fieldName);
        String paramName = SqlConstant.COLON + fieldName;
        paramNameList.add(paramName);
        if (!Arrays.asList("createTime", idFieldName).contains(fieldName)) {
            updateColumnList.add(columnName + SqlConstant.EQUAL + paramName);
        }
        return columnName;
    }

    private static String getColumnName(Field field, String fieldName) {
        TdColumn tableFieldAnno = field.getAnnotation(TdColumn.class);
        String columnName;
        if (tableFieldAnno != null) {
            columnName = tableFieldAnno.value();
        } else {
            columnName = StrUtil.toUnderlineCase(fieldName);
        }
        return columnName;
    }


    /**
     * 获取插入语句的前缀 SQL
     *
     * @param entityList    实体列表
     * @param defaultTbName 默认表名
     * @param entityClass   实体类class 对象
     * @param fields        List<Field>
     * @param <T>           实体类泛型
     * @return SQL string builder
     */
    public static <T> StringBuilder getInsertIntoSql(boolean entityList, String defaultTbName, Class<T> entityClass, List<Field> fields, boolean update) {
        if (entityList || CollectionUtils.isEmpty(fields)) {
            return null;
        }

        // 获取字段名称部分SQL
        Pair<String, String> columnNameSqlAndParamNameSqlPair = update ? getColumnNameSqlAndParamNameWithUpdateSqlPair(fields, entityClass)
                : getColumnNameSqlAndParamNameSqlPair(fields);

        // 拼接INSERT INTO语句的初始化 SQL
        return new StringBuilder(SqlConstant.INSERT_INTO)
                .append(StrUtil.isBlankIfStr(defaultTbName) ? getTbName(entityClass) : defaultTbName)
                .append(columnNameSqlAndParamNameSqlPair.getKey())
                .append(SqlConstant.VALUES)
                .append(columnNameSqlAndParamNameSqlPair.getValue());
    }


    public static StringBuilder getInsertIntoSql(String tbName, Map<String, Object> map) {
        Set<String> keySet = map.keySet();
        List<String> columNames = new ArrayList<>();
        List<String> paramsNames = new ArrayList<>();
        keySet.forEach(key -> {
            columNames.add(StrUtil.toUnderlineCase(key));
            paramsNames.add(SqlConstant.COLON + key);
        });

        // 拼接INSERT INTO语句的初始化 SQL
        return new StringBuilder(SqlConstant.INSERT_INTO)
                .append(tbName)
                .append(separateByCommas(columNames, true))
                .append(SqlConstant.VALUES)
                .append(separateByCommas(paramsNames, true));
    }

    public static StringBuilder getInsertIntoSql(String tbName, Map<String, Object> map, String idKeyName, boolean update) {
        Pair<String, String> columnNameSqlAndParamNameSqlPair = getColumnNameSqlAndParamNameSqlPair(map, idKeyName, update);

        // 拼接INSERT INTO语句的初始化 SQL
        return new StringBuilder(SqlConstant.INSERT_INTO)
                .append(tbName)
                .append(columnNameSqlAndParamNameSqlPair.getKey())
                .append(SqlConstant.VALUES)
                .append(columnNameSqlAndParamNameSqlPair.getValue());
    }


    /**
     * 获取Java字段对应的数据库的字段名称
     * 优先取@TableField注解的value属性, 没有则取Snake形式的字段名称
     *
     * @param field Field
     * @return String
     */
    public static String getColumnName(Field field) {
        TdColumn tableField = field.getAnnotation(TdColumn.class);
        String fieldNameUnderlineCase = StrUtil.toUnderlineCase(field.getName());
        return tableField == null ? fieldNameUnderlineCase
                : StrUtil.isBlank(tableField.value()) ? fieldNameUnderlineCase : tableField.value();
    }

    /**
     * 获取Field对应的数据库字段名, 用逗号","拼接
     *
     * @param fields 字段
     * @return {@link String }
     */
    public static String joinColumnNames(List<Field> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return StrUtil.EMPTY;
        }

        return fields.stream().map(TdSqlUtil::getColumnName).collect(getColumnWithoutBracketCollector());
    }


    /**
     * 获取Field对应的数据库字段名, 用逗号","拼接, 并使用括号进行包裹
     *
     * @param fields 字段
     * @return {@link String }
     */
    public static String joinColumnNamesWithBracket(List<Field> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return StrUtil.EMPTY;
        }

        return fields.stream().map(TdSqlUtil::getColumnName).collect(getColumnWithBracketCollector());
    }

    public static Collector<CharSequence, ?, String> getColumnWithBracketCollector() {
        return Collectors.joining(SqlConstant.COMMA, SqlConstant.LEFT_BRACKET, SqlConstant.RIGHT_BRACKET);
    }

    public static Collector<CharSequence, ?, String> getColumnWithoutBracketCollector() {
        return Collectors.joining(SqlConstant.COMMA);
    }


    public static Pair<String, List<Field>> getTbNameAndFieldListPair(Class<?> clazz) {
        // 获取所有字段
        List<Field> fieldList = ClassUtil.getAllFields(clazz);
        Assert.notEmpty(fieldList, "[TdSqlUtil#getTbNameAndFieldListPair] No field found!");
        return Pair.of(getTbName(clazz), fieldList);
    }


    /**
     * 获取字段名称以及对应的值的Map, 组成映射关系
     *
     * @param fields 字段
     * @param o      o
     * @return {@link Map }<{@link String }, {@link Object }>
     */
    public static Map<String, Object> getFiledValueMap(List<Field> fields, Object o) {
        Map<String, Object> tagValueMap = new HashMap<>(fields.size());
        for (Field field : fields) {
            tagValueMap.put(field.getName(), ReflectUtil.getFieldValue(o, field));
        }
        return tagValueMap;
    }

    public static String joinColumnNamesAndValuesSql(Object object, List<Field> fields, Map<String, Object> paramsMap) {
        if (CollectionUtils.isEmpty(fields)) {
            return StrUtil.EMPTY;
        }

        List<String> fieldValueParamNames = new ArrayList<>();
        String fieldNameStr = fields.stream().map(field -> {
            String columnName = TdSqlUtil.getColumnName(field);
            fieldValueParamNames.add(columnName);
            paramsMap.put(columnName, ReflectUtil.getFieldValue(object, field));
            return columnName;
        }).collect(TdSqlUtil.getColumnWithBracketCollector());

        String fieldValueParamsStr = fieldValueParamNames.stream()
                .map(item -> SqlConstant.COLON + item)
                .collect(TdSqlUtil.getColumnWithBracketCollector());

        return fieldNameStr + SqlConstant.VALUES + fieldValueParamsStr;
    }

    public static <T> String getColumnName(GetterFunction<T, ?> getterFunc) {
        String fieldName = LambdaUtil.getFiledNameByGetter(getterFunc);
        Field field = ClassUtil.getFieldByName(LambdaUtil.getEntityClass(getterFunc), fieldName);
        String tableFiledAnnoValue = AnnotationUtil.getAnnotationValue(field, TdColumn.class, "value");
        return StrUtil.isNotBlank(tableFiledAnnoValue) ? tableFiledAnnoValue : StrUtil.toUnderlineCase(fieldName);
    }

    public static <T> String getColumnName(Class<T> tClass, GetterFunction<T, ?> getterFunc) {
        String fieldName = LambdaUtil.getFiledNameByGetter(getterFunc);
        Field field = ClassUtil.getFieldByName(tClass, fieldName);
        String tableFiledAnnoValue = AnnotationUtil.getAnnotationValue(field, TdColumn.class, "value");
        return StrUtil.isNotBlank(tableFiledAnnoValue) ? tableFiledAnnoValue : StrUtil.toUnderlineCase(fieldName);
    }

    public static <T> String joinSqlValue(T entity, List<Field> fields, Map<String, Object> paramsMapList, int index) {
        Map<Boolean, List<Field>> fieldGroups = fields.stream()
                .collect(Collectors.partitioningBy(field -> field.isAnnotationPresent(TdTag.class)));
        List<Field> commFields = fieldGroups.get(Boolean.FALSE);

        return commFields.stream()
                .map(field -> {
                    String fieldName = field.getName();
                    paramsMapList.put(fieldName + index, ReflectUtil.getFieldValue(entity, field));
                    return ":" + fieldName + index;
                })
                .collect(TdSqlUtil.getColumnWithBracketCollector());
    }

    public static <T> String getInsertUsingSqlPrefix(T object, String sTbName, List<Field> fieldList, DynamicNameStrategy<T> dynamicTbNameStrategy, Map<String, Object> map) {
        // 根据是否为TAG字段做分组
        Pair<List<Field>, List<Field>> fieldsPair = differentiateByTag(fieldList);
        // 获取TAGS字段名称&对应的值
        String tagFieldSql = getTagFieldNameAndValuesSql(object, fieldsPair.getKey(), map, true);
        // 获取普通字段的名称
        String commFieldSql = TdSqlUtil.joinColumnNamesWithBracket(fieldsPair.getValue());
        // 根据策略生成表名(传入实体对象以支持基于数据的命名)
        return SqlConstant.INSERT_INTO + dynamicTbNameStrategy.dynamicTableName(object, sTbName)
                + TdSqlConstant.USING + sTbName + tagFieldSql + commFieldSql + SqlConstant.VALUES;
    }


    public static <T> Pair<String, Map<String, Object>> getFinalInsertUsingSql(T object, List<Field> fieldList, String sTbName, DynamicNameStrategy<T> dynamicTbNameStrategy) {
        Map<String, Object> paramsMap = new HashMap<>(fieldList.size());

        // 根据是否为TAG字段做分组
        Pair<List<Field>, List<Field>> fieldsPair = differentiateByTag(fieldList);

        // 获取TAGS字段相关SQL
        String tagFieldSql = getTagFieldNameAndValuesSql(object, fieldsPair.getKey(), paramsMap, true);
        // 获取普通字段相关SQL
        String commFieldSql = getTagFieldNameAndValuesSql(object, fieldsPair.getValue(), paramsMap, false);

        // 根据策略生成表名(传入实体对象以支持基于数据的命名)
        String childTbName = dynamicTbNameStrategy.dynamicTableName(object, sTbName);

        // 拼接最终SQL
        String finalSql = SqlConstant.INSERT_INTO + childTbName + TdSqlConstant.USING + sTbName + tagFieldSql + commFieldSql;

        return Pair.of(finalSql, paramsMap);
    }

    /**
     * 按是否有Tag注解区分Field
     *
     * @param fieldList 所有字段列表
     * @return {@link Pair }<{@link List }<{@link Field }> Tag字段, {@link List }<{@link Field }>> 非Tag字段
     */
    public static Pair<List<Field>, List<Field>> differentiateByTag(List<Field> fieldList) {
        Map<Boolean, List<Field>> fieldGroups = fieldList.stream().collect(Collectors.partitioningBy(field -> field.isAnnotationPresent(TdTag.class)));
        List<Field> tagFields = fieldGroups.get(Boolean.TRUE);
        List<Field> commFields = fieldGroups.get(Boolean.FALSE);
        return Pair.of(tagFields, commFields);
    }


    public static String getTagFieldNameAndValuesSql(Object object, List<Field> fields, Map<String, Object> paramsMap, boolean isTag) {
        if (CollectionUtils.isEmpty(fields)) {
            return StrUtil.EMPTY;
        }

        List<String> fieldValueParamNames = new ArrayList<>();
        String fieldNameStr = fields.stream().map(field -> {
            String fieldName = field.getName();
            fieldValueParamNames.add(field.getName());
            paramsMap.put(fieldName, ReflectUtil.getFieldValue(object, field));
            return StrUtil.toUnderlineCase(fieldName);
        }).collect(TdSqlUtil.getColumnWithBracketCollector());

        String fieldValueParamsStr = fieldValueParamNames.stream().map(item -> ":" + item).collect(TdSqlUtil.getColumnWithBracketCollector());
        return fieldNameStr + (isTag ? TdSqlConstant.TAGS : SqlConstant.VALUES) + fieldValueParamsStr;
    }

    public static String getFieldTypeAndLength(Field field) {
        TdColumn tdField = field.getAnnotation(TdColumn.class);
        TdFieldTypeEnum type = null == tdField ? getColumnTypeByField(field) : tdField.type();
        if (type.isNeedLengthLimit()) {
            if (tdField == null || tdField.length() <= 0) {
                log.warn("Field [{}] has no length limit.", field.getName());
                throw new TdOrmException(TdOrmExceptionCode.FIELD_NO_LENGTH);
            }
            int length = tdField.length();
            return type.getFiledType() + SqlConstant.LEFT_BRACKET + length + SqlConstant.RIGHT_BRACKET;
        }
        return type.getFiledType();
    }

    private static TdFieldTypeEnum getColumnTypeByField(Field field) {
        Class<?> fieldType = field.getType();
        TdFieldTypeEnum tdFieldTypeEnum = TdFieldTypeEnum.matchByFieldType(fieldType);
        if (null == tdFieldTypeEnum) {
            throw new TdOrmException(TdOrmExceptionCode.CANT_NOT_MATCH_FIELD_TYPE);
        }

        return tdFieldTypeEnum;
    }

    public static String buildCreateColumn(List<Field> fields, Field primaryTsField) {
        fields.remove(primaryTsField);

        String tsColumn = primaryTsField == null ? StrUtil.EMPTY
                : SqlConstant.HALF_ANGLE_DASH
                + TdSqlUtil.getColumnName(primaryTsField)
                + SqlConstant.HALF_ANGLE_DASH
                + SqlConstant.BLANK
                + TdFieldTypeEnum.TIMESTAMP.getFiledType()
                + SqlConstant.COMMA;

        StringBuilder finalSb = new StringBuilder(SqlConstant.LEFT_BRACKET).append(tsColumn);
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);

            // 组装 字段名称 类型(长度)
            finalSb.append(SqlConstant.HALF_ANGLE_DASH)
                    .append(TdSqlUtil.getColumnName(field))
                    .append(SqlConstant.HALF_ANGLE_DASH)
                    .append(SqlConstant.BLANK)
                    .append(TdSqlUtil.getFieldTypeAndLength(field));

            // 最后一个不用➕逗号
            if (i != fields.size() - 1) {
                finalSb.append(SqlConstant.COMMA);
            }
        }
        // 首位必须是Timestamp字段
        return finalSb.append(SqlConstant.RIGHT_BRACKET).toString();
    }


    /**
     * 获取非Tag字段列表
     *
     * @param clazz clazz
     * @return {@link List }<{@link Field }>
     */
    public static List<Field> getNoTagFieldList(Class<?> clazz) {
        return ClassUtil.getAllFields(clazz, field -> !AnnotationUtil.hasAnnotation(field, TdTag.class));
    }


    /**
     * 检查是否有且只有一个名称为Ts的字段
     *
     * @param fieldList 待检查的字段列表
     * @return {@link Field }
     */
    public static Field checkPrimaryTsField(List<Field> fieldList) {
        List<Field> tsFieldList = fieldList.stream()
                .filter(field -> TdColumnConstant.TS.equals(TdSqlUtil.getColumnName(field)))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tsFieldList)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_PRIMARY_TS);
        }
        return tsFieldList.get(0);
    }

    public static String buildAggregationFunc(TdSelectFuncEnum tdSelectFuncEnum, String columnName, String aliasName) {
        return StrUtil.format(tdSelectFuncEnum.getFunc(), columnName, aliasName);
    }
}
