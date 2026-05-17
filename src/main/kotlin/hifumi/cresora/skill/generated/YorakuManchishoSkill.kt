package hifumi.cresora.skill.generated

import hifumi.cresora.WeaponData
import hifumi.cresora.WeaponDefinition
import hifumi.cresora.WeaponSkillAccess
import hifumi.cresora.skill.WeaponSkillHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting

public object YorakuManchishoSkill : WeaponSkillHandler {
  override fun activate(
    player: ServerPlayerEntity,
    definition: WeaponDefinition,
    `data`: WeaponData,
    access: WeaponSkillAccess,
  ): ActionResult {

    ; run execute@ {
      val state = QianqiuYeluoSkill.qiucanStackStates.get(player.uuid);
                      if (state == null || state.stacks < 20) {
                         
          player.sendMessage(net.minecraft.text.Text.translatable("item.cresora.weapon.qianqiu_yeluo.insufficient_stacks").formatted(net.minecraft.util.Formatting.RED),
          true);
                          return@execute;
                      }

                      val buffState =
          QianqiuYeluoSkill.yorakuManchishoActiveStates.getOrPut(player.uuid) {
                          QianqiuYeluoSkill.YorakuManchishoActiveState(0L, 0)
                      }
                      buffState.expireTick =
          hifumi.cresora.WeaponSkillService.currentWorldTime(player) + 60 * 20L;
                      buffState.stacks = 1;

                      hifumi.cresora.HotbarOverrideService.restoreHotbar(player);
    }

    player.sendMessage(Text.translatable("item.cresora.weapon.qianqiu_yeluo.yoraku_activated").formatted(Formatting.GOLD),
        true)


    return ActionResult.SUCCESS
  }
}
