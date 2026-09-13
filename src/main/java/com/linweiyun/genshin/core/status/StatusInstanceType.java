package com.linweiyun.genshin.core.status;

import java.util.function.Supplier;

/**
 * StatusInstance 子类的注册表条目
 * <p>
 * 用法示例：
 * <pre>{@code
 * StatusInstanceTypes.register(new StatusInstanceType<>("elemental_attachment", ElementalAttachmentInstance::new));
 * }</pre>
 * <p>
 * 之后添加新的 StatusInstance 子类只需：
 * 1. 继承 StatusInstance，在字段上添加 @Persisted 注解
 * 2. 在无参构造中设置 typeId
 * 3. 调用 StatusInstanceTypes.register 注册
 * <p>
 * 序列化/反序列化由 StatusContainer 自动根据 typeId 调度，无需额外编写序列化代码。
 */
public record StatusInstanceType<T extends StatusInstance>(
        String typeId,
        Supplier<T> constructor
) {}