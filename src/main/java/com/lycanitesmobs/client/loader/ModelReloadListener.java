package com.lycanitesmobs.client.loader;

import com.lycanitesmobs.client.manager.ModelManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Trimmed - drops the Iris/VBO-batcher cache clearing (VBOBatcher/IrisStaticVboBatcher/
 * OculusCompat/CustomRenderStates/RecolorTextureCache - none of that is ported, see
 * PORT_PLAN.md on why the whole Iris/VBO layer was skipped).
 *
 * Registering this is the real fix for creature models silently failing to load their .obj:
 * EntityRenderersEvent.RegisterRenderers (where ModelManager.createModels() instantiates every
 * ported CreatureModel, e.g. ModelCalpod) fires before the mod's own resources have been loaded
 * into the client ResourceManager (confirmed by log order - the "Reloading ResourceManager:
 * vanilla, mod_resources, mod/lycanitesmobs, mod/neoforge" line comes AFTER the "Unable to load
 * model: lycanitesmobs:modelparts/entity/calpod.obj" warning). Each model's first initModel()
 * call inside its constructor genuinely fails at that point - not a missing-asset bug, a
 * lifecycle-ordering one. This reload listener re-runs ModelManager.reloadModels() (which calls
 * CreatureObjModel.reloadModel() on the SAME already-constructed model instances) once the
 * resource manager actually has the mod's assets, self-healing the models CreatureRenderer
 * instances already reference.
 */
@OnlyIn(Dist.CLIENT)
public class ModelReloadListener implements PreparableReloadListener {
    public static final ModelReloadListener INSTANCE = new ModelReloadListener();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                           ResourceManager resourceManager,
                                           ProfilerFiller prepProfiler,
                                           ProfilerFiller reloadProfiler,
                                           Executor backgroundExecutor,
                                           Executor gameExecutor) {
        return CompletableFuture
                .supplyAsync(ModelManager::getInstance, backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(manager -> {
                    LMHelperClass.logDebug("Resources", "ModelReloadListener.reload: apply");
                    manager.reloadModels(resourceManager);
                }, gameExecutor);
    }
}
