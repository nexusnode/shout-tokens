package net.craftingdead.shouttokens;

import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;

@Mod(ShoutTokens.ID)
public class ShoutTokens {

  public static final String ID = "shouttokens";

  private static ShoutTokens instance;

  private static final Path SHOUT_TOKENS_PATH = Path.of("shout-tokens.json");

  private final TokenManager tokenManager;

  public ShoutTokens() {
    instance = this;

    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShoutTokensConfig.SPEC);
    this.tokenManager = TokenManager.load(SHOUT_TOKENS_PATH);

    var forgeBus = MinecraftForge.EVENT_BUS;
    forgeBus.addListener(EventPriority.LOWEST, this::handleChat);
    forgeBus.addListener(this::handleServerStopping);
    forgeBus.addListener(this::handlePermissionNodesGather);
    forgeBus.addListener(this::handleRegisterCommands);
  }

  public TokenManager tokenManager() {
    return this.tokenManager;
  }

  public void saveTokens() {
    this.tokenManager.save(SHOUT_TOKENS_PATH);
  }

  private void handlePermissionNodesGather(PermissionGatherEvent.Nodes event) {
    event.addNodes(ShoutTokensPermissions.BYPASS_TOKEN_COST, ShoutTokensPermissions.ADMIN);
  }

  private void handleRegisterCommands(RegisterCommandsEvent event) {
    ShoutTokensCommand.register(event.getDispatcher());
  }

  private void handleServerStopping(ServerStoppingEvent event) {
    this.saveTokens();
  }

  private void handleChat(ServerChatEvent event) {
    var player = event.getPlayer();

    var prefix = ShoutTokensConfig.INSTANCE.shoutPrefix.get();
    var prefixLength = prefix.length();
    if (!event.getMessage().startsWith(prefix)) {
      return;
    }
    if (event.getMessage().length() <= prefixLength) {
      return;
    }

    event.setCanceled(true);

    if (!this.tokenManager.purchaseShout(player)) {
      player.displayClientMessage(
          new TextComponent(ShoutTokensConfig.INSTANCE.insufficientTokensMessage.get()
              .formatted(ShoutTokensConfig.INSTANCE.tokensPerMessage.get()))
                  .withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
          true);
      return;
    }

    var baseMessage = TextComponent.EMPTY.copy()
        .append(ShoutTokensConfig.INSTANCE.shoutMessagePrefix.get());

    var message = baseMessage.copy()
        .append(stripShoutPrefix(event.getComponent()));
    var filteredMessage = baseMessage.copy()
        .append(stripShoutPrefix(event.getFilteredComponent()));

    player.server.getPlayerList().broadcastMessage(message,
        recipient -> player.shouldFilterMessageTo(recipient)
            ? filteredMessage
            : message,
        ChatType.CHAT, player.getUUID());
  }

  public static ShoutTokens instance() {
    return instance;
  }

  private static Component stripShoutPrefix(Component message) {
    var stripper = new ShoutFormatter();
    message.visit(stripper, Style.EMPTY);
    return stripper.result();
  }

  private static class ShoutFormatter implements FormattedText.StyledContentConsumer<Void> {

    private final MutableComponent result = TextComponent.EMPTY.copy();
    private boolean foundShoutPrefix;

    public Component result() {
      return this.result;
    }

    @Override
    public Optional<Void> accept(Style style, String content) {
      if (!this.foundShoutPrefix
          && content.contains(ShoutTokensConfig.INSTANCE.shoutPrefix.get())) {
        content = content.replace(ShoutTokensConfig.INSTANCE.shoutPrefix.get(), "");
        this.foundShoutPrefix = true;
      }

      if (!content.isEmpty()) {
        this.result.append(new TextComponent(content).setStyle(style));
      }

      return Optional.empty();
    }
  }
}
