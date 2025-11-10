package com.zephyrcicd.tdengineorm.wrapper;

import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.enums.TdSelectFuncEnum;
import com.zephyrcicd.tdengineorm.exception.TdOrmException;
import com.zephyrcicd.tdengineorm.exception.TdOrmExceptionCode;
import com.zephyrcicd.tdengineorm.util.AssertUtil;
import com.zephyrcicd.tdengineorm.util.TdSqlUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Zephyr
 */
@RequiredArgsConstructor
public abstract class AbstractSelectCalc {
    protected final List<SelectCalcWrapper.SelectColumn> selectColumnList = new ArrayList<>(16);

    protected String getFinalColumnAliasName() {
        return "";
    }

    public String getFinalSelectSql() {
        if (CollectionUtils.isEmpty(selectColumnList)) {
            return "";
        }
        AssertUtil.notBlank(getFinalColumnAliasName(), new TdOrmException(TdOrmExceptionCode.COLUMN_NO_ALIAS_NAME));
        StringBuilder finalSelectColumn = new StringBuilder(SqlConstant.LEFT_BRACKET);
        selectColumnList.forEach(selectColumn -> {
            TdSelectFuncEnum selectFuncEnum = selectColumn.getSelectFuncEnum();
            finalSelectColumn.append(selectFuncEnum == null ? selectColumn.getColumnName()
                    : TdSqlUtil.buildAggregationFunc(selectFuncEnum, selectColumn.getColumnName(), ""));

            if (null != selectColumn.getSelectJoinSymbolSuffix()) {
                finalSelectColumn.append(selectColumn.getSelectJoinSymbolSuffix().getKey());
            }
        });

        return finalSelectColumn.append(SqlConstant.RIGHT_BRACKET).append(getFinalColumnAliasName()).toString();
    }
}
