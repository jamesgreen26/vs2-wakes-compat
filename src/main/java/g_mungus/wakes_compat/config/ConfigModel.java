package g_mungus.wakes_compat.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.RangeConstraint;

@Modmenu(modId = "vs-wakes-compat")
@Config(name = "vs-wakes-config", wrapperName = "VSWakesConfig")
public class ConfigModel {
    public boolean shouldSkipWakesFromSide = true;

    @RangeConstraint(min = 0, max = 64)
    public int maxWidth = 24;
}
