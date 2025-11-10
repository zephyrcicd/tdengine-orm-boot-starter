package com.zephyrcicd.tdengineorm.wrapper;

import com.zephyrcicd.tdengineorm.annotation.TdTable;
import com.zephyrcicd.tdengineorm.constant.SqlConstant;
import com.zephyrcicd.tdengineorm.constant.TdSqlConstant;
import com.zephyrcicd.tdengineorm.enums.TdWrapperTypeEnum;
import com.zephyrcicd.tdengineorm.util.FieldUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Zephyr
 */
@NoArgsConstructor
public abstract class AbstractTdWrapper<T> {

    protected StringBuilder finalSql = new StringBuilder();
    protected String tbName;
    protected StringBuilder where = new StringBuilder();
    protected AtomicInteger paramNameSeq;
    @Getter
    @Setter
    private Class<T> entityClass;
    @Getter
    @Setter
    private Map<String, Object> paramsMap = new HashMap<>(16);
    /**
     * 当前层, 最内层为0, 向上递增
     */
    protected int layer;


    public AbstractTdWrapper(Class<T> entityClass) {
        this.entityClass = entityClass;
        initTbName();
    }

    protected abstract TdWrapperTypeEnum type();


    protected void buildFrom(StringBuilder sql) {
        sql.append(SqlConstant.FROM).append(tbName).append(SqlConstant.BLANK);
    }

    protected void initTbName() {
        String name = null;
        TdTable annotation = entityClass.getAnnotation(TdTable.class);
        if (annotation != null && StringUtils.hasText(annotation.value())) {
            name = annotation.value();
        }
        if (!StringUtils.hasText(name)) {
            name = FieldUtil.toUnderlineCase(entityClass.getSimpleName());
        }
        tbName = name;
    }

    protected Integer getParamNameSeq() {
        if (paramNameSeq == null) {
            paramNameSeq = new AtomicInteger(0);
            return paramNameSeq.getAndIncrement();
        }
        return paramNameSeq.incrementAndGet();
    }

    protected String genParamName() {
        return TdSqlConstant.MAP_PARAM_NAME_PREFIX + layer + "_" + getParamNameSeq();
    }
}
