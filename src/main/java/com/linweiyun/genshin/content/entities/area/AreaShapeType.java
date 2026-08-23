package com.linweiyun.genshin.content.entities.area;

public enum AreaShapeType {
    SPHERE("sphere"),      // 球体形状
    CYLINDER("cylinder");  // 圆柱体形状

    // 形状类型的字符串标识符
    private final String shapeType;

    AreaShapeType(String shapeType) {
        this.shapeType = shapeType;  // 存储标识符
    }

    /** 获取形状类型的字符串标识符 */
    public String getShapeType() {
        return shapeType;
    }
}
