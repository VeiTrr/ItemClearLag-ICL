package vt.icl.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import vt.icl.ICL;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ConfigScreenEntrypoint implements ModMenuApi {
//    @Override
//    public ConfigScreenFactory<?> getModConfigScreenFactory() {
//       // return parent -> ModMenuConfigScreen.createScreen(parent, ICL.getConfig());
//        return;
//    }
}