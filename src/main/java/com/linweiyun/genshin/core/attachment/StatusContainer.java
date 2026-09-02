package com.linweiyun.genshin.core.attachment;

import com.linweiyun.genshin.core.status.StatusInstance;
import com.linweiyun.genshin.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.genshin.core.system.about.FrozenDecayState;
import com.linweiyun.genshin.enums.ElementalsGIM;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
        * 状态容器 —— 某个宿主身上所有 StatusInstance 的集合
 *
         * 通俗地说：张三身上现在挂着哪些状态的清单。
        * 容器只存、取、tick 实例、清理过期——它完全不理解里面存的是什么。
        *
        * 依赖方向：桥接层（本类）不依赖任何元素附着层的类。
        * 元素附着层依赖桥接层（继承 StatusInstance、往本容器里 add 实例）。
        */
public class StatusContainer implements IPersistedSerializable {
    public static final Logger LOGGER = LogUtils.getLogger();
    public final static Codec<StatusContainer> CODEC = PersistedParser.createCodec(StatusContainer::new);
    public final static StreamCodec<ByteBuf, StatusContainer> STREAM_CODEC = PersistedParser.createStreamCodec(StatusContainer::new);

    @Persisted(key = "statuses")
    private List<StatusInstance> instances;

    /**
     * 冻元素动态衰减状态 —— 独立于普通 StatusInstance
     * 所有 FROZEN 实例共享这一个衰减率调节器
     */
    @Persisted(key = "frozen_decay_state")
    private FrozenDecayState frozenDecayState = new FrozenDecayState();

    public StatusContainer() {
        this.instances = new ArrayList<>();
    }

    public static final StatusContainer EMPTY = new StatusContainer();

    // ========== 存取 ==========

    /** 直接添加一个实例（不去重不判断，由调用方管） */
    public void add(StatusInstance instance) {
        instances.add(instance);
        if (instance instanceof ElementalAttachmentInstance ea) {
            ea.setContainer(this);
        }
    }

    /** 按条件查找第一个匹配的实例 */
    public StatusInstance find(Predicate<StatusInstance> matcher) {
        return instances.stream().filter(matcher).findFirst().orElse(null);
    }

    /** 容器里是否存在未 finished 的匹配实例 */
    public boolean hasAlive(Predicate<StatusInstance> matcher) {
        return instances.stream().anyMatch(i -> !i.isFinished() && matcher.test(i));
    }

    /** 按条件移除第一个匹配的实例，移除时调 onRemove() */
    public boolean removeFirst(Predicate<StatusInstance> matcher) {
        Iterator<StatusInstance> it = instances.iterator();
        while (it.hasNext()) {
            StatusInstance inst = it.next();
            if (matcher.test(inst)) {
                inst.onRemove();
                it.remove();
                return true;
            }
        }
        return false;
    }

    /** 移除指定实例，移除时调 onRemove() */
    public void remove(StatusInstance instance) {
        instance.onRemove();
        instances.remove(instance);
    }

    /** 清空所有实例，每个都调 onRemove() */
    public void clear() {
        for (StatusInstance inst : instances) {
            inst.onRemove();
        }
        instances.clear();
    }

    public List<StatusInstance> getAll() { return instances; }
    public boolean isEmpty() { return instances.isEmpty(); }

    // ========== Tick ==========

    /**
     * 每 tick 调用。遍历所有实例，先让它们 tick（让时间流逝），
     * 再判断是否 finished，finished 的调 onRemove() 然后清掉。
     */
    public void tick() {
        boolean hadFrozenAlive = false;
        for (StatusInstance inst : instances) {
            if (!inst.isFinished()
                    && inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == ElementalsGIM.FROZEN) {
                hadFrozenAlive = true;
                break;
            }
        }
        Iterator<StatusInstance> it = instances.iterator();
        while (it.hasNext()) {
            StatusInstance inst = it.next();
            inst.tick();
            if (inst.isFinished()) {
                inst.onRemove();
                it.remove();
            }
        }
        frozenDecayState.onTick(hadFrozenAlive);
    }

    public FrozenDecayState getFrozenDecayState() {
        return frozenDecayState;
    }

    // ========== 拷贝 ==========

    public StatusContainer copy() {
        StatusContainer c = new StatusContainer();
        for (StatusInstance inst : instances) {
            c.instances.add(inst.copy());
        }
        return c;
    }
}