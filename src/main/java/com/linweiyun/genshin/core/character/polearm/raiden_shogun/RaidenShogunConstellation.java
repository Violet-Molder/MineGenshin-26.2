package com.linweiyun.genshin.core.character.polearm.raiden_shogun;

import com.linweiyun.genshin.core.character.talent.ConstellationBase;

/**
 * 雷电将军的<b>命之座</b>（C1..C6）。
 *
 * <p>现状：还没有实现任何命座效果 —— 招式与天赋里没有任何
 * {@code hasConstellation(n)} 分支，所以这里暂时是空壳。
 * 留这个类是为了让三个协作者的形状一致；以后加命座时在这里写，由
 * {@link RaidenShogunSkill} 一行调用。
 */
public class RaidenShogunConstellation extends ConstellationBase {
}
