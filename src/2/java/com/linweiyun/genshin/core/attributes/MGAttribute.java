package com.linweiyun.genshin.core.attributes;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class MGAttribute implements IPersistedSerializable {
  @Persisted(key = "base_value")
  private double baseValue;

  @Persisted(key = "extra_value")
  private double extraValue;

  @Persisted(key = "extra_percent")
  private double extraPercent;

  @Persisted(key = "temporary_value")
  private double temporaryValue;

  @Persisted(key = "temporary_percent")
  private double temporaryPercent;

  public MGAttribute() {
    this.baseValue = 0;
    this.extraPercent = 0;
    this.extraValue = 0;
    this.temporaryPercent = 0;
    this.temporaryValue = 0;
  }

  public MGAttribute(double baseValue) {
    this.baseValue = baseValue;
    this.extraPercent = 0;
    this.extraValue = 0;
    this.temporaryPercent = 0;
    this.temporaryValue = 0;
  }

  public MGAttribute(MGAttribute original) {
    this.baseValue = original.baseValue;
    this.extraPercent = original.extraPercent;
    this.extraValue = original.extraValue;
    this.temporaryPercent = original.temporaryPercent;
    this.temporaryValue = original.temporaryValue;
  }

  private static double roundTo4(double value) {
    return Math.round(value * 10000.0) / 10000.0;
  }
  private static double roundTo3(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }
  private static double roundTo2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
  public void setBaseValue(double baseValue) {
    this.baseValue = roundTo4(baseValue);
  }
  public void addBaseValue(double baseValue) {this.baseValue = roundTo4(this.baseValue + baseValue);}
  public void reduceBaseValue(double baseValue) {this.baseValue = roundTo4(this.baseValue - baseValue);}

  public void setExtraPercent(double extraPercent) {
    this.extraPercent = roundTo4(extraPercent);
  }
  public void addExtraPercent(double extraPercent) {this.extraPercent = roundTo4(this.extraPercent + extraPercent);}
  public void reduceExtraPercent(double extraPercent) {this.extraPercent = roundTo4(this.extraPercent - extraPercent);}

  public void setExtraValue(double extraValue) {
    this.extraValue = roundTo4(extraValue);
  }
  public void addExtraValue(double extraValue) {this.extraValue = roundTo4(this.extraValue + extraValue);}
  public void reduceExtraValue(double extraValue) {this.extraValue = roundTo4(this.extraValue - extraValue);}

  public void setTemporaryPercent(double temporaryPercent) {
    this.temporaryPercent = roundTo4(temporaryPercent);
  }
  public void addTemporaryPercent(double temporaryPercent) {this.temporaryPercent = roundTo4(this.temporaryPercent + temporaryPercent);}
  public void reduceTemporaryPercent(double temporaryPercent) {this.temporaryPercent = roundTo4(this.temporaryPercent - temporaryPercent);}

  public void setTemporaryValue(double temporaryValue) {
    this.temporaryValue = roundTo4(temporaryValue);
  }
  public void addTemporaryValue(double temporaryValue) {this.temporaryValue = roundTo4(this.temporaryValue + temporaryValue);}
  public void reduceTemporaryValue(double temporaryValue) {this.temporaryValue = roundTo4(this.temporaryValue - temporaryValue);}

  public double getBaseValue(boolean asInteger) {
    return asInteger ? (double)(long) baseValue : roundTo3(baseValue);
  }

  public double getExtraValue(boolean asInteger) {
    double val = extraValue + extraValue * extraPercent;
    return asInteger ? (double)(long) val : roundTo3(val);
  }

  public double getExtraPercent(boolean asInteger) {
    return asInteger ? (double)(long) extraPercent : roundTo2(extraPercent * 100);
  }

  public double getTemporaryValue(boolean asInteger) {
    double val = temporaryValue + temporaryValue * temporaryPercent;
    return asInteger ? (double)(long) val : roundTo3(val);
  }

  public double getTotalValue(boolean asInteger) {
    double val = baseValue + extraValue + baseValue * extraPercent + temporaryValue + temporaryValue * temporaryPercent;
    return asInteger ? (double)(long) val : roundTo3(val);
  }

  public double getPermanentValue(boolean asInteger) {
    double val = baseValue + extraValue + baseValue * extraPercent;
    return asInteger ? (double)(long) val : roundTo3(val);
  }

  public static final Codec<MGAttribute> CODEC = PersistedParser.createCodec(MGAttribute::new);

  public static final StreamCodec<ByteBuf, MGAttribute> STREAM_CODEC =
      PersistedParser.createStreamCodec(MGAttribute::new);

  public boolean equals(MGAttribute a, MGAttribute b) {
    return Double.compare(a.baseValue, b.baseValue) == 0
            && Double.compare(a.extraPercent, b.extraPercent) == 0
            && Double.compare(a.extraValue, b.extraValue) == 0
            && Double.compare(a.temporaryPercent, b.temporaryPercent) == 0
            && Double.compare(a.temporaryValue, b.temporaryValue) == 0;
  }

  public MGAttribute copy() {
    MGAttribute copy = new MGAttribute();
    copy.baseValue = this.baseValue;
    copy.extraPercent = this.extraPercent;
    copy.extraValue = this.extraValue;
    copy.temporaryPercent = this.temporaryPercent;
    copy.temporaryValue = this.temporaryValue;
    return copy;
  }
}
