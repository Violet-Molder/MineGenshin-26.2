package com.linweiyun.genshin.core.status;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * 叠加实例基类 —— "可以被存进 StatusContainer 的东西"的统一规格
 *
 * 通俗地说：快递柜能放的包裹的最小要求——必须有过期判断、能让时间流逝。
 * 桥接层完全不关心具体是什么（元素附着 / 通用 buff / 以后的任何状态），
 * 它只知道"这个东西得告诉容器自己该不该被清掉"。
 *
 * 子类（比如 ElementalAttachmentInstance）必须实现：
 *   tick()        每次宿主 tick 时让生命周期流逝
 *   isFinished()  告诉容器自己是否该被移除
 *   getTypeId()   序列化恢复用的类型标识（由 typeId 字段返回）
 *   copy()        深拷贝
 *   onRemove()    可选钩子，被移除时回调（默认空实现）
 */
public class StatusInstance implements IPersistedSerializable {

    /** 序列化恢复用的类型标识，子类构造时设置 */
    @Persisted(key = "type_id")
    protected String typeId = "";

    /** 序列化恢复用的类型标识，子类自己决定返回什么字符串 */
    public String getTypeId() { return typeId; }

    /** 每次宿主 tick 时调用，让生命周期流逝（倒计时或量值衰减） */
    public void tick(){};

    /** 告诉容器自己是否已经结束（该被移除了） */
    public boolean isFinished() {
        return false;
    };

    /** 被容器移除时的回调钩子（自然结束或强制清除都会调），默认空实现 */
    public void onRemove() {}

    /** 深拷贝 */
    public StatusInstance copy() {
        return this;
    }
}