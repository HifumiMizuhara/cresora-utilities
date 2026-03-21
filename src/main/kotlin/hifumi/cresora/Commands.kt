package hifumi.cresora

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
object Commands {
    fun init() {
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            // ここにコマンドを定義していきます
            dispatcher.register(
                literal("about")
                    .executes { context ->
                        context.source.sendFeedback({Text.translatable("commands.cresora.about", version)} , false)
                        1
                    }
            )

        }
    }
}
