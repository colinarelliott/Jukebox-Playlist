package rehdpanda.jukeboxplaylist;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class JPItems {
    public static final Identifier WRENCH_ID = Identifier.of(JPInit.MOD_ID, "wrench");
    public static final Item WRENCH_ITEM = Registry.register(
            Registries.ITEM,
            WRENCH_ID,
            new Item(new Item.Settings()
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, WRENCH_ID))
                    .maxCount(1)
                    .maxDamage(500))
    );

    public static void register() {
        // Static initializer handles registration
    }
}
