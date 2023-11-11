package net.craftingdead.shouttokens;

import net.minecraftforge.common.ForgeConfigSpec;

public class ShoutTokensConfig {

  public static final ShoutTokensConfig INSTANCE;
  public static final ForgeConfigSpec SPEC;

  static {
    var pair = new ForgeConfigSpec.Builder().configure(ShoutTokensConfig::new);
    SPEC = pair.getRight();
    INSTANCE = pair.getLeft();
  }

  public final ForgeConfigSpec.IntValue tokensPerMessage;
  public final ForgeConfigSpec.ConfigValue<String> insufficientTokensMessage;
  public final ForgeConfigSpec.ConfigValue<String> shoutMessagePrefix;
  public final ForgeConfigSpec.ConfigValue<String> shoutPrefix;

  private ShoutTokensConfig(ForgeConfigSpec.Builder builder) {
    this.tokensPerMessage = builder.defineInRange("tokensPerMessage", 1, 1, Integer.MAX_VALUE);
    this.insufficientTokensMessage =
        builder.define("insufficientTokensMessage", "Insufficient shout tokens, %s required.");
    this.shoutMessagePrefix = builder.define("shoutMessagePrefix", "§c[§fShout§c] ");
    this.shoutPrefix = builder.define("shoutPrefix", "!");
  }
}
