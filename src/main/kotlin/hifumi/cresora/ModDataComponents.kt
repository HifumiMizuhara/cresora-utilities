package hifumi.cresora

import com.mojang.serialization.Codec
import net.minecraft.component.ComponentType
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

    // このメソッドをModの初期化時に呼ぶことで、クラスがロードされコンポーネントが登録される
    fun initialize() {}
}
