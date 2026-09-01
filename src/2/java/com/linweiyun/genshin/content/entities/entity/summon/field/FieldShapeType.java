package com.linweiyun.genshin.content.entities.entity.summon.field;

public enum FieldShapeType {
  SPHERE("sphere"),
  CYLINDER("cylinder");

  private final String shapeType;

  FieldShapeType(String shapeType) {
    this.shapeType = shapeType;
  }

  public String getShapeType() {
    return shapeType;
  }
}
