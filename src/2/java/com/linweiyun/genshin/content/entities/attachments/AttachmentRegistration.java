package com.linweiyun.genshin.content.entities.attachments;

import com.linweiyun.genshin.Minegenshin;
import com.linweiyun.genshin.content.entities.attachments.attachment.*;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AttachmentRegistration {

  public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Minegenshin.MOD_ID);

  public static final Supplier<AttachmentType<PlayerPrimogemAttachment>> PRIMOGEM_ATTACHMENT =
      ATTACHMENTS.register(
          "player_primogem",
          () -> AttachmentType.serializable(PlayerPrimogemAttachment::new).build());

  public static final Supplier<AttachmentType<PlayerGenshinModeAttachment>>
      GENSHIN_MODE_ATTACHMENT =
          ATTACHMENTS.register(
              "player_genshin_mode",
              () ->
                  AttachmentType.serializable(PlayerGenshinModeAttachment::new)
                      .copyOnDeath()
                      .build());

  public static final Supplier<AttachmentType<CharacterParty>> CHARACTER_PARTY_ATTACHMENT =
      ATTACHMENTS.register(
          "character_party",
          () -> AttachmentType.serializable(CharacterParty::new).copyOnDeath().build());
  public static final Supplier<AttachmentType<CharacterSheet>> CHARACTER_SHEET_ATTACHMENT =
      ATTACHMENTS.register(
          "character_sheet",
          () -> AttachmentType.serializable(CharacterSheet::new).copyOnDeath().build());
  public static final Supplier<AttachmentType<GenshinBackpack>> GENSHIN_BACKPACK_ATTACHMENT =
      ATTACHMENTS.register(
          "genshin_backpack",
          () -> AttachmentType.serializable(GenshinBackpack::new).copyOnDeath().build()
      );

  public static void register(IEventBus modEventBus) {
    ATTACHMENTS.register(modEventBus);
  }
}
