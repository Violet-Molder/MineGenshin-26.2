package com.linweiyun.genshin.core.character.polearm.shenhe;

import com.linweiyun.genshin.core.character.talent.ConstellationBase;

/**
 * 申鹤的<b>命之座</b>（C1..C6）。
 *
 * <p>现状：申鹤还没有实现任何命座效果 —— 招式与天赋里没有任何
 * {@code hasConstellation(n)} 分支，所以这里暂时是空壳。
 * 留这个类是为了让「每个角色都有 Skill / Talent / Constellation 三个协作者」这条形状一致，
 * 以后加命座时直接在这里写、由 {@link ShenheSkill} 一行调用即可。
 */
public class ShenheConstellation extends ConstellationBase {
}
