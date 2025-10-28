package com.zephyrcicd.tdengineorm.wrapper;

import com.zephyrcicd.tdengineorm.enums.SelectJoinSymbolEnum;
import lombok.RequiredArgsConstructor;

/**
 * @author Zephyr
 */
@RequiredArgsConstructor
public class SelectCalcSymbol<T> extends AbstractSelectCalc {

    private final SelectCalcWrapper<T> selectCalcWrapper;

    public SelectCalcWrapper<T> operate(SelectJoinSymbolEnum selectJoinSymbolEnum) {
        selectCalcWrapper.operate(selectJoinSymbolEnum);
        return this.selectCalcWrapper;
    }

    public SelectCalcWrapper<T> setFinalColumnAliasName(String aliasName) {
        selectCalcWrapper.setFinalColumnAliasName(aliasName);
        return selectCalcWrapper;
    }

}
