package com.zephyrcicd.tdengineorm.wrapper;

import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.enums.TdWindFuncTypeEnum;
import com.zephyrcicd.tdengineorm.enums.TdWrapperTypeEnum;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.func.GetterFunction;
import com.zephyrcicd.tdengineorm.util.AssertUtil;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Zephyr
 */
public abstract class AbstractTdQueryWrapper<T> extends AbstractTdWrapper<T> {

    protected String limit;
    protected String groupBy;
    protected String partitionBy;
    protected String[] selectColumnNames;
    protected String windowFunc;
    protected SelectCalcWrapper<T> selectCalcWrapper;
    protected final StringBuilder orderBy = new StringBuilder();
    protected List<TdQueryWrapper.JoinQuery> joinQueryEntityList = new ArrayList<>();
    /**
     * 内层Wrapper对象
     */
    protected AbstractTdQueryWrapper<T> innerQueryWrapper;

    public AbstractTdQueryWrapper(Class<T> entityClass) {
        super(entityClass);
    }

    @Override
    protected TdWrapperTypeEnum type() {
        return TdWrapperTypeEnum.QUERY;
    }

    @Override
    protected void buildFrom(StringBuilder sql) {
        if (innerQueryWrapper != null) {
            String innerSql = innerQueryWrapper.getSql();
            this.tbName = " (" + innerSql + ") t" + layer + SqlConstant.BLANK;
        }
        super.buildFrom(sql);
    }

    public String getSql() {
        StringBuilder sql = new StringBuilder();
        buildSelect(sql);
        buildFrom(sql);
        joinQueryEntityList.forEach(joinQueryEntity -> {
            sql
                    .append(joinQueryEntity.getJoinType().getSql())
                    .append(joinQueryEntity.getJoinTableName())
                    .append(SqlConstant.ON)
                    .append(joinQueryEntity.getJoinOnSql())
                    .append(SqlConstant.BLANK);
        });

        if (where.length() > 0) {
            sql.append(SqlConstant.WHERE).append(where);
        }
        if (StringUtils.hasText(partitionBy)) {
            sql.append(partitionBy);
        }
        if (StringUtils.hasText(windowFunc)) {
            sql.append(windowFunc);
        }
        if (StringUtils.hasText(groupBy)) {
            sql.append(groupBy);
        }
        if (orderBy.length() > 0) {
            sql.append(orderBy);
        }
        if (StringUtils.hasText(limit)) {
            sql.append(limit);
        }

        return sql.toString();
    }


    protected void doLimit(String limitCount) {
        limit = limitCount;
    }

    protected void doLimit(long pageNo, long pageSize) {
        limit = SqlConstant.LIMIT + (pageNo - 1) + SqlConstant.COMMA + pageSize;
    }

    private void buildSelect(StringBuilder sql) {
        sql.append(SqlConstant.SELECT);
        if ((selectColumnNames == null || selectColumnNames.length == 0) && selectCalcWrapper == null) {
            // 默认查询所有字段
            sql.append(SqlConstant.ALL);
            return;
        }

        if (selectColumnNames != null && selectColumnNames.length > 0) {
            for (int i = 1; i <= selectColumnNames.length; i++) {
                if (i > 1) {
                    sql.append(SqlConstant.COMMA);
                }
                sql.append(selectColumnNames[i - 1]);
            }
        }

        if (null != selectCalcWrapper) {
            sql.append(selectCalcWrapper.getFinalSelectSql());
        }
    }

    protected void doSelectAll() {
        selectColumnNames = new String[]{SqlConstant.ALL};
    }

    protected void doWindowFunc(TdWindFuncTypeEnum funcType, String winFuncValue) {
        Assert.isNull(windowFunc, "[TDengineQueryWrapper] 不可重复设置窗口函数");
        windowFunc = buildWindowFunc(funcType, winFuncValue);
    }

    protected String buildWindowFunc(TdWindFuncTypeEnum tdWindFuncTypeEnum, String winFuncValue) {
        // 窗口函数的内容不可用引号包括, 所以这里直接使用拼接的方式
        return tdWindFuncTypeEnum.getKey() + SqlConstant.LEFT_BRACKET
                + winFuncValue
                + SqlConstant.RIGHT_BRACKET;
    }

    /**
     * 设置 PARTITION BY 子句
     * PARTITION BY 可以和窗口函数一起使用，但不能和 GROUP BY 一起使用
     * 
     * @param columns 分区列，多个列用逗号分隔
     */
    protected void doPartitionBy(String columns) {
        partitionBy = " PARTITION BY " + columns + SqlConstant.BLANK;
    }


    protected void doInnerWrapper(AbstractTdQueryWrapper<T> innerWrapper) {
        // 限制最多调用一次
        AssertUtil.isTrue(layer == 0, new TdOrmException(TdOrmExceptionCode.SQL_LAYER_OUT_LIMITED));
        innerWrapper.layer = 1;
        this.getParamsMap().putAll(innerWrapper.getParamsMap());
        this.innerQueryWrapper = innerWrapper;
    }


    protected void addColumnName(String columnName) {
        if (selectColumnNames == null || selectColumnNames.length == 0) {
            selectColumnNames = new String[]{columnName};
            return;
        }
        List<String> newList = Arrays.stream(selectColumnNames).collect(Collectors.toList());
        newList.add(columnName);
        selectColumnNames = newList.toArray(new String[0]);
    }

    protected void addColumnNames(String[] columnNames) {
        if (columnNames == null || columnNames.length == 0) {
            return;
        }
        if (selectColumnNames == null || selectColumnNames.length == 0) {
            selectColumnNames = columnNames.clone();
            return;
        }
        String[] merged = new String[selectColumnNames.length + columnNames.length];
        System.arraycopy(selectColumnNames, 0, merged, 0, selectColumnNames.length);
        System.arraycopy(columnNames, 0, merged, selectColumnNames.length, columnNames.length);
        selectColumnNames = merged;
    }

    protected void addWhereParam(Object value, String columnName, String paramName, String symbol) {
        AssertUtil.notNull(value, new TdOrmException(TdOrmExceptionCode.PARAM_VALUE_CANT_NOT_BE_NULL));
        checkHasWhere();
        where
                .append(columnName)
                .append(symbol)
                .append(SqlConstant.COLON)
                .append(paramName)
                .append(SqlConstant.BLANK);
        if (null != value) {
            getParamsMap().put(paramName, value);
        }
    }

    private void checkHasWhere() {
        if (where.length() > 0) {
            where.append(SqlConstant.AND);
        }
    }

    protected String getColumnName(GetterFunction<T, ?> getterFunc) {
        return TdSqlUtil.getColumnName(getEntityClass(), getterFunc);
    }

    protected void doIn(String columnName, Object... valueArray) {
        checkHasWhere();

        Map<String, Object> paramsMap = getParamsMap();
        String finalInColumnsStr = Arrays.stream(valueArray)
                .map(value -> {
                    String paramName = genParamName();
                    paramsMap.put(paramName, value);
                    return SqlConstant.COLON + paramName;
                }).collect(TdSqlUtil.getParenthesisCollector());

        where
                .append(columnName)
                .append(SqlConstant.IN)
                .append(finalInColumnsStr)
                .append(SqlConstant.BLANK);
    }


    protected void doNotIn(String column, Object... valueArray) {
        if (where.length() > 0) {
            where.append(SqlConstant.AND);
        }

        Map<String, Object> paramsMap = getParamsMap();
        String finalInColumnsStr = Arrays.stream(valueArray)
                .map(value -> {
                    String paramName = genParamName();
                    paramsMap.put(paramName, value);
                    return SqlConstant.COLON + paramName;
                }).collect(TdSqlUtil.getParenthesisCollector());

        where
                .append(column)
                .append(SqlConstant.NOT_IN)
                .append(finalInColumnsStr)
                .append(SqlConstant.BLANK);
    }


    protected void doNotNull(String columnName) {
        if (where.length() > 0) {
            where.append(SqlConstant.AND);
        }
        where
                .append(columnName)
                .append(SqlConstant.IS_NOT_NULL);
    }

    protected void doIsNull(String columnName) {
        if (where.length() > 0) {
            where.append(SqlConstant.AND);
        }
        where
                .append(columnName)
                .append(SqlConstant.IS_NULL);
    }
}
