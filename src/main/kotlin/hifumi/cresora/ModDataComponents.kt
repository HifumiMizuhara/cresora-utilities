package hifumi.cresora

import hifumi.cresora.weapon.WeaponData
import hifumi.cresora.equipment.EquipmentData
import com.mojang.serialization.Codec
import net.minecraft.component.ComponentType
import net.minecraft.item.ItemStack
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

object ModDataComponents {
    // 'level'という名前で、Int(整数)を保存するデータコンポーネントを定義
    val LEVEL: ComponentType<Int> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "level"),
        ComponentType.builder<Int>()
            .codec(Codec.INT) // データをディスクに保存・ロードする方法
            .packetCodec(PacketCodecs.VAR_INT) // データをネットワークで送受信する方法
            .build()
    )

    val EQUIPMENT_DATA: ComponentType<EquipmentData> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "equipment_data"),
        ComponentType.builder<EquipmentData>()
            .codec(EquipmentData.CODEC)
            .packetCodec(PacketCodecs.registryCodec(EquipmentData.CODEC))
            .build()
    )

    val WEAPON_DATA: ComponentType<WeaponData> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "weapon_data"),
        ComponentType.builder<WeaponData>()
            .codec(WeaponData.CODEC)
            .packetCodec(PacketCodecs.registryCodec(WeaponData.CODEC))
            .build()
    )

    val STORY_LOAN_SESSION_ID: ComponentType<String> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "story_loan_session_id"),
        ComponentType.builder<String>()
            .codec(Codec.STRING)
            .packetCodec(PacketCodecs.STRING)
            .build()
    )

    val MASQUERADE_SESSION_ID: ComponentType<String> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "masquerade_session_id"),
        ComponentType.builder<String>()
            .codec(Codec.STRING)
            .packetCodec(PacketCodecs.STRING)
            .build()
    )

    val SUB_SKILL_EFFECT_ID: ComponentType<String> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "sub_skill_effect_id"),
        ComponentType.builder<String>()
            .codec(Codec.STRING)
            .packetCodec(PacketCodecs.STRING)
            .build()
    )

    val WEAPON_ARTIFACTS: ComponentType<List<ItemStack>> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of(CreSoraUtilities.MOD_ID, "weapon_artifacts"),
        ComponentType.builder<List<ItemStack>>()
            .codec(ItemStack.OPTIONAL_CODEC.listOf())
            .packetCodec(ItemStack.OPTIONAL_PACKET_CODEC.collect(PacketCodecs.toList()))
            .build()
    )

    // このメソッドをModの初期化時に呼ぶことで、クラスがロードされコンポーネントが登録される
    fun initialize() {}
}
