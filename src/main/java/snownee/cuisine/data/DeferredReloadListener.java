package snownee.cuisine.data;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IFutureReloadListener;
import net.minecraft.resources.IResourceManager;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public enum DeferredReloadListener implements IFutureReloadListener {
    INSTANCE;

    public final ListMultimap<LoadingStage, IFutureReloadListener> listeners = ArrayListMultimap.create(3, 4);
    private CompletableFuture<Void> registryCompleted;
    private int count;

    @Override
    public CompletableFuture<Void> reload(IStage stage, IResourceManager resourceManager, IProfiler preparationsProfiler, IProfiler reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        registryCompleted = new CompletableFuture<>();
        count = 0;
        Function<IFutureReloadListener, CompletableFuture<?>> mapper = listener -> listener.reload(DummyStage.INSTANCE, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
        return stage.markCompleteAwaitingOthers(null).thenCompose($ -> make(LoadingStage.REGISTRY, mapper)).thenCompose($ -> registryCompleted).thenCompose($ -> make(LoadingStage.TAG, mapper)).thenCompose($ -> make(LoadingStage.RECIPE, mapper));
    }

    public synchronized <T extends IForgeRegistryEntry<T>> void complete(IForgeRegistry<T> registry) {
        if (++count >= 2) {
            registryCompleted.complete(null);
        }
    }

    private CompletableFuture<Void> make(LoadingStage loadingStage, Function<IFutureReloadListener, CompletableFuture<?>> mapper) {
        CompletableFuture<?>[] futures = {};
        listeners.get(loadingStage).stream().map(mapper).collect(Collectors.toList()).toArray(futures);
        return CompletableFuture.allOf(futures);
    }

    public static enum LoadingStage {
        REGISTRY, // 4
        TAG, // 1
        RECIPE // 1
    }

    public static enum DummyStage implements IStage {
        INSTANCE;

        @Override
        public <T> CompletableFuture<T> markCompleteAwaitingOthers(T backgroundResult) {
            return CompletableFuture.completedFuture(backgroundResult);
        }

    }

}
