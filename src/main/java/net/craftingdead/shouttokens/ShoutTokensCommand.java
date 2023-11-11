package net.craftingdead.shouttokens;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;

public class ShoutTokensCommand {

  private static final Component MESSAGE_PREFIX = new TextComponent("§c[§fShout Tokens§c]§7 ");

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher
        .register(Commands.literal(ShoutTokens.ID)
            .then(Commands.literal("balance")
                .executes(ShoutTokensCommand::balance))
            .then(Commands.literal("save-tokens")
                .requires(ShoutTokensCommand::hasAdminPermission)
                .executes(ShoutTokensCommand::saveTokens))
            .then(Commands.literal("give")
                .requires(ShoutTokensCommand::hasAdminPermission)
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .then(Commands.argument("tokens", IntegerArgumentType.integer(1))
                        .executes(ShoutTokensCommand::giveTokens))))
            .then(Commands.literal("remove")
                .requires(ShoutTokensCommand::hasAdminPermission)
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .then(Commands.argument("tokens", IntegerArgumentType.integer(1))
                        .executes(ShoutTokensCommand::removeTokens))))
            .then(Commands.literal("set")
                .requires(ShoutTokensCommand::hasAdminPermission)
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .then(Commands.argument("tokens", IntegerArgumentType.integer(0))
                        .executes(ShoutTokensCommand::setTokens)))));
  }

  private static int balance(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    var player = context.getSource().getPlayerOrException();

    String message;
    if (PermissionAPI.getPermission(player, ShoutTokensPermissions.BYPASS_TOKEN_COST)) {
      message = "§aYou have §ounlimited §atokens.";
    } else {
      message = "You have %d token(s)."
          .formatted(ShoutTokens.instance().tokenManager().tokenBalance(player.getUUID()));
    }
    context.getSource().sendSuccess(message(new TextComponent(message)), false);
    return 0;
  }

  private static int saveTokens(CommandContext<CommandSourceStack> context) {
    ShoutTokens.instance().saveTokens();
    context.getSource().sendSuccess(
        message(new TextComponent("Shout tokens saved.")
            .withStyle(ChatFormatting.GREEN)),
        true);
    return 0;
  }

  private static int giveTokens(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    var tokenManager = ShoutTokens.instance().tokenManager();
    var targets = GameProfileArgument.getGameProfiles(context, "targets");
    var tokens = IntegerArgumentType.getInteger(context, "tokens");
    for (var target : targets) {
      tokenManager.addTokens(target.getId(), tokens);
    }
    var message = targets.size() == 1
        ? "Gave %d shout token(s) to %s.".formatted(tokens, targets.iterator().next().getName())
        : "Gave %d shout token(s) to %d players.".formatted(tokens, targets.size());
    context.getSource().sendSuccess(
        message(new TextComponent(message).withStyle(ChatFormatting.GREEN)), false);
    return 0;
  }

  private static int removeTokens(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    var tokenManager = ShoutTokens.instance().tokenManager();
    var targets = GameProfileArgument.getGameProfiles(context, "targets");
    var tokens = IntegerArgumentType.getInteger(context, "tokens");
    for (var target : targets) {
      tokenManager.removeTokens(target.getId(), tokens);
    }
    var message = targets.size() == 1
        ? "Removed %d shout token(s) from %s.".formatted(tokens,
            targets.iterator().next().getName())
        : "Removed %d shout token(s) from %d players.".formatted(tokens, targets.size());
    context.getSource().sendSuccess(
        message(new TextComponent(message).withStyle(ChatFormatting.GREEN)), false);
    return 0;
  }

  private static int setTokens(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    var tokenManager = ShoutTokens.instance().tokenManager();
    var targets = GameProfileArgument.getGameProfiles(context, "targets");
    var tokens = IntegerArgumentType.getInteger(context, "tokens");
    for (var target : targets) {
      tokenManager.setTokens(target.getId(), tokens);
    }
    var message = targets.size() == 1
        ? "Set %s's shout token balance to %d."
            .formatted(targets.iterator().next().getName(), tokens)
        : "Set %d players' shout token balances to %d.".formatted(targets.size(), tokens);
    context.getSource().sendSuccess(
        message(new TextComponent(message).withStyle(ChatFormatting.GREEN)), false);
    return 0;
  }

  private static Component message(Component body) {
    return TextComponent.EMPTY.copy()
        .append(MESSAGE_PREFIX)
        .append(body);
  }

  private static boolean hasAdminPermission(CommandSourceStack source) {
    return source.getEntity() instanceof ServerPlayer player
        && PermissionAPI.getPermission(player, ShoutTokensPermissions.ADMIN);
  }
}
