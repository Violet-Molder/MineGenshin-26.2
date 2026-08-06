package com.linweiyun.genshin.enums;

public enum AttachmentType {
  WEAK(1.0f, 0.084f), // 弱附着：初始量1，每20tick衰减0.084
  STRONG(2.0f, 0.133f), // 强附着：初始量2，每20tick衰减0.133
  ULTRA_STRONG(4.0f, 0.188f); // 超强附着：初始量4，每20tick衰减0.188

  private final float initialAmount;
  private final float decayPer20Ticks;

  AttachmentType(float initialAmount, float decayPer20Ticks) {
    this.initialAmount = initialAmount;
    this.decayPer20Ticks = decayPer20Ticks;
  }

  public float getInitialAmount() {
    return initialAmount;
  }

  public float getDecayPer20Ticks() {
    return decayPer20Ticks;
  }
}
