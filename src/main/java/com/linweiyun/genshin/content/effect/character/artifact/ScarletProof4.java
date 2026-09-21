package com.linweiyun.genshin.content.effect.character.artifact;

/**
 * 血红之证 · 四件套：<b>触发星扩散反应后</b>10 秒内，暴击率 +16%、星扩散反应伤害 +40%。
 *
 * <p>这个类本身不改属性 —— 它只是「穿着四件套」的<b>标记</b>：
 * 触发星扩散时（{@code SwirlReaction}）检查角色身上有没有它，
 * 有就挂上 {@link ScarletProofBuffEffect}（那个才真正给暴击率与星扩散加成，10 秒后自然掉）。
 *
 * <p>为什么不直接把加成写在这里：四件套是<b>常驻</b>效果，而加成只在触发后的 10 秒内生效 ——
 * 两者生命周期不同，混在一起就得在 tick 里反复加/减修饰符，容易漏。
 */
public class ScarletProof4 extends ArtifactSetEffect {
}
