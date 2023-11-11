package net.craftingdead.shouttokens;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.permission.PermissionAPI;

public class TokenManager {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  private static final Codec<TokenManager> CODEC =
      Codec.unboundedMap(Codec.STRING.xmap(UUID::fromString, UUID::toString), Entry.CODEC)
          .xmap(TokenManager::new, TokenManager::entries);

  private final Map<UUID, Entry> entries;

  private TokenManager(Map<UUID, Entry> entries) {
    this.entries = new HashMap<>(entries);
  }

  private Map<UUID, Entry> entries() {
    return this.entries;
  }

  public int tokenBalance(UUID id) {
    var entry = this.entries.get(id);
    return entry == null ? 0 : entry.tokenBalance;
  }

  public boolean purchaseShout(ServerPlayer player) {
    if (PermissionAPI.getPermission(player, ShoutTokensPermissions.BYPASS_TOKEN_COST)) {
      return true;
    }

    var entry = this.entries.get(player.getUUID());
    if (entry == null) {
      return false;
    }

    var cost = ShoutTokensConfig.INSTANCE.tokensPerMessage.get();
    if (entry.tokenBalance < cost) {
      return false;
    }

    entry.tokenBalance -= cost;
    return true;
  }

  public void addTokens(UUID id, int tokens) {
    var entry = this.entries.get(id);
    if (entry == null) {
      this.entries.put(id, new Entry(tokens));
      return;
    }
    entry.tokenBalance += tokens;
  }

  public void removeTokens(UUID id, int tokens) {
    var entry = this.entries.get(id);
    if (entry == null) {
      return;
    }
    entry.tokenBalance = Math.max(0, entry.tokenBalance - tokens);
  }

  public void setTokens(UUID id, int tokens) {
    if (tokens < 0) {
      throw new IllegalArgumentException("tokens cannot be less than zero.");
    }

    if (tokens == 0) {
      this.entries.remove(id);
      return;
    }

    var entry = this.entries.get(id);
    if (entry == null) {
      this.entries.put(id, new Entry(tokens));
      return;
    }
    entry.tokenBalance = tokens;
  }

  public void save(Path path) {
    var json = CODEC.encodeStart(JsonOps.INSTANCE, this)
        .result()
        .orElseThrow();
    try (var writer = GSON.newJsonWriter(Files.newBufferedWriter(path))) {
      GSON.toJson(json, writer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static TokenManager load(Path path) {
    if (Files.notExists(path)) {
      return new TokenManager(Map.of());
    }

    try (var reader = Files.newBufferedReader(path)) {
      return CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(reader, JsonElement.class))
          .result()
          .orElseThrow();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static class Entry {

    private static final Codec<Entry> CODEC =
        RecordCodecBuilder.create(instance -> instance
            .group(
                Codec.INT
                    .optionalFieldOf("tokenBalance", 0)
                    .forGetter(Entry::tokenBalance))
            .apply(instance, Entry::new));

    private int tokenBalance;

    public Entry(int tokenBalance) {
      this.tokenBalance = tokenBalance;
    }

    public int tokenBalance() {
      return this.tokenBalance;
    }
  }
}
