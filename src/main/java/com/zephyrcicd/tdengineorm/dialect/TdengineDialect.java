package com.zephyrcicd.tdengineorm.dialect;

import org.springframework.data.jdbc.repository.config.DialectResolver;
import org.springframework.data.relational.core.dialect.AbstractDialect;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.LockOptions;
import org.springframework.util.Assert;

/**
 * TDengine 数据库方言
 *
 * <p>TDengine 作为时序数据库，具有以下特点：</p>
 * <ul>
 *     <li>支持 LIMIT 语法进行分页</li>
 *     <li>不支持悲观锁（FOR UPDATE、LOCK IN SHARE MODE）</li>
 *     <li>标识符使用反引号包围</li>
 *     <li>标识符默认不区分大小写</li>
 * </ul>
 *
 * <p>此类实现了 {@link Dialect} 接口，可以被 Spring Data JDBC 的
 * {@link DialectResolver} 识别和使用。</p>
 *
 * @author Zephyr
 * @since 1.2.1
 */
public class TdengineDialect extends AbstractDialect implements Dialect {

    /**
     * TDengine 标识符处理规则：使用反引号，不区分大小写
     */
    public static final IdentifierProcessing TDENGINE_IDENTIFIER_PROCESSING = IdentifierProcessing.create(
            new IdentifierProcessing.Quoting("`"),
            IdentifierProcessing.LetterCasing.LOWER_CASE
    );

    /**
     * 单例实例
     */
    public static final TdengineDialect INSTANCE = new TdengineDialect();

    private final IdentifierProcessing identifierProcessing;

    /**
     * 使用默认标识符处理规则创建方言实例
     */
    protected TdengineDialect() {
        this(TDENGINE_IDENTIFIER_PROCESSING);
    }

    /**
     * 使用自定义标识符处理规则创建方言实例
     *
     * @param identifierProcessing 标识符处理规则，不能为 null
     */
    public TdengineDialect(IdentifierProcessing identifierProcessing) {
        Assert.notNull(identifierProcessing, "IdentifierProcessing must not be null");
        this.identifierProcessing = identifierProcessing;
    }

    /**
     * TDengine LIMIT 子句实现
     *
     * <p>TDengine 支持以下 LIMIT 语法：</p>
     * <ul>
     *     <li>LIMIT count</li>
     *     <li>LIMIT count OFFSET offset</li>
     *     <li>LIMIT offset, count（兼容 MySQL 语法）</li>
     * </ul>
     */
    private static final LimitClause LIMIT_CLAUSE = new LimitClause() {

        @Override
        public String getLimit(long limit) {
            return "LIMIT " + limit;
        }

        @Override
        public String getOffset(long offset) {
            // TDengine 必须同时指定 LIMIT 和 OFFSET
            // 使用最大值作为 LIMIT 的替代方案
            return String.format("LIMIT 9223372036854775807 OFFSET %d", offset);
        }

        @Override
        public String getLimitOffset(long limit, long offset) {
            // TDengine 支持 LIMIT count OFFSET offset 语法
            return String.format("LIMIT %d OFFSET %d", limit, offset);
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    /**
     * TDengine LOCK 子句实现
     *
     * <p>TDengine 作为时序数据库，不支持传统关系型数据库的行锁机制：</p>
     * <ul>
     *     <li>不支持 FOR UPDATE（悲观写锁）</li>
     *     <li>不支持 LOCK IN SHARE MODE（悲观读锁）</li>
     *     <li>所有锁模式都返回空字符串</li>
     * </ul>
     *
     * <p>时序数据库的设计理念强调高并发写入和查询性能，
     * 通常采用乐观并发控制或无锁设计，不需要传统的行级锁。</p>
     */
    private static final LockClause LOCK_CLAUSE = new LockClause() {

        @Override
        public String getLock(LockOptions lockOptions) {
            // TDengine 不支持锁机制，所有锁模式都返回空字符串
            return "";
        }

        @Override
        public Position getClausePosition() {
            return Position.AFTER_ORDER_BY;
        }
    };

    @Override
    public LimitClause limit() {
        return LIMIT_CLAUSE;
    }

    @Override
    public LockClause lock() {
        return LOCK_CLAUSE;
    }

    @Override
    public IdentifierProcessing getIdentifierProcessing() {
        return identifierProcessing;
    }
}