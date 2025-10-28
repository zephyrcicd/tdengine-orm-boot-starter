package com.zephyrcicd.tdengineorm.wrapper;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.enums.JoinTypeEnum;
import com.zephyrcicd.tdengineorm.enums.TdSelectFuncEnum;
import com.zephyrcicd.tdengineorm.enums.TdWindFuncTypeEnum;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.func.GetterFunction;
import com.zephyrcicd.tdengineorm.util.AssertUtil;
import com.zephyrcicd.tdengineorm.util.ClassUtil;
import com.zephyrcicd.tdengineorm.util.LambdaUtil;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tdengine查询包装
 *
 * @author Zephyr
 * @date 2024/05/11
 */
public class TdQueryWrapper<T> extends AbstractTdQueryWrapper<T> {

    public TdQueryWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    public TdQueryWrapper<T> selectAll() {
        doSelectAll();
        return this;
    }

    public TdQueryWrapper<T> selectAll(Class<?> selectClass) {
        List<Field> allFields = ClassUtil.getAllFields(selectClass);
        AssertUtil.notEmpty(allFields, new TdOrmException(TdOrmExceptionCode.NO_FILED));
        String[] columnNames = allFields.stream().map(TdSqlUtil::getColumnName).toArray(String[]::new);
        addColumnNames(columnNames);
        return this;
    }


    public <R> TdQueryWrapper<T> selectAlias(String columnName, GetterFunction<R, ?> aliasGetter) {
        String column = StrUtil.format("{} {}", columnName, LambdaUtil.getUnderlineFieldNameByGetter(aliasGetter));

        String[] columns = {column};
        addColumnNames(columns);
        return this;
    }

    public TdQueryWrapper<T> select(String... columnNames) {
        addColumnNames(columnNames);
        return this;
    }

    public TdQueryWrapper<T> select(GetterFunction<T, ?>... getterFuncArray) {
        if (ArrayUtil.isEmpty(getterFuncArray)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_SELECT);
        }

        String[] fieldNameArray = Arrays.stream(getterFuncArray)
                .map(getter -> tbName + SqlConstant.DOT + TdSqlUtil.getColumnName(getEntityClass(), getter))
                .toArray(String[]::new);
        addColumnNames(fieldNameArray);
        return this;
    }

    public <R> TdQueryWrapper<T> select(Class<R> clazz, GetterFunction<R, ?>... getterFuncArray) {
        if (ArrayUtil.isEmpty(getterFuncArray)) {
            throw new TdOrmException(TdOrmExceptionCode.NO_SELECT);
        }

        String getterTbName = TdSqlUtil.getTbName(clazz);
        String[] fieldNameArray = Arrays.stream(getterFuncArray)
                .map(getter -> getterTbName + SqlConstant.DOT + TdSqlUtil.getColumnName(clazz, getter))
                .toArray(String[]::new);
        addColumnNames(fieldNameArray);
        return this;
    }

    public TdQueryWrapper<T> selectCalc(String aliasColumnName, Consumer<SelectCalcWrapper<T>> consumer) {
        SelectCalcWrapper<T> selectCalcWrapper = new SelectCalcWrapper<>(getEntityClass());
        consumer.accept(selectCalcWrapper);
        selectCalcWrapper.setFinalColumnAliasName(aliasColumnName);
        super.selectCalcWrapper = selectCalcWrapper;
        return this;
    }

    public TdQueryWrapper<T> selectCalc(GetterFunction<T, ?> aliasColumnFunc, Consumer<SelectCalcWrapper<T>> consumer) {
        return selectCalc(getColumnName(aliasColumnFunc), consumer);
    }

    public <R> TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, String columnName, GetterFunction<R, ?> aliasColumnName) {
        addColumnName(TdSqlUtil.buildAggregationFunc(selectFuncEnum, columnName, LambdaUtil.getUnderlineFieldNameByGetter(aliasColumnName)));
        return this;
    }

    public TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, String... columnNames) {
        String[] array = Arrays.stream(columnNames)
                .map(columnName -> TdSqlUtil.buildAggregationFunc(selectFuncEnum, columnName, columnName))
                .toArray(String[]::new);
        addColumnNames(array);
        return this;
    }

    public TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, String columnName) {
        addColumnName(TdSqlUtil.buildAggregationFunc(selectFuncEnum, columnName, columnName));
        return this;
    }

    public TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, String columnName, String aliasColumnName) {
        addColumnName(TdSqlUtil.buildAggregationFunc(selectFuncEnum, columnName, aliasColumnName));
        return this;
    }

    public final TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, GetterFunction<T, ?> getterFunc, String aliasColumnName) {
        String columName = TdSqlUtil.buildAggregationFunc(selectFuncEnum, getColumnName(getterFunc), aliasColumnName);
        addColumnName(columName);
        return this;
    }

    public TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, GetterFunction<T, ?> column) {
        String columnName = getColumnName(column);
        selectFunc(selectFuncEnum, columnName);
        return this;
    }

    public <R> TdQueryWrapper<T> selectFunc(TdSelectFuncEnum selectFuncEnum, GetterFunction<T, ?> column, GetterFunction<R, ?> aliasColumn) {
        selectFunc(selectFuncEnum, getColumnName(column), TdSqlUtil.getColumnName(LambdaUtil.getEntityClass(aliasColumn), aliasColumn));
        return this;
    }


    public TdQueryWrapper<T> eq(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.EQUAL);
        return this;
    }


    public TdQueryWrapper<T> eq(GetterFunction<T, ?> getterFunc, Object value) {
        return eq(getColumnName(getterFunc), value);
    }


    public TdQueryWrapper<T> eq(boolean condition, String columnName, Object value) {
        if (condition) {
            addWhereParam(value, columnName, genParamName(), SqlConstant.EQUAL);
        }
        return this;
    }


    public TdQueryWrapper<T> eq(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? eq(getColumnName(getterFunc), value) : this;
    }

    public TdQueryWrapper<T> and() {
        this.where.append(SqlConstant.BLANK).append(SqlConstant.AND).append(SqlConstant.BLANK);
        return this;
    }

    // public TdQueryWrapper<T> and(Consumer<TdQueryWrapper<T>> consumer) {
    //     consumer.accept(this);
    //     return this;
    // }

    public TdQueryWrapper<T> or(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.OR);
        return this;
    }

    public TdQueryWrapper<T> or(boolean condition, String columnName, Object value) {
        if (condition) {
            addWhereParam(value, columnName, genParamName(), SqlConstant.OR);
        }
        return this;
    }

    public TdQueryWrapper<T> ne(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.NE);
        return this;
    }

    public TdQueryWrapper<T> ne(GetterFunction<T, ?> getterFunc, Object value) {
        return ne(getColumnName(getterFunc), value);
    }

    public TdQueryWrapper<T> ne(boolean condition, String columnName, Object value) {
        return condition ? ne(columnName, value) : this;
    }

    public TdQueryWrapper<T> ne(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? ne(getColumnName(getterFunc), value) : this;
    }


    public TdQueryWrapper<T> notNull(String columnName) {
        doNotNull(columnName);
        return this;
    }

    public TdQueryWrapper<T> isNull(String columnName) {
        doIsNull(columnName);
        return this;
    }

    public TdQueryWrapper<T> isNull(boolean condition, String columnName) {
        return condition ? isNull(columnName) : this;
    }

    public TdQueryWrapper<T> isNull(GetterFunction<T, ?> getterFunc) {
        return isNull(getColumnName(getterFunc));
    }


    public TdQueryWrapper<T> isNull(boolean condition, GetterFunction<T, ?> getterFunc) {
        return condition ? isNull(getColumnName(getterFunc)) : this;
    }

    public TdQueryWrapper<T> notNull(boolean condition, String columnName) {
        return condition ? notNull(columnName) : this;
    }

    public TdQueryWrapper<T> notNull(GetterFunction<T, ?> getterFunc) {
        return notNull(getColumnName(getterFunc));
    }

    public TdQueryWrapper<T> notNull(boolean condition, GetterFunction<T, ?> getterFunc) {
        return condition ? notNull(getColumnName(getterFunc)) : this;
    }

    public TdQueryWrapper<T> stateWindow(String column) {
        doWindowFunc(TdWindFuncTypeEnum.STATE_WINDOW, column);
        return this;
    }


    public TdQueryWrapper<T> orderByAsc(String columnName) {
        if (StrUtil.isNotBlank(orderBy)) {
            orderBy.append(SqlConstant.COMMA);
            orderBy.append(columnName);
        } else {
            orderBy.append(SqlConstant.ORDER_BY).append(columnName);
        }
        return this;
    }


    public TdQueryWrapper<T> orderByDesc(String columnName) {
        if (StrUtil.isNotBlank(orderBy)) {
            orderBy.append(SqlConstant.COMMA);
            orderBy.append(columnName);
        } else {
            orderBy.append(SqlConstant.ORDER_BY)
                    .append(columnName)
                    .append(SqlConstant.BLANK)
                    .append(SqlConstant.DESC);
        }
        return this;
    }

    public TdQueryWrapper<T> orderByAsc(GetterFunction<T, ?> getterFunc) {
        return orderByAsc(getColumnName(getterFunc));
    }


    public <R> TdQueryWrapper<T> orderByDesc(Class<R> clazz, GetterFunction<R, ?> getterFunc) {
        return orderByDesc(TdSqlUtil.getColumnName(clazz, getterFunc));
    }

    public <R> TdQueryWrapper<T> orderByAsc(Class<R> clazz, GetterFunction<R, ?> getterFunc) {
        return orderByAsc(TdSqlUtil.getColumnName(clazz, getterFunc));
    }

    public TdQueryWrapper<T> orderByDesc(GetterFunction<T, ?> getterFunc) {
        return orderByDesc(getColumnName(getterFunc));
    }

    public TdQueryWrapper<T> intervalWindow(String interval) {
        doWindowFunc(TdWindFuncTypeEnum.INTERVAL, interval);
        return this;
    }


    public TdQueryWrapper<T> stateWindow(GetterFunction<T, ?> getterFunc) {
        return stateWindow(getColumnName(getterFunc));
    }


    public TdQueryWrapper<T> in(String columnName, Object... valueArray) {
        doIn(columnName, valueArray);
        return this;
    }

    public TdQueryWrapper<T> in(GetterFunction<T, ?> column, Object... valueArray) {
        doIn(getColumnName(column), valueArray);
        return this;
    }

    public TdQueryWrapper<T> in(boolean condition, String columnName, Object... valueArray) {
        return condition ? in(columnName, valueArray) : this;
    }

    public TdQueryWrapper<T> in(boolean condition, GetterFunction<T, ?> column, Object... valueArray) {
        return condition ? in(column, valueArray) : this;
    }

    public TdQueryWrapper<T> notIn(String columnName, Object... valueArray) {
        doNotIn(columnName, valueArray);
        return this;
    }

    public TdQueryWrapper<T> notIn(GetterFunction<T, ?> column, Object... valueArray) {
        doNotIn(getColumnName(column), valueArray);
        return this;
    }

    public TdQueryWrapper<T> notIn(boolean condition, String columnName, Object... valueArray) {
        return condition ? in(columnName, valueArray) : this;
    }

    public TdQueryWrapper<T> notIn(boolean condition, GetterFunction<T, ?> column, Object... valueArray) {
        return condition ? notIn(column, valueArray) : this;
    }

    public TdQueryWrapper<T> innerQueryWrapper(Consumer<TdQueryWrapper<T>> innerQueryWrapperConsumer) {
        TdQueryWrapper<T> innerWrapper = TdWrappers.queryWrapper(getEntityClass());
        innerQueryWrapperConsumer.accept(innerWrapper);
        doInnerWrapper(innerWrapper);
        return this;
    }

    public TdQueryWrapper<T> limit(int count) {
        doLimit(SqlConstant.LIMIT + count);
        return this;
    }

    /**
     * 分页
     *
     * @param pageNo   页码, 起始为1
     * @param pageSize 页大小
     * @return {@link TdQueryWrapper}<{@link T}>
     */
    public TdQueryWrapper<T> limit(long pageNo, long pageSize) {
        doLimit(pageNo, pageSize);
        return this;
    }

    public TdQueryWrapper<T> lt(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.LT);
        return this;
    }

    public TdQueryWrapper<T> lt(GetterFunction<T, ?> getterFunc, Object value) {
        return lt(getColumnName(getterFunc), value);
    }

    public TdQueryWrapper<T> lt(boolean condition, String columnName, Object value) {
        return condition ? lt(columnName, value) : this;
    }

    public TdQueryWrapper<T> lt(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? lt(getterFunc, value) : this;
    }


    public TdQueryWrapper<T> le(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.LE);
        return this;
    }

    public TdQueryWrapper<T> le(GetterFunction<T, ?> getterFunc, Object value) {
        return le(getColumnName(getterFunc), value);
    }

    public TdQueryWrapper<T> le(boolean condition, String columnName, Object value) {
        return condition ? le(columnName, value) : this;
    }

    public TdQueryWrapper<T> le(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? le(getterFunc, value) : this;
    }

    public TdQueryWrapper<T> gt(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.GE);
        return this;
    }

    public TdQueryWrapper<T> gt(GetterFunction<T, ?> getterFunc, Object value) {
        return gt(getColumnName(getterFunc), value);
    }


    public TdQueryWrapper<T> gt(boolean condition, String columnName, Object value) {
        return condition ? gt(columnName, value) : this;
    }

    public TdQueryWrapper<T> gt(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? gt(getterFunc, value) : this;
    }

    public TdQueryWrapper<T> ge(String columnName, Object value) {
        addWhereParam(value, columnName, genParamName(), SqlConstant.GT);
        return this;
    }

    public TdQueryWrapper<T> ge(GetterFunction<T, ?> getterFunc, Object value) {
        return ge(getColumnName(getterFunc), value);
    }

    public TdQueryWrapper<T> ge(boolean condition, String columnName, Object value) {
        return condition ? ge(columnName, value) : this;
    }

    public TdQueryWrapper<T> ge(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? ge(getterFunc, value) : this;
    }

    public TdQueryWrapper<T> like(String columnName, Object value) {
        addWhereParam(StrUtil.format("%{}%", value), columnName, genParamName(), SqlConstant.LIKE);
        return this;
    }

    public TdQueryWrapper<T> like(GetterFunction<T, ?> getterFunc, Object value) {
        return like(getColumnName(getterFunc), value);
    }

    public TdQueryWrapper<T> like(boolean condition, String columnName, Object value) {
        return condition ? like(columnName, value) : this;
    }

    public TdQueryWrapper<T> like(boolean condition, GetterFunction<T, ?> getterFunc, Object value) {
        return condition ? like(getterFunc, value) : this;
    }


    /**
     * 左闭右闭区间范围
     *
     * @param columnName 列名
     * @param leftValue  左区间值
     * @param rightValue 右区间值
     * @return {@link TdQueryWrapper }<{@link T }>
     */
    public TdQueryWrapper<T> between(String columnName, Object leftValue, Object rightValue) {
        if (StrUtil.isNotBlank(where)) {
            where.append(SqlConstant.AND);
        }

        String leftParamName = genParamName();
        String rightParamName = genParamName();

        where
                .append(SqlConstant.LEFT_BRACKET)
                .append(columnName)
                .append(SqlConstant.LE)
                .append(SqlConstant.COLON)
                .append(leftParamName)
                .append(SqlConstant.AND)
                .append(columnName)
                .append(SqlConstant.GT)
                .append(SqlConstant.COLON)
                .append(rightParamName)
                .append(SqlConstant.RIGHT_BRACKET);

        getParamsMap().put(leftParamName, leftValue);
        getParamsMap().put(rightParamName, rightValue);
        return this;
    }

    public TdQueryWrapper<T> between(boolean condition, String columnName, Object leftValue, Object rightValue) {
        return condition ? between(columnName, leftValue, rightValue) : this;
    }

    /**
     * 左闭右闭区间范围
     *
     * @param getterFunc get方法
     * @param leftValue  左区间值
     * @param rightValue 右区间值
     * @return {@link TdQueryWrapper }<{@link T }>
     */
    public TdQueryWrapper<T> between(GetterFunction<T, ?> getterFunc, Object leftValue, Object rightValue) {
        return between(getColumnName(getterFunc), leftValue, rightValue);
    }

    public TdQueryWrapper<T> between(boolean condition, GetterFunction<T, ?> getterFunc, Object leftValue, Object rightValue) {
        return condition ? between(getterFunc, leftValue, rightValue) : this;
    }


    public TdQueryWrapper<T> groupBy(String columnName) {
        if (StrUtil.isNotBlank(groupBy)) {
            throw new TdOrmException(TdOrmExceptionCode.MULTI_GROUP_BY);
        }
        groupBy = SqlConstant.GROUP_BY + columnName + SqlConstant.BLANK;
        return this;
    }


    public TdQueryWrapper<T> groupBy(GetterFunction<T, ?> getterFunction) {
        return groupBy(getColumnName(getterFunction));
    }


    /**
     * 连表查询
     * 根据TDengine目前的规范，连表查询仅支持主键使用“=”关联
     *
     * @param joinType       连接类型
     * @param joinTableClass 连接表类
     * @return {@link TdQueryWrapper }<{@link T }>
     */
    public <R> TdQueryWrapper<T> join(JoinTypeEnum joinType, Class<R> joinTableClass) {
        String joinTbName = TdSqlUtil.getTbName(joinTableClass);
        joinQueryEntityList.add(JoinQuery.builder()
                .joinType(joinType)
                .joinTableName(joinTbName)
                .joinOnSql(joinTbName + SqlConstant.DOT + "ts" + SqlConstant.EQUAL + tbName + SqlConstant.DOT + "ts")
                .build());

        return this;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static class JoinQuery {
        private int index;
        private JoinTypeEnum joinType;
        private String joinTableName;
        private String joinOnSql;
    }

}
