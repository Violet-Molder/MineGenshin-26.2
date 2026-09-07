package com.linweiyun.genshin.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;


public class Config {

  private static final ModConfigSpec.Builder CHARACTER_EXP_BUILDER = new ModConfigSpec.Builder();
  private static final ModConfigSpec.Builder CHARACTER_ATTRIBUTE_BUILDER = new ModConfigSpec.Builder();

  public static final ModConfigSpec.ConfigValue<List<? extends Integer>> CHARACTER_UP_EXP =
		  CHARACTER_EXP_BUILDER.translation("minegenshin.config.exp.character")
				  .defineList(
						  List.of("Character Up Exp"),
						  () -> List.of(
								  1000, 1325, 1700, 2150, 2625, 3150, 3725, 4350, 5000, 5700,
								  6450, 7225, 8050, 8925, 9825, 10750, 11725, 12725, 13775, 14875,
								  16800, 18000, 19250, 20550, 21875, 23250, 24650, 26100, 27575, 29100,
								  30650, 32250, 33875, 35550, 37250, 38975, 40750, 42575, 44425, 46300,
								  50625, 52700, 54775, 56900, 59075, 61275, 63525, 65800, 68125, 70457,
								  76500, 79050, 81650, 84275, 86950, 89650, 92400, 95175, 98000, 100875,
								  108950, 112050, 115175, 118325, 121525, 124775, 128075, 131400, 134775, 138175,
								  148700, 152375, 156075, 159825, 163600, 167425, 171300, 175225, 179175, 183175,
								  216225, 243025, 273100, 306800, 344600, 386950, 434225, 487625, 547200
						  ),
						  null,
						  obj -> obj instanceof Integer,
						  ModConfigSpec.Range.of(88, 88)
				  );
  //申鹤
	public static final ModConfigSpec CHARACTER_ATTRIBUTE_SPEC;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SHENHE_HP;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SHENHE_DEF;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SHENHE_ATK;

	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> COLUMBINA_HP;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> COLUMBINA_DEF;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> COLUMBINA_ATK;
	static {


		//申鹤的基础属性
		CHARACTER_ATTRIBUTE_BUILDER
				.push("shenhe");

		SHENHE_HP = CHARACTER_ATTRIBUTE_BUILDER
				.translation("minegenshin.configuration.attribute.hp")
				.defineList(
						List.of("hp"),
						() -> List.of(
								1011, 1095, 1179, 1264, 1348, 1433, 1517, 1602, 1687, 1771,
								1856, 1941, 2026, 2112, 2197, 2282, 2368, 2453, 2539, 2624,
								3491, 3577, 3663, 3749, 3835, 3921, 4008, 4094, 4180, 4267, 4353,
								4440, 4527, 4614, 4700, 4787, 4875, 4962, 5049, 5136, 5224,
								5840, 5927, 6015, 6103, 6190, 6278, 6366, 6454, 6542, 6631, 6719,
								7540, 7628, 7717, 7805, 7894, 7983, 8072, 8161, 8250, 8339, 8429,
								9045, 9134, 9223, 9313, 9402, 9492, 9582, 9671, 9761, 98541, 9941,
								10557, 10647, 10738, 10828, 10918, 11009, 11099, 11190, 11281, 11372, 11463,
								12080, 12171, 12262, 12353, 12444, 12535, 12627, 12717, 12810, 12902, 12993
						),
						null,
						obj -> obj instanceof Integer,
						ModConfigSpec.Range.of(95, 95)
				);

		SHENHE_DEF = CHARACTER_ATTRIBUTE_BUILDER
				.translation("minegenshin.configuration.attribute.def")
				.defineList(
						List.of("def"),
						() -> List.of(
								65, 70, 75, 81, 86, 92, 97, 102, 108, 113,
								119, 124, 129, 135, 140, 146, 151, 157, 162, 168,
								223, 229, 234, 239, 245, 250, 256, 262, 267, 273, 278,
								284, 289, 295, 300, 306, 311, 317, 323, 328, 334,
								373, 379, 384, 390, 395, 401, 407, 412, 418, 424, 429,
								482, 487, 493, 499, 504, 510, 516, 521, 527, 533, 538,
								578, 583, 589, 595, 601, 606, 612, 618, 624, 629, 635,
								674, 680, 686, 692, 697, 703, 709, 715, 721, 727, 732,
								772, 778, 783, 789, 795, 801, 807, 812, 818, 824, 830

						),
						null,
						obj -> obj instanceof Integer,
						ModConfigSpec.Range.of(88, 88)
				);

		SHENHE_ATK = CHARACTER_ATTRIBUTE_BUILDER
				.translation("minegenshin.configuration.attribute.atk")
				.comment("Shenhe ATK per level")
				.defineList(
						List.of("atk"),
						() -> List.of(
								24, 26, 28, 30, 32, 34, 35, 37, 39, 41,
								43, 45, 47, 49, 51, 53, 55, 57, 59, 61,
								82, 84, 86, 88, 90, 92, 94, 96, 98, 100, 102,
								104, 106, 108, 110, 112, 114, 116, 118, 120, 122,
								137, 139, 141, 143, 145, 147, 149, 151, 153, 155, 157,
								176, 178, 180, 182, 185, 187, 189, 191, 193, 195, 197,
								211, 214, 216, 218, 220, 222, 224, 226, 228, 230, 232,
								247, 249, 251, 253, 255, 257, 259, 262, 264, 266, 268,
								282, 285, 287, 289, 291, 293, 295, 297, 299, 302, 304
						),
						null,
						obj -> obj instanceof Integer,
						ModConfigSpec.Range.of(88, 88)
				);
		CHARACTER_ATTRIBUTE_BUILDER.pop();

		//哥伦比娅的基础属性
		CHARACTER_ATTRIBUTE_BUILDER
				.push("columbina");

		COLUMBINA_HP = CHARACTER_ATTRIBUTE_BUILDER
				.translation("minegenshin.configuration.attribute.hp")
				.defineList(
						List.of("hp"),
						() -> List.of(
								1144, 1239, 1334, 1430, 1525, 1621, 1716, 1812, 1908, 2003,
								2099, 2195

						),
						null,
						obj -> obj instanceof Integer,
						ModConfigSpec.Range.of(95, 95)
				);
		COLUMBINA_DEF = CHARACTER_ATTRIBUTE_BUILDER
				.translation("minegenshin.configuration.attribute.def")
				.defineList(
						List.of("def"),
						() -> List.of(
								10, 12, 14, 16, 18, 20, 22, 24, 26, 28,
								30, 32, 34, 36, 38, 40, 42, 44, 46, 48, 50
						),
						null,
						obj -> obj instanceof Integer,
						ModConfigSpec.Range.of(88, 88)
				);
		COLUMBINA_ATK = CHARACTER_ATTRIBUTE_BUILDER
				.translation("minegenshin.configuration.attribute.atk")
				.defineList(
						List.of("atk"),
						() -> List.of(
								5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
								15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25
						),
						null,
						obj -> obj instanceof Integer,
						ModConfigSpec.Range.of(88, 88)
				);
		CHARACTER_ATTRIBUTE_SPEC = CHARACTER_ATTRIBUTE_BUILDER.build();
	}

  public static final ModConfigSpec CHARACTER_EXP_SPEC = CHARACTER_EXP_BUILDER.build();
}