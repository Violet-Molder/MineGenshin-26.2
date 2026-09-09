package com.linweiyun.genshin.render.gui.screens.atrifact;

import com.linweiyun.genshin.content.items.artifact.ArtifactItem;
import com.linweiyun.genshin.content.items.artifact.type.ArtifactType;
import com.linweiyun.genshin.content.items.component.ArtifactStatsComponent;
import com.linweiyun.genshin.content.stat.TeyvatItemStat;
import com.linweiyun.genshin.core.attachment.AttachmentRegistration;
import com.linweiyun.genshin.core.attachment.Backpack;
import com.linweiyun.genshin.content.items.artifact.inventory.ArtifactInventory;
import com.linweiyun.genshin.core.character.PGCharacterData;
import com.linweiyun.genshin.core.network.NetworkManager;
import com.linweiyun.genshin.core.system.registry.register.ModDataComponents;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUIClientAccess;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Scroller;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;

/**
 * 圣遗物选择/管理界面
 *
 * 架构核心：
 * - Screen 本身持有全部状态，不使用静态方法传递状态
 * - ModularUI 在 init() 之前就已完整构建且所有面板均为可见状态
 * - LDLib2 的调试系统（F12）在 init() → setScreenAndInit() 时扫描整个组件树
 * - 所有 UI 容器和内容在注册前就已存在于树中，确保调试系统能完整追踪
 */
public class ScreenArtifactEquip extends Screen {

    private static final Logger LOG = LogUtils.getLogger();

    final ModularUI modularUI;

    // ======== 常量 ========
    private static final ArtifactType[] TYPES = ArtifactType.values();
    private static final String[] LABELS = {"生之花","死之羽","时之沙","空之杯","理之冠"};
    private static final String BORDER = "minegenshin:textures/character_avatar/selected_border.png";
    private static final float SCF = 20f;

    public ScreenArtifactEquip(ModularUI modularUI) {
        super(Component.empty());
        this.modularUI = modularUI;
    }

    @Override
    public void init() {
        super.init();
        ModularUIClientAccess.setScreenAndInit(this.modularUI, this);
        this.addRenderableWidget(ModularUIClientAccess.getWidget(modularUI));
        LOG.info("ScreenArtifactEquip init() → LDLib2 UI已注册");
    }

    // ================================================================
    //  工厂方法：构建完整 UI（所有内容在 init() 前已就位）
    // ================================================================

    public static ModularUI createModularUI(Player player, int slotIndex) {
        var ss = StylesheetManager.INSTANCE.getStylesheetSafe(
                Identifier.parse("minegenshin:lss/artifact_equip.lss"));

        var ca = player.getData(AttachmentRegistration.PLAYER_CHARACTERS_ATTACHMENT);
        var cc = ca.getCurrentCharacter();
        var root = new UIElement().setId("root");
        root.layout(l -> { l.widthPercent(100); l.heightPercent(100); });

        if (cc == null || cc.getData() == null) {
            root.addChild(new Label().setText("未选择角色"));
            return ModularUI.of(UI.of(root, ss), player);
        }

        var cd = cc.getData();
        var inv = cd.getArtifactInventory();
        var bp = player.getData(AttachmentRegistration.BACKPACK_ATTACHMENT);

        // ===== 状态对象 =====
        var st = new St(slotIndex, ItemStack.EMPTY, -1,
                slotIndex >= 0 && slotIndex < 5 ? ArtifactInventory.slotToType(slotIndex) : ArtifactType.FLOWER,
                null, 0f);
        autoSel(inv, bp, st);

        // ===== 窗口容器 =====
        var win = new UIElement().setId("window");

        // ---------- 顶部 5 槽位 ----------
        var top = new UIElement().setId("top-section");
        var srow = new UIElement().setId("artifact-slots-row");
        for (int i = 0; i < 5; i++) {
            int si = i;
            var se = slotEl(i, inv);
            se.addEventListener(UIEvents.MOUSE_ENTER, e -> {
                se.style(s -> s.overlay(SpriteTexture.of(BORDER)));
                se.transform(t -> t.scale(1.1f));
            });
            se.addEventListener(UIEvents.MOUSE_LEAVE, e -> {
                if (!(st.slot == si && st.artifact.isEmpty())) {
                    se.style(s -> s.overlay(null));
                    se.transform(t -> t.scale(1f));
                }
            });
            se.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                se.transform(t -> t.scale(1f));
            });
            se.addEventListener(UIEvents.CLICK, e -> clickSlot(si, inv, bp, cd, st, top));
            srow.addChild(se);
        }
        top.addChild(srow);

        // ---------- 详情区域 ----------
        var det = new UIElement().setId("detail-section");
        var lp = new UIElement().setId("left-panel");
        var tc = new UIElement().setId("type-toggle-container");
        var src = new UIElement().setId("list-scroller-container");
        var scr = new Scroller.Vertical();
        var lc = new UIElement().setId("artifact-list-container");
        scr.bindObserver(v -> { st.scroll = v; lc.layout(l -> l.bottom(v * SCF)); lc.markAsInternal(); });
        src.addChildren(scr, lc);
        lp.addChildren(tc, src);
        var mp = new UIElement().setId("middle-panel");
        var rp = new UIElement().setId("right-panel");
        det.addChildren(lp, mp, rp);

        win.addChildren(top, det);
        root.addChild(win);

        // ===== ★ 填充初始内容（发生在 init() 之前） =====
        fillTC(tc, inv, bp, cd, st);
        fillLC(lc, inv, bp, st);
        fillDetail(mp, rp, inv, bp, st);

        // ===== ★ 内容在 init() 前已填充（LDLib2 组件树已注册），但默认隐藏 =====
        det.setDisplay(false);
        if (slotIndex >= 0 && slotIndex < 5) {
            top.setDisplay(false);
            det.setDisplay(true);
        }

        LOG.info("createModularUI完成，详情页内容已填充但默认隐藏");
        return ModularUI.of(UI.of(root, ss), player);
    }

    // ================================================================
    //  点击槽位
    // ================================================================

    private static void clickSlot(int si, ArtifactInventory inv, Backpack bp,
                                  PGCharacterData cd, St st, UIElement top) {
        LOG.info("点击槽位[{}]", si);
        st.slot = si;
        st.artifact = ItemStack.EMPTY;
        st.invIdx = -1;
        st.type = ArtifactInventory.slotToType(si);
        st.sel = null;
        st.scroll = 0f;
        top.setDisplay(false);
        var det = byId(top.getParent(), "detail-section");
        if (det != null) det.setDisplay(true);
        autoSel(inv, bp, st);

        var tc = byId(top.getParent(), "type-toggle-container");
        var lc = byId(top.getParent(), "artifact-list-container");
        var mp = byId(top.getParent(), "middle-panel");
        var rp = byId(top.getParent(), "right-panel");
        if (tc != null) fillTC(tc, inv, bp, cd, st);
        if (lc != null) fillLC(lc, inv, bp, st);
        if (mp != null && rp != null) fillDetail(mp, rp, inv, bp, st);
    }

    // ================================================================
    //  辅助：构建单个槽位元素
    // ================================================================

    private static UIElement slotEl(int idx, ArtifactInventory inv) {
        var s = inv.getItem(idx);
        var e = new UIElement().addClass("equipped-slot");
        if (!s.isEmpty()) e.style(x -> x.background(SpriteTexture.of(tex(s))));
        else e.addClass("empty-slot");
        return e;
    }

    // ================================================================
    //  自动选中第一个物品
    // ================================================================

    private static void autoSel(ArtifactInventory inv, Backpack bp, St st) {
        int si = ArtifactInventory.typeToSlot(st.type);
        var eq = inv.getItem(si);
        if (!eq.isEmpty()) { st.artifact = eq.copy(); st.invIdx = -1; return; }

        var arr = bp.getCategoryList(Backpack.Category.ARTIFACTS);
        for (int i = 0; i < arr.size(); i++) {
            var s = arr.get(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof ArtifactItem ai)) continue;
            if (ai.getType() != st.type) continue;
            st.artifact = s.copy(); st.invIdx = i; return;
        }
        st.artifact = ItemStack.EMPTY; st.invIdx = -1;
    }

    // ================================================================
    //  填充类型切换按钮
    // ================================================================

    private static void fillTC(UIElement c, ArtifactInventory inv, Backpack bp,
                               PGCharacterData cd, St st) {
        c.clearAllChildren();
        for (int i = 0; i < TYPES.length; i++) {
            var t = TYPES[i];
            var b = new Button();
            b.setText(LABELS[i]);
            b.addClass("type-toggle-btn");
            if (t == st.type) b.addClass("type-toggle-active");
            b.setOnClick(e -> {
                st.type = t; st.artifact = ItemStack.EMPTY; st.invIdx = -1;
                st.sel = null; st.slot = ArtifactInventory.typeToSlot(t); st.scroll = 0f;
                autoSel(inv, bp, st);
                fillTC(c, inv, bp, cd, st);
                var lc = byId(c.getParent().getParent(), "artifact-list-container");
                if (lc != null) fillLC(lc, inv, bp, st);
                var mp = byId(c.getParent().getParent(), "middle-panel");
                var rp = byId(c.getParent().getParent(), "right-panel");
                if (mp != null && rp != null) fillDetail(mp, rp, inv, bp, st);
            });
            c.addChild(b);
        }
        c.markAsInternal();
    }

    // ================================================================
    //  填充圣遗物列表
    // ================================================================

    private static void fillLC(UIElement c, ArtifactInventory inv, Backpack bp, St st) {
        c.clearAllChildren();
        st.sel = null;

        int si = ArtifactInventory.typeToSlot(st.type);
        var eq = inv.getItem(si);
        var list = new ArrayList<Ent>();
        if (!eq.isEmpty()) list.add(new Ent(eq.copy(), true, -1));

        var arr = bp.getCategoryList(Backpack.Category.ARTIFACTS);
        for (int i = 0; i < arr.size(); i++) {
            var s = arr.get(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof ArtifactItem ai)) continue;
            if (ai.getType() != st.type) continue;
            list.add(new Ent(s.copy(), false, i));
        }

        for (int i = 0; i < list.size(); i++) {
            var e = list.get(i);
            var el = new UIElement().addClass("artifact-list-item");
            el.style(x -> x.background(SpriteTexture.of(tex(e.s))));

            boolean sel = !st.artifact.isEmpty()
                    ? ItemStack.isSameItemSameComponents(e.s, st.artifact)
                    : e.eq;
            if (sel) { el.style(x -> x.overlay(SpriteTexture.of(BORDER)));
                        el.transform(t -> t.scale(1.1f)); st.sel = el; }

            el.addEventListener(UIEvents.MOUSE_ENTER, ev -> {
                if (st.sel != el) { el.style(x -> x.overlay(SpriteTexture.of(BORDER)));
                                    el.transform(t -> t.scale(1.1f)); }
            });
            el.addEventListener(UIEvents.MOUSE_LEAVE, ev -> {
                if (st.sel != el) { el.style(x -> x.overlay(null));
                                    el.transform(t -> t.scale(1f)); }
            });
            el.addEventListener(UIEvents.MOUSE_DOWN, ev -> {
                el.transform(t -> t.scale(1f));
            });
            el.addEventListener(UIEvents.CLICK, ev -> {
                var prev = st.sel;
                if (prev != null && prev != el) {
                    prev.style(x -> x.overlay(null));
                    prev.transform(t -> t.scale(1f));
                }
                st.sel = el;
                el.style(x -> x.overlay(SpriteTexture.of(BORDER)));
                el.transform(t -> t.scale(1.1f));
                st.artifact = e.s.copy();
                st.invIdx = e.eq ? -1 : e.idx;

                var mp = byId(c.getParent().getParent().getParent(), "middle-panel");
                var rp = byId(c.getParent().getParent().getParent(), "right-panel");
                if (mp != null && rp != null) fillDetail(mp, rp, inv, bp, st);
            });

            c.addChild(el);
        }
        c.markAsInternal();
    }

    // ================================================================
    //  填充详情面板（大图标 + 属性 + 按钮）
    // ================================================================

    private static void fillDetail(UIElement mp, UIElement rp,
                                   ArtifactInventory inv, Backpack bp, St st) {
        mp.clearAllChildren(); rp.clearAllChildren();

        int slot = st.slot;
        if (slot < 0 || slot >= 5) return;

        var sel = st.artifact;
        var eq = inv.getItem(slot);
        var disp = !sel.isEmpty() ? sel : (!eq.isEmpty() ? eq : ItemStack.EMPTY);
        boolean fromEq = !sel.isEmpty() ? st.invIdx < 0 : true;

        if (disp.isEmpty()) {
            rp.addChild(new Label().setText("该槽位无圣遗物").setId("info-empty"));
            rp.markAsInternal(); return;
        }

        // 大图标
        var ic = new UIElement().addClass("artifact-detail-icon");
        ic.style(x -> x.background(SpriteTexture.of(tex(disp))));
        ic.markAsInternal(); mp.addChild(ic); mp.markAsInternal();

        if (!(disp.getItem() instanceof ArtifactItem ai)) return;
        var stats = disp.getOrDefault(ModDataComponents.ARTIFACT_STATS.get(), ArtifactStatsComponent.DEFAULT);
        int star = ai.getStar();

        var info = new UIElement().setId("info-container");
        info.addChild(new Label().setText(disp.getHoverName()).addClass("info-name"));
        info.addChild(new Label().setText(Component.literal("★".repeat(star)).withStyle(ChatFormatting.GOLD)).addClass("info-star"));
        info.addChild(new Label().setText(Component.literal("等级: +"+stats.level).withStyle(ChatFormatting.GRAY)).addClass("info-level"));
        long ex = stats.getExpToNextLevel(star);
        info.addChild(new Label().setText(
                Component.literal(ex>0?"经验: "+stats.exp+" / "+ex:"经验: 已满级").withStyle(ChatFormatting.GRAY)).addClass("info-exp"));

        if (stats.mainStat != null && stats.mainStat.isInitialized())
            info.addChild(new Label().setText(Component.literal(statTxt(stats.mainStat)).withStyle(ChatFormatting.YELLOW)).addClass("info-main-stat"));
        if (stats.subStats != null) for (var ss : stats.subStats) {
            if (!ss.isInitialized()) continue;
            info.addChild(new Label().setText(Component.literal(statTxt(ss))
                    .withStyle(ss.isUnlocked()?ChatFormatting.GRAY:ChatFormatting.DARK_GRAY)).addClass("info-sub-stat"));
        }

        // 按钮
        var bc = new UIElement().setId("button-container");
        var act = new Button();
        act.addClass("action-button");
        if (fromEq) {
            if (!eq.isEmpty()) {
                act.setText("卸下");
                var tu = eq.copy();
                act.setOnClick(e -> {
                    NetworkManager.sendUnequipArtifactToServer(slot);
                    inv.setItem(slot, ItemStack.EMPTY);
                    bp.addItemToCategory(Backpack.Category.ARTIFACTS, tu);
                    st.artifact = ItemStack.EMPTY; st.invIdx = -1;
                    var lc = byId(mp.getParent(), "artifact-list-container");
                    if (lc != null) fillLC(lc, inv, bp, st);
                    fillDetail(mp, rp, inv, bp, st);
                });
            } else act.setDisplay(false);
        } else {
            act.setText(eq.isEmpty()?"穿戴":"更换");
            int ii = st.invIdx; var te = sel.copy();
            act.setOnClick(e -> {
                NetworkManager.sendEquipOrSwapArtifactToServer(slot, ii);
                var old = inv.getItem(slot);
                bp.removeItemFromCategory(Backpack.Category.ARTIFACTS, ii);
                inv.setItem(slot, te);
                if (!old.isEmpty()) bp.addItemToCategory(Backpack.Category.ARTIFACTS, old.copy());
                st.artifact = ItemStack.EMPTY; st.invIdx = -1;
                var lc = byId(mp.getParent(), "artifact-list-container");
                if (lc != null) fillLC(lc, inv, bp, st);
                fillDetail(mp, rp, inv, bp, st);
            });
        }

        var up = new Button();
        up.setText("升级");
        up.addClass("upgrade-button");
        up.setOnClick(e -> NetworkManager.sendArtifactLevelUpToServer(disp, 10000));
        if (stats.level >= stats.getMaxLevel(star)) up.setDisplay(false);

        bc.addChildren(act, up); info.addChild(bc);
        rp.addChild(info); rp.markAsInternal();
    }

    // ================================================================
    //  工具方法
    // ================================================================

    private static UIElement byId(UIElement p, String id) {
        if (id.equals(p.getId())) return p;
        for (var c : p.getChildren()) { var r = byId(c, id); if (r != null) return r; }
        return null;
    }

    private static String tex(ItemStack s) {
        if (s.isEmpty()) return "minegenshin:textures/empty.png";
        var id = BuiltInRegistries.ITEM.getKey(s.getItem());
        return id.getNamespace()+":textures/item/"+id.getPath()+".png";
    }

    private static String statTxt(TeyvatItemStat s) {
        if (!s.isInitialized()) return "";
        var n = Component.translatable(s.getAttribute().translationKey()).getString();
        return s.getKind()==TeyvatItemStat.StatKind.PERCENT
                ? n+" +"+String.format("%.1f%%",s.getValue()*100)
                : n+" +"+String.format("%.0f",s.getValue());
    }

    // ================================================================
    //  内部数据类
    // ================================================================

    private static final class St {
        int slot; ItemStack artifact; int invIdx;
        ArtifactType type; UIElement sel; float scroll;
        St(int s, ItemStack a, int i, ArtifactType t, UIElement e, float sc) {
            slot=s; artifact=a; invIdx=i; type=t; sel=e; scroll=sc;
        }
    }

    private record Ent(ItemStack s, boolean eq, int idx) {}
}