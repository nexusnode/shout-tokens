package net.craftingdead.shouttokens;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

public class ShoutTokensPermissions {

  public static final PermissionNode<Boolean> BYPASS_TOKEN_COST =
      new PermissionNode<Boolean>(ShoutTokens.ID, "bypasscost",
          PermissionTypes.BOOLEAN, ShoutTokensPermissions::resolvePermission);

  public static final PermissionNode<Boolean> ADMIN =
      new PermissionNode<Boolean>(ShoutTokens.ID, "admin",
          PermissionTypes.BOOLEAN, ShoutTokensPermissions::resolvePermission);

  private static boolean resolvePermission(@Nullable ServerPlayer player, UUID playerUUID,
      PermissionDynamicContext<?>... context) {
    return player != null
        && player.hasPermissions(player.getServer().getOperatorUserPermissionLevel());
  }
}
